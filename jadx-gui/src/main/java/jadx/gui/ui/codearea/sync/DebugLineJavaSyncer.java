package jadx.gui.ui.codearea.sync;

import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.codearea.SmaliArea;

/**
 * Use debug line info from dex to correlate from java to java/smali
 */
public class DebugLineJavaSyncer implements IToSmaliSyncStrategy, IToJavaSyncStrategy {
	private static final Logger LOG = LoggerFactory.getLogger(DebugLineJavaSyncer.class);

	private final CodeArea from;

	public DebugLineJavaSyncer(CodeArea area) {
		this.from = area;
	}

	@Override
	public boolean syncTo(CodeArea to) {
		// This might be any combination between java/simple/fallback
		// We cannot just rely on the current line.
		// Instead try to correlate with line mappings.
		try {
			int lineIndex = from.getCaretLineNumber();
			Map<Integer, Integer> toLineMapping = to.getFunctionUniqueLineMappings();
			// lineIndex is 0-indexed whereas the line mappings are based off a 1-index.
			Integer sourceLine = getClosestSourceLine(lineIndex + 1);
			if (sourceLine == null) {
				return false;
			}
			// find the equivalent linenumber in the 'to' by a reverse lookup from the source line
			for (Map.Entry<Integer, Integer> entry : toLineMapping.entrySet()) {
				int toLine = entry.getKey();
				int candidateSourceLine = entry.getValue();
				if (sourceLine == candidateSourceLine) {
					// we have the mapped line we target the lineIndex which is a 0-index
					CodeSyncHighlighter.defaultHighlighter().highlightAndScrollToLine(to, toLine - 1);
					LOG.info("{} - successful sync of code to code", LOG.getName());
					return true;
				}
			}
		} catch (Exception e) {
			LOG.error("{} - Failed to sync from CodeArea to CodeArea: {}", LOG.getName(), e.getLocalizedMessage());
		}
		return false;
	}

	@Override
	public boolean syncTo(SmaliArea to) {
		try {
			int lineIndex = from.getCaretLineNumber();

			// lineIndex is 0-indexed but the line mappings are based of 1-indexed line numbers.
			int lineNum = lineIndex + 1;
			Integer sourceLine = getClosestSourceLine(lineNum);
			if (sourceLine == null) {
				to.removeAllLineHighlights();
				LOG.debug("decompiled line {} not mapped to source line", lineNum);
				return false;
			}

			// find the smali line where ".line <sourceLine>" is
			LOG.debug("Finding \".line {}\" in smali", sourceLine);
			int smaliLine = findSmaliLineIndex(to, sourceLine);
			if (smaliLine < 0) {
				LOG.warn("{} - Source line {} not annotated in Smali", LOG.getName(), sourceLine);
				return false;
			}

			CodeSyncHighlighter.defaultHighlighter().highlightAndScrollToLine(to, smaliLine);
			LOG.info("{} - successful sync of code to smali", LOG.getName());
			return true;
		} catch (Exception ex) {
			LOG.error("{} - Failed to sync CodeArea to SmaliArea: {}", LOG.getName(), ex.getLocalizedMessage());
		}
		return false;
	}

	private @Nullable Integer getClosestSourceLine(int lineNum) {
		// get the line mappings of the Java/Simple/Fallback code
		Map<Integer, Integer> lineMapping = from.getFunctionUniqueLineMappings();
		if (lineMapping == null || lineMapping.isEmpty()) {
			return null;
		}
		// get the source line from the decomp line
		Integer sourceLine = null;
		// Some of the intermediate lines are not mapped so keep going back until we find one
		// e.g. multiple instruction lines in the 'Simple' view belong to a single source line
		while (lineNum >= 0 && (sourceLine = lineMapping.get(lineNum)) == null) {
			--lineNum;
		}
		return sourceLine;
	}

	/**
	 * find the ".line \d+" line in the smali
	 */
	private static int findSmaliLineIndex(SmaliArea smaliArea, int sourceLine) {
		String line = ".line " + Integer.toString(sourceLine);
		String[] smaliLines = smaliArea.getText().split("\\R");
		for (int i = 0; i < smaliLines.length; ++i) {
			String l = smaliLines[i];
			if (l.trim().equals(line)) {
				return i;
			}
		}
		return -1;
	}
}
