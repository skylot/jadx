package jadx.api.utils;

import java.util.function.BiFunction;

import jadx.api.ICodeInfo;
import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.core.dex.nodes.MethodNode;

public class CodeUtils {

	public static String getLineForPos(String code, int pos) {
		int start = getLineStartForPos(code, pos);
		int end = getLineEndForPos(code, pos);
		return code.substring(start, end);
	}

	public static int getLineStartForPos(String code, int pos) {
		int start = getNewLinePosBefore(code, pos);
		return start == -1 ? 0 : start + 1;
	}

	public static int getLineEndForPos(String code, int pos) {
		int end = getNewLinePosAfter(code, pos);
		return end == -1 ? code.length() : end;
	}

	public static int getNewLinePosAfter(String code, int startPos) {
		int pos = code.indexOf('\n', startPos);
		if (pos != -1) {
			// check for '\r\n'
			int prev = pos - 1;
			if (code.charAt(prev) == '\r') {
				return prev;
			}
		}
		return pos;
	}

	public static int getNewLinePosBefore(String code, int startPos) {
		return code.lastIndexOf('\n', startPos);
	}

	public static int getLineNumForPos(String code, int pos, String newLine) {
		int newLineLen = newLine.length();
		int line = 1;
		int prev = 0;
		while (true) {
			int next = code.indexOf(newLine, prev);
			if (next >= pos) {
				return line;
			}
			prev = next + newLineLen;
			line++;
		}
	}

	/**
	 * Cut method code (including comments and annotations) from class code.
	 *
	 * @return method code or empty string if metadata is not available
	 */
	public static String extractMethodCode(MethodNode mth, ICodeInfo codeInfo) {
		int end = getMethodEnd(mth, codeInfo);
		if (end == -1) {
			return "";
		}
		int start = getMethodStart(mth, codeInfo);
		if (end < start) {
			return "";
		}
		return codeInfo.getCodeStr().substring(start, end);
	}

	/**
	 * Search first empty line before method definition to include comments and annotations
	 */
	private static int getMethodStart(MethodNode mth, ICodeInfo codeInfo) {
		int pos = mth.getDefPosition();
		String newLineStr = mth.root().getArgs().getCodeNewLineStr();
		String emptyLine = newLineStr + newLineStr;
		int emptyLinePos = codeInfo.getCodeStr().lastIndexOf(emptyLine, pos);
		return emptyLinePos == -1 ? pos : emptyLinePos + emptyLine.length();
	}

	/**
	 * Search method end position in provided class code info.
	 *
	 * @return end pos or -1 if metadata not available
	 */
	public static int getMethodEnd(MethodNode mth, ICodeInfo codeInfo) {
		if (!codeInfo.hasMetadata()) {
			return -1;
		}
		// skip nested nodes DEF/END until first unpaired END annotation (end of this method)
		Integer end = codeInfo.getCodeMetadata().searchDown(mth.getDefPosition() + 1, new BiFunction<>() {
			int nested = 0;

			@Override
			public Integer apply(Integer pos, ICodeAnnotation ann) {
				switch (ann.getAnnType()) {
					case DECLARATION:
						ICodeNodeRef node = ((NodeDeclareRef) ann).getNode();
						switch (node.getAnnType()) {
							case CLASS:
							case METHOD:
								nested++;
								break;
						}
						break;

					case END:
						if (nested == 0) {
							return pos;
						}
						nested--;
						break;
				}
				return null;
			}
		});
		return end == null ? -1 : end;
	}
}
