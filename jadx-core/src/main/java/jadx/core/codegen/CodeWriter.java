package jadx.core.codegen;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.CodePosition;
import jadx.api.ICodeInfo;
import jadx.api.impl.SimpleCodeInfo;
import jadx.core.dex.attributes.nodes.LineAttrNode;
import jadx.core.utils.StringUtils;

public class CodeWriter {
	private static final Logger LOG = LoggerFactory.getLogger(CodeWriter.class);

	public static final String NL = System.getProperty("line.separator");
	public static final String INDENT_STR = "    ";

	private static final boolean ADD_LINE_NUMBERS = false;

	private static final String[] INDENT_CACHE = {
			"",
			INDENT_STR,
			INDENT_STR + INDENT_STR,
			INDENT_STR + INDENT_STR + INDENT_STR,
			INDENT_STR + INDENT_STR + INDENT_STR + INDENT_STR,
			INDENT_STR + INDENT_STR + INDENT_STR + INDENT_STR + INDENT_STR,
	};

	private StringBuilder buf;
	@Nullable
	private String code;
	private String indentStr;
	private int indent;

	private int line = 1;
	private int offset = 0;
	private Map<CodePosition, Object> annotations = Collections.emptyMap();
	private Map<Integer, Integer> lineMap = Collections.emptyMap();

	public CodeWriter() {
		this.buf = new StringBuilder();
		this.indent = 0;
		this.indentStr = "";
		if (ADD_LINE_NUMBERS) {
			incIndent(3);
			add(indentStr);
		}
	}

	public CodeWriter startLine() {
		addLine();
		addLineIndent();
		return this;
	}

	public CodeWriter startLine(char c) {
		addLine();
		addLineIndent();
		add(c);
		return this;
	}

	public CodeWriter startLine(String str) {
		addLine();
		addLineIndent();
		add(str);
		return this;
	}

	public CodeWriter startLineWithNum(int sourceLine) {
		if (sourceLine == 0) {
			startLine();
			return this;
		}
		if (ADD_LINE_NUMBERS) {
			newLine();
			attachSourceLine(sourceLine);
			String ln = "/* " + sourceLine + " */ ";
			add(ln);
			if (indentStr.length() > ln.length()) {
				add(indentStr.substring(ln.length()));
			}
		} else {
			startLine();
			attachSourceLine(sourceLine);
		}
		return this;
	}

	public CodeWriter addMultiLine(String str) {
		if (str.contains(NL)) {
			buf.append(str.replace(NL, NL + indentStr));
			line += StringUtils.countMatches(str, NL);
			offset = 0;
		} else {
			buf.append(str);
		}
		return this;
	}

	public CodeWriter add(String str) {
		buf.append(str);
		offset += str.length();
		return this;
	}

	public CodeWriter add(char c) {
		buf.append(c);
		offset++;
		return this;
	}

	CodeWriter add(CodeWriter code) {
		line--;
		for (Map.Entry<CodePosition, Object> entry : code.annotations.entrySet()) {
			CodePosition pos = entry.getKey();
			attachAnnotation(entry.getValue(), new CodePosition(line + pos.getLine(), pos.getOffset()));
		}
		for (Map.Entry<Integer, Integer> entry : code.lineMap.entrySet()) {
			attachSourceLine(line + entry.getKey(), entry.getValue());
		}
		line += code.line;
		offset = code.offset;
		buf.append(code.buf);
		return this;
	}

	public CodeWriter newLine() {
		addLine();
		return this;
	}

	public CodeWriter addIndent() {
		add(INDENT_STR);
		return this;
	}

	private void addLine() {
		buf.append(NL);
		line++;
		offset = 0;
	}

	private CodeWriter addLineIndent() {
		buf.append(indentStr);
		offset += indentStr.length();
		return this;
	}

	@SuppressWarnings("StringRepeatCanBeUsed")
	private void updateIndent() {
		int curIndent = indent;
		if (curIndent < INDENT_CACHE.length) {
			this.indentStr = INDENT_CACHE[curIndent];
		} else {
			StringBuilder s = new StringBuilder(curIndent * INDENT_STR.length());
			for (int i = 0; i < curIndent; i++) {
				s.append(INDENT_STR);
			}
			this.indentStr = s.toString();
		}
	}

	public void incIndent() {
		incIndent(1);
	}

	public void decIndent() {
		decIndent(1);
	}

	public void incIndent(int c) {
		this.indent += c;
		updateIndent();
	}

	public void decIndent(int c) {
		this.indent -= c;
		if (this.indent < 0) {
			LOG.warn("Indent < 0");
			this.indent = 0;
		}
		updateIndent();
	}

	public int getIndent() {
		return indent;
	}

	public void setIndent(int indent) {
		this.indent = indent;
		updateIndent();
	}

	public int getLine() {
		return line;
	}

	private static class DefinitionWrapper {
		private final LineAttrNode node;

		private DefinitionWrapper(LineAttrNode node) {
			this.node = node;
		}

		public LineAttrNode getNode() {
			return node;
		}
	}

	public void attachDefinition(LineAttrNode obj) {
		attachAnnotation(obj);
		attachAnnotation(new DefinitionWrapper(obj), new CodePosition(line, offset));
	}

	public void attachAnnotation(Object obj) {
		attachAnnotation(obj, new CodePosition(line, offset + 1));
	}

	public void attachLineAnnotation(Object obj) {
		attachAnnotation(obj, new CodePosition(line, 0));
	}

	private Object attachAnnotation(Object obj, CodePosition pos) {
		if (annotations.isEmpty()) {
			annotations = new HashMap<>();
		}
		return annotations.put(pos, obj);
	}

	public void attachSourceLine(int sourceLine) {
		if (sourceLine == 0) {
			return;
		}
		attachSourceLine(line, sourceLine);
	}

	private void attachSourceLine(int decompiledLine, int sourceLine) {
		if (lineMap.isEmpty()) {
			lineMap = new TreeMap<>();
		}
		lineMap.put(decompiledLine, sourceLine);
	}

	public ICodeInfo finish() {
		removeFirstEmptyLine();
		processDefinitionAnnotations();
		code = buf.toString();
		buf = null;
		return new SimpleCodeInfo(code, lineMap, annotations);
	}

	private void removeFirstEmptyLine() {
		int len = NL.length();
		if (buf.length() > len && buf.substring(0, len).equals(NL)) {
			buf.delete(0, len);
		}
	}

	private void processDefinitionAnnotations() {
		if (!annotations.isEmpty()) {
			annotations.entrySet().removeIf(entry -> {
				Object v = entry.getValue();
				if (v instanceof DefinitionWrapper) {
					LineAttrNode l = ((DefinitionWrapper) v).getNode();
					l.setDecompiledLine(entry.getKey().getLine());
					return true;
				}
				return false;
			});
		}
	}

	public int bufLength() {
		return buf.length();
	}

	public String getCodeStr() {
		if (code == null) {
			finish();
		}
		return code;
	}

	@Override
	public String toString() {
		return code != null ? code : buf.toString();
	}
}
