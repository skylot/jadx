package jadx.gui.ui.codearea;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldParser;
import org.fife.ui.rsyntaxtextarea.folding.FoldParserManager;
import org.fife.ui.rsyntaxtextarea.folding.FoldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmaliFoldParser implements FoldParser {
	private static final Logger LOG = LoggerFactory.getLogger(SmaliFoldParser.class);

	private static final Pattern CLASS_LINE_PATTERN =
			Pattern.compile("^\\.class\\b", Pattern.MULTILINE);
	private static final Pattern ENDMETHOD_LINE_PATTERN =
			Pattern.compile("^\\.end method\\b", Pattern.MULTILINE);
	private static final Pattern STARTMETHOD_LINE_PATTERN =
			Pattern.compile("^\\.method\\b", Pattern.MULTILINE);

	@Override
	public List<Fold> getFolds(RSyntaxTextArea textArea) {
		ArrayList<Fold> classFolds = new ArrayList<>();
		String text = textArea.getText();

		List<Integer> classStartOffsets = getClassStartOffsets(text);
		NavigableSet<Integer> startMethodStartOffsets = getStartMethodStartOffsets(text);
		NavigableSet<Integer> endMethodEndOffsets = getEndMethodEndOffsets(text);
		for (int i = 0; i < classStartOffsets.size(); i++) {
			// Start offset of .class
			int startOffset = classStartOffsets.get(i);

			int classLimit;
			if (i < classStartOffsets.size() - 1) {
				classLimit = classStartOffsets.get(i + 1);
			} else {
				classLimit = text.length();
			}

			// Get the last ".end method" before next .class or end of file
			Integer endOffset = endMethodEndOffsets.floor(classLimit);
			if (endOffset != null) {
				LOG.info("Found smali class at " + startOffset + ", " + endOffset);

				Fold classFold = createFold(textArea, startOffset, endOffset);
				classFolds.add(classFold);

				// Start looking for .method after .class definition
				Integer startMethodStartOffset = startMethodStartOffsets.ceiling(startOffset);
				while (startMethodStartOffset != null && startMethodStartOffset < endOffset) {
					Integer endMethodEndOffset = endMethodEndOffsets.ceiling(startMethodStartOffset);
					if (endMethodEndOffset != null) {
						LOG.info("Found smali method at " + startMethodStartOffset + ", " + endMethodEndOffset);
						addFold(classFold, startMethodStartOffset, endMethodEndOffset);
					}
					// Look for next .method starting from last .end method
					startMethodStartOffset = startMethodStartOffsets.ceiling(endMethodEndOffset);
				}
			}
		}

		return classFolds;
	}

	public static void register() {
		FoldParserManager.get().addFoldParserMapping(AbstractCodeArea.SYNTAX_STYLE_SMALI, new SmaliFoldParser());
	}

	public static Fold createFold(RSyntaxTextArea textArea, int startOffset, int endOffset) {
		Fold fold = null;
		try {
			fold = new Fold(FoldType.CODE, textArea, startOffset);
			fold.setEndOffset(endOffset);
		} catch (BadLocationException e) {
			LOG.warn("Code folding smali resulted in {} : {}", e.getClass().getSimpleName(), e.getMessage());
		}
		return fold;
	}

	public static void addFold(Fold parent, int startOffset, int endOffset) {
		Fold fold;
		try {
			fold = parent.createChild(FoldType.CODE, startOffset);
			fold.setEndOffset(endOffset);
		} catch (BadLocationException e) {
			LOG.warn("Code folding smali resulted in {} : {}", e.getClass().getSimpleName(), e.getMessage());
		}
	}

	private List<Integer> getClassStartOffsets(String text) {
		ArrayList<Integer> startOffsets = new ArrayList<>();

		Matcher matcher = CLASS_LINE_PATTERN.matcher(text);
		while (matcher.find()) {
			int startOffset = matcher.start();
			startOffsets.add(startOffset);
		}

		return startOffsets;
	}

	private NavigableSet<Integer> getStartMethodStartOffsets(String text) {
		NavigableSet<Integer> startOffsets = new TreeSet<>();

		Matcher matcher = STARTMETHOD_LINE_PATTERN.matcher(text);
		while (matcher.find()) {
			int startOffset = matcher.end();
			startOffsets.add(startOffset);
		}

		return startOffsets;
	}

	private NavigableSet<Integer> getEndMethodEndOffsets(String text) {
		NavigableSet<Integer> endOffsets = new TreeSet<>();

		Matcher matcher = ENDMETHOD_LINE_PATTERN.matcher(text);
		while (matcher.find()) {
			int endOffset = matcher.end();
			endOffsets.add(endOffset);
		}

		return endOffsets;
	}
}
