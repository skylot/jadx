package jadx.gui.ui.codearea.sync;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.codearea.SmaliArea;

/**
 * Use Debug lines in smali from dex debug info to correlate with code
 */
public class DebugLineSmaliSyncer implements IToJavaSyncStrategy {
	private static final Logger LOG = LoggerFactory.getLogger(DebugLineSmaliSyncer.class);

	private final SmaliArea from;

	public DebugLineSmaliSyncer(SmaliArea area) {
		this.from = area;
	}

	@Override
	public boolean syncTo(CodeArea to) {
		try {
			// Get the from lines and currentline index
			int lineIndex = from.getCaretLineNumber();
			String[] fromLines = from.getText().split("\\R");
			if (lineIndex >= fromLines.length) {
				return false;
			}

			// find an Anchor to guide what to look for and highlight in the CodeArea
			Anchor anchor = findNearestAnchor(lineIndex, fromLines);
			if (anchor == null) {
				LOG.error("{} - No Smali Anchor found", LOG.getName());
				return false;
			}

			if (anchor.getType() == Anchor.Type.SOURCE_LINE) {
				LOG.debug(anchor.toString());
				Map<Integer, Integer> toDecompToSourceMapping = to.getFunctionUniqueLineMappings();
				for (Map.Entry<Integer, Integer> entry : toDecompToSourceMapping.entrySet()) {
					int decompLine = entry.getKey();
					int sourceLine = entry.getValue();
					if (anchor.getCodeMappedLineNumber() == sourceLine) {
						int decompLineIndex = decompLine - 1;
						LOG.debug("Highlighting {} on {}", decompLine, to);
						CodeSyncHighlighter.defaultHighlighter().highlightAndScrollToLine(to, decompLineIndex);
						LOG.info("{} - successful sync of smali to code", LOG.getName());
						return true;
					}
				}
			}
			to.removeAllLineHighlights();
		} catch (Exception ex) {
			LOG.error("{} - Failed to sync from Smali to Code", LOG.getName(), ex);
		}
		return false;
	}

	@Nullable
	private Anchor findNearestAnchor(int smaliLineNumber, String[] lines) {
		for (int i = smaliLineNumber; i >= 0; i--) {
			String trimmedLine = lines[i].trim();
			if (trimmedLine.startsWith(".line")) {
				return new Anchor(Anchor.Type.SOURCE_LINE, trimmedLine, i);
			}
			if (trimmedLine.startsWith(".method")) {
				return new Anchor(Anchor.Type.METHOD_START, trimmedLine, i);
			}
			if (trimmedLine.startsWith(".end")) {
				return new Anchor(Anchor.Type.METHOD_END, trimmedLine, i);
			}
			if (trimmedLine.startsWith(".field")) {
				return new Anchor(Anchor.Type.FIELD, trimmedLine, i);
			}
			if (trimmedLine.startsWith(".class")) {
				return new Anchor(Anchor.Type.CLASS, trimmedLine, smaliLineNumber);
			}
		}
		return null;
	}

	/**
	 * Line in the smali that can be used to find a section to highlight in the code area
	 */
	private static class Anchor {
		public enum Type {
			SOURCE_LINE,
			METHOD_START,
			METHOD_END,
			FIELD,
			CLASS
		}

		private final Type type;
		private final String line;
		private final int smaliLineNumber;
		private int codeMappedLineNumber = -1;

		public Anchor(Type type, String line, int smaliLineNumber) {
			this.type = type;
			this.line = line;
			this.smaliLineNumber = smaliLineNumber;
			this.map();
		}

		public Type getType() {
			return type;
		}

		public int getCodeMappedLineNumber() {
			return codeMappedLineNumber;
		}

		private void map() {
			switch (type) {
				case SOURCE_LINE:
					Pattern p = Pattern.compile("(\\.line\\s)(\\d+)");
					Matcher m = p.matcher(line);
					if (m.find()) {
						codeMappedLineNumber = Integer.parseInt(m.group(2));
					}
					break;
				default:
					codeMappedLineNumber = -1;
					break;
			}
		}

		@Override
		public String toString() {
			return String.format("Anchor %s, %d, %d", type.name(), smaliLineNumber, codeMappedLineNumber);
		}
	}
}
