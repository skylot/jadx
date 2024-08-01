package jadx.gui.ui.codearea;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldParser;
import org.fife.ui.rsyntaxtextarea.folding.FoldParserManager;
import org.fife.ui.rsyntaxtextarea.folding.FoldType;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmaliFoldParser implements FoldParser {
	private static final Logger LOG = LoggerFactory.getLogger(SmaliFoldParser.class);

	private static final Pattern CLASS_LINE_PATTERN = Pattern.compile("^\\.class\\b", Pattern.MULTILINE);
	private static final Pattern ENDMETHOD_LINE_PATTERN = Pattern.compile("^\\.end method\\b", Pattern.MULTILINE);
	private static final Pattern STARTMETHOD_LINE_PATTERN = Pattern.compile("^\\.method\\b", Pattern.MULTILINE);

	public static void register() {
		FoldParserManager.get().addFoldParserMapping(AbstractCodeArea.SYNTAX_STYLE_SMALI, new SmaliFoldParser());
	}

	private SmaliFoldParser() {
	}

	@Override
	public List<Fold> getFolds(RSyntaxTextArea textArea) {
		List<Fold> classFolds = new ArrayList<>();
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
				Fold classFold = createFold(textArea, startOffset, endOffset);
				if (classFold != null) {
					classFolds.add(classFold);

					// Start looking for .method after .class definition
					Integer startMethodStartOffset = startMethodStartOffsets.ceiling(startOffset);
					while (startMethodStartOffset != null && startMethodStartOffset < endOffset) {
						Integer endMethodEndOffset = endMethodEndOffsets.ceiling(startMethodStartOffset);
						if (endMethodEndOffset != null) {
							addFold(classFold, startMethodStartOffset, endMethodEndOffset);
						}
						// Look for next .method starting from last .end method
						startMethodStartOffset = startMethodStartOffsets.ceiling(endMethodEndOffset);
					}
				}
			}
		}
		return classFolds;
	}

	private static @Nullable Fold createFold(RSyntaxTextArea textArea, int startOffset, int endOffset) {
		try {
			Fold fold = new Fold(FoldType.CODE, textArea, startOffset);
			fold.setEndOffset(endOffset);
			return fold;
		} catch (Exception e) {
			LOG.error("Failed to create code fold", e);
			return null;
		}
	}

	private static void addFold(Fold parent, int startOffset, int endOffset) {
		try {
			Fold fold = parent.createChild(FoldType.CODE, startOffset);
			fold.setEndOffset(endOffset);
		} catch (Exception e) {
			LOG.error("Failed to add code fold", e);
		}
	}

	private List<Integer> getClassStartOffsets(String text) {
		List<Integer> startOffsets = new ArrayList<>();
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
			int startOffset = matcher.start();
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
