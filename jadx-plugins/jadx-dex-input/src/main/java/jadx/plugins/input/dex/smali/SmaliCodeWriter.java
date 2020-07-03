package jadx.plugins.input.dex.smali;

import java.util.List;

public class SmaliCodeWriter {
	public static final String NL = System.getProperty("line.separator");
	public static final String INDENT_STR = "    ";

	private final StringBuilder code = new StringBuilder();

	private int indent;
	private String indentStr = "";

	public SmaliCodeWriter startLine(String line) {
		startLine();
		code.append(line);
		return this;
	}

	public SmaliCodeWriter startLine() {
		if (code.length() != 0) {
			code.append(NL);
			code.append(indentStr);
		}
		return this;
	}

	public SmaliCodeWriter add(Object obj) {
		code.append(obj);
		return this;
	}

	public SmaliCodeWriter add(int i) {
		code.append(i);
		return this;
	}

	public SmaliCodeWriter add(char c) {
		code.append(c);
		return this;
	}

	public SmaliCodeWriter add(String str) {
		code.append(str);
		return this;
	}

	public SmaliCodeWriter addArgs(List<String> argTypes) {
		for (String type : argTypes) {
			code.append(type);
		}
		return this;
	}

	public void incIndent() {
		this.indent++;
		buildIndent();
	}

	public void decIndent() {
		this.indent--;
		buildIndent();
	}

	private void buildIndent() {
		StringBuilder s = new StringBuilder(indent * INDENT_STR.length());
		for (int i = 0; i < indent; i++) {
			s.append(INDENT_STR);
		}
		this.indentStr = s.toString();
	}

	public String getCode() {
		return code.toString();
	}

}
