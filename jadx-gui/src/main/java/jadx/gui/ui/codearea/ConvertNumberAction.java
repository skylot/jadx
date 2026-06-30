package jadx.gui.ui.codearea;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.event.PopupMenuEvent;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.Token;
import org.jetbrains.annotations.Nullable;

import jadx.api.data.CommentStyle;
import jadx.api.data.ICodeComment;
import jadx.api.data.impl.JadxCodeComment;
import jadx.gui.treemodel.JClass;
import jadx.gui.ui.action.ActionModel;
import jadx.gui.ui.dialog.CommentDialog;
import jadx.gui.utils.NLS;

public class ConvertNumberAction extends CommentAction {
	private static final String DEFAULT_TEXT = NLS.str("popup.convert_number");
	private static final String TOOLTIP_TEXT = NLS.str("popup.convert_number_tooltip");

	private @Nullable String codeComment;

	public ConvertNumberAction(CodeArea codeArea) {
		super(ActionModel.CONVERT_NUMBER, codeArea);
		setEnabled(false);
		setNameAndDesc(DEFAULT_TEXT);
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		if (codeArea.getNode() instanceof JClass) {
			// try parse number from word under caret
			// and set text of popup menu dynamically
			String word = getWordByPosition(codeArea.getCaretPosition());
			List<String> conversions = getConversionsFromWord(word);
			if (!conversions.isEmpty()) {
				codeComment = String.join(" | ", conversions);
				setName(DEFAULT_TEXT + ": " + codeComment);
				setShortDescription(TOOLTIP_TEXT);
				setEnabled(true);
			}
		}
	}

	@Override
	public void popupMenuCanceled(PopupMenuEvent e) {
		// reset menu to disabled on cancel
		setEnabled(false);
		setNameAndDesc(DEFAULT_TEXT);
		codeComment = null;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (!enabled) {
			return;
		}
		String newText = codeComment;
		if (newText == null) {
			return;
		}
		ICodeComment comment = getCommentRef(codeArea.getCaretPosition());
		if (comment == null) {
			return;
		}
		ICodeComment newComment = new JadxCodeComment(comment.getNodeRef(), comment.getCodeRef(), newText, CommentStyle.LINE);
		CommentDialog.updateCommentsData(codeArea, list -> list.add(newComment));

	}

	/**
	 * Similar to AbstractCodeArea::getWordByPosition
	 * but includes "-" for negative numbers
	 */
	public @Nullable String getWordByPosition(int offset) {
		Token token = codeArea.getWordTokenAtOffset(offset);
		if (token == null) {
			return null;
		}
		String str = token.getLexeme();
		try {
			String prev = codeArea.getText(token.getOffset() - 1, 1);
			if (prev.equals("-")) {
				str = "-" + str;
			}
		} catch (BadLocationException e) {
			// ignore
		}
		int len = str.length();
		if (len > 2 && str.startsWith("\"") && str.endsWith("\"")) {
			return str.substring(1, len - 1);
		}
		return str;
	}

	private static final Pattern NUMBER_FORMAT = Pattern.compile("(-?\\d+L?|0x[0-9A-Fa-f]+)");

	/**
	 * Tries to parse a number from input string,
	 * returns list of strings of the number converted to different formats.
	 * e.g. if input number is in hex, converts to decimal and binary.
	 */
	static List<String> getConversionsFromWord(@Nullable String word) {
		if (word == null || word.isEmpty() || word.equals("0") || word.equals("0L")) {
			return Collections.emptyList();
		}
		if (!NUMBER_FORMAT.matcher(word).matches()) {
			return Collections.emptyList();
		}
		int i32;
		long i64;
		int radix;
		boolean parsedLong = false;
		if (word.startsWith("0x")) {
			word = word.substring(2);
			radix = 16;
		} else {
			radix = 10;
		}
		// handle long int syntax like "12345L"
		if (word.endsWith("L")) {
			word = word.substring(0, word.length() - 1);
			i64 = tryParseLong(word, radix);
			i32 = (int) i64;
			parsedLong = true;
			if (i64 == 0) {
				return Collections.emptyList();
			}
		} else {
			i32 = tryParseInt(word, radix);
			if (i32 != 0) {
				i64 = i32;
			} else {
				i64 = tryParseLong(word, radix);
				parsedLong = true;
				if (i64 == 0) {
					return Collections.emptyList();
				}
			}
		}
		List<String> conversions = new ArrayList<>();
		// if we parsed decimal, output hex and vice versa
		if (radix == 10) {
			if (parsedLong) {
				conversions.add(String.format("0x%x", i64));
			} else {
				conversions.add(String.format("0x%x", i32));
			}
		} else {
			conversions.add(parsedLong ? Long.toString(i64) : Integer.toString(i32));
		}

		// pad binary in 8-bit groups
		int padBits = (int) Math.ceil((64 - Long.numberOfLeadingZeros(i64)) / 8.0) * 8;
		if (padBits < 8) {
			padBits = 8;
		}
		if (!parsedLong && padBits > 32) {
			padBits = 32;
		}
		// format padded binary
		String binaryString = parsedLong ? Long.toBinaryString(i64) : Integer.toBinaryString(i32);
		String fmt = String.format("0b%%%ds", padBits);
		conversions.add(String.format(fmt, binaryString).replace(' ', '0'));

		// format printable ASCII chars
		if (i32 >= ' ' && i32 <= '~') {
			conversions.add(String.format("'%c'", i32));
		}
		return conversions;
	}

	private static int tryParseInt(String str, int radix) {
		try {
			return Integer.parseInt(str, radix);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long tryParseLong(String str, int radix) {
		try {
			return Long.parseLong(str, radix);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
