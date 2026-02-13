package jadx.gui.ui.codearea;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.event.PopupMenuEvent;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.Token;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.data.CommentStyle;
import jadx.api.data.ICodeComment;
import jadx.api.data.impl.JadxCodeComment;
import jadx.api.data.impl.JadxCodeData;
import jadx.gui.settings.JadxProject;
import jadx.gui.treemodel.JClass;
import jadx.gui.ui.action.ActionModel;
import jadx.gui.utils.NLS;

public class ConvertNumberAction extends CommentAction {

	private static final Logger LOG = LoggerFactory.getLogger(ConvertNumberAction.class);

	private static final String DEFAULT_TEXT = "";
	private final String tooltipText;

	public ConvertNumberAction(CodeArea codeArea) {

		super(ActionModel.CONVERT_NUMBER, codeArea);

		tooltipText = NLS.str("popup.convert_number");

		// default menu item to disabled
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
			if (conversions != null && !conversions.isEmpty()) {
				String joined = String.join(" | ", conversions);
				setName(joined);
				setShortDescription(tooltipText);
				setEnabled(true);
			}
		}
	}

	@Override
	public void popupMenuCanceled(PopupMenuEvent e) {
		// reset menu to disabled on cancel
		setEnabled(false);
		setNameAndDesc(DEFAULT_TEXT);
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if (!super.enabled) {
			return;
		}

		String newText = e.getActionCommand();
		if (newText == null) {
			return;
		}

		ICodeComment comment = getCommentRef(codeArea.getCaretPosition());
		if (comment == null) {
			return;
		}

		ICodeComment newComment = new JadxCodeComment(comment.getNodeRef(), comment.getCodeRef(), newText, CommentStyle.LINE);
		updateCommentsData(codeArea, list -> list.add(newComment));

	}

	/**
	 * Adds comments to project file and code area
	 */
	private static void updateCommentsData(CodeArea codeArea, Consumer<List<ICodeComment>> updater) {
		try {
			JadxProject project = codeArea.getProject();
			JadxCodeData codeData = project.getCodeData();
			if (codeData == null) {
				codeData = new JadxCodeData();
			}
			List<ICodeComment> list = new ArrayList<>(codeData.getComments());
			updater.accept(list);
			Collections.sort(list);
			codeData.setComments(list);
			project.setCodeData(codeData);
			codeArea.getMainWindow().getWrapper().reloadCodeData();
		} catch (Exception e) {
			LOG.error("Comment action failed", e);
		}
		try {
			// refresh code
			codeArea.backgroundRefreshClass();
		} catch (Exception e) {
			LOG.error("Failed to reload code", e);
		}
	}

	/**
	 * similar to AbstractCodeArea::getWordByPosition
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

	/**
	 * Tries to parse a number from input string,
	 * returns list of strings of the number converted to different formats.
	 * e.g. if input number is in hex, converts to decimal and binary.
	 */
	static @Nullable List<String> getConversionsFromWord(String word) {

		List<String> conversions = new ArrayList<>();

		if (word == null || word.isEmpty()) {
			return null;
		}

		int i32 = 0;
		long i64 = 0;
		int radix = 10;
		boolean parsedLong = false;

		// handle hex
		if (word.startsWith("0x")) {
			word = word.substring(2);
			radix = 16;
		}

		// handle long int syntax like "12345L"
		if (word.endsWith("L")) {
			word = word.substring(0, word.length() - 1);
			parsedLong = true;
		}

		// try parse int
		try {
			i32 = Integer.parseInt(word, radix);
			i64 = i32;

		} catch (NumberFormatException e) {

			// try parse long
			try {
				i64 = Long.parseLong(word, radix);
				parsedLong = true;

			} catch (NumberFormatException ignore) {
				return null;
			}
		}

		// if we parsed decimal, output hex and vice versa
		if (radix == 10) {
			if (parsedLong) {
				conversions.add(String.format("0x%x", i64));
			} else {
				conversions.add(String.format("0x%x", i32));
			}

		} else if (radix == 16) {
			conversions.add(String.format("%d", i32));
		}

		// pad binary in 8-bit groups
		// int leadingZeros = parsed_long ? : Integer.numberOfLeadingZeros(i32);
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

		// format printable ascii chars
		if (i32 >= ' ' && i32 <= '~') {
			conversions.add(String.format("'%c'", (int) i32));
		}

		return conversions; // no match
	}
}
