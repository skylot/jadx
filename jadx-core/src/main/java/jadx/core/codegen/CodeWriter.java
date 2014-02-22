package jadx.core.codegen;

import jadx.core.dex.attributes.LineAttrNode;
import jadx.core.utils.Utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeWriter {
	private static final Logger LOG = LoggerFactory.getLogger(CodeWriter.class);
	private static final int MAX_FILENAME_LENGTH = 128;

	public static final String NL = System.getProperty("line.separator");
	public static final String INDENT = "\t";

	private final StringBuilder buf = new StringBuilder();
	private String indentStr;
	private int indent;

	private int line = 1;
	private Map<Object, Integer> annotations = Collections.emptyMap();

	public CodeWriter() {
		this.indent = 0;
		this.indentStr = "";
	}

	public CodeWriter(int indent) {
		this.indent = indent;
		updateIndent();
	}

	public CodeWriter startLine() {
		addLine();
		buf.append(indentStr);
		return this;
	}

	public CodeWriter startLine(char c) {
		addLine();
		buf.append(indentStr);
		buf.append(c);
		return this;
	}

	public CodeWriter startLine(String str) {
		addLine();
		buf.append(indentStr);
		buf.append(str);
		return this;
	}

	public CodeWriter startLine(int ind, String str) {
		addLine();
		buf.append(indentStr);
		for (int i = 0; i < ind; i++) {
			buf.append(INDENT);
		}
		buf.append(str);
		return this;
	}

	public CodeWriter add(String str) {
		buf.append(str);
		return this;
	}

	public CodeWriter add(char c) {
		buf.append(c);
		return this;
	}

	public CodeWriter add(CodeWriter code) {
		line--;
		for (Map.Entry<Object, Integer> entry : code.annotations.entrySet()) {
			attachAnnotation(entry.getKey(), line + entry.getValue());
		}
		line += code.line;
		buf.append(code);
		return this;
	}

	public CodeWriter newLine() {
		addLine();
		return this;
	}

	private void addLine() {
		buf.append(NL);
		line++;
	}

	public int getLine() {
		return line;
	}

	public Object attachAnnotation(Object obj) {
		return attachAnnotation(obj, line);
	}

	public Object attachAnnotation(Object obj, int line) {
		if (annotations.isEmpty()) {
			annotations = new HashMap<Object, Integer>();
		}
		return annotations.put(obj, line);
	}

	public CodeWriter indent() {
		buf.append(indentStr);
		return this;
	}

	private static final String[] INDENT_CACHE = {
			"",
			INDENT,
			INDENT + INDENT,
			INDENT + INDENT + INDENT,
			INDENT + INDENT + INDENT + INDENT,
			INDENT + INDENT + INDENT + INDENT + INDENT,
	};

	private void updateIndent() {
		int curIndent = indent;
		if (curIndent < 6) {
			this.indentStr = INDENT_CACHE[curIndent];
		} else {
			StringBuilder s = new StringBuilder(curIndent * INDENT.length());
			for (int i = 0; i < curIndent; i++) {
				s.append(INDENT);
			}
			this.indentStr = s.toString();
		}
	}

	public int getIndent() {
		return indent;
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

	public void finish() {
		buf.trimToSize();
		for (Map.Entry<Object, Integer> entry : annotations.entrySet()) {
			Object v = entry.getKey();
			if (v instanceof LineAttrNode) {
				LineAttrNode l = (LineAttrNode) v;
				l.setDecompiledLine(entry.getValue());
			}
		}
		annotations.clear();
	}

	private static String removeFirstEmptyLine(String str) {
		if (str.startsWith(NL)) {
			return str.substring(NL.length());
		} else {
			return str;
		}
	}

	public int length() {
		return buf.length();
	}

	public boolean isEmpty() {
		return buf.length() == 0;
	}

	public boolean notEmpty() {
		return buf.length() != 0;
	}

	@Override
	public String toString() {
		return buf.toString();
	}

	public void save(File dir, String subDir, String fileName) {
		save(dir, new File(subDir, fileName).getPath());
	}

	public void save(File dir, String fileName) {
		save(new File(dir, fileName));
	}

	public void save(File file) {
		String name = file.getName();
		if (name.length() > MAX_FILENAME_LENGTH) {
			int dotIndex = name.indexOf('.');
			int cutAt = MAX_FILENAME_LENGTH - name.length() + dotIndex - 1;
			if (cutAt <= 0) {
				name = name.substring(0, MAX_FILENAME_LENGTH - 1);
			} else {
				name = name.substring(0, cutAt) + name.substring(dotIndex);
			}
			file = new File(file.getParentFile(), name);
		}

		PrintWriter out = null;
		try {
			Utils.makeDirsForFile(file);
			out = new PrintWriter(file, "UTF-8");
			String code = buf.toString();
			code = removeFirstEmptyLine(code);
			out.print(code);
		} catch (Exception e) {
			LOG.error("Save file error", e);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	@Override
	public int hashCode() {
		return buf.toString().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CodeWriter)) {
			return false;
		}
		CodeWriter that = (CodeWriter) o;
		return buf.toString().equals(that.buf.toString());
	}
}
