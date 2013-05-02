package jadx.codegen;

import jadx.utils.exceptions.JadxRuntimeException;

import java.io.File;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeWriter {
	private static final Logger LOG = LoggerFactory.getLogger(CodeWriter.class);
	private static final int MAX_FILENAME_LENGTH = 128;

	public static final String NL = System.getProperty("line.separator");
	private static final String INDENT = "\t";

	private StringBuilder buf = new StringBuilder();
	private String indentStr;
	private int indent;

	public CodeWriter() {
		this.indent = 0;
		this.indentStr = "";
	}

	public CodeWriter(int indent) {
		this.indent = indent;
		updateIndent();
	}

	public CodeWriter startLine(String str) {
		buf.append(NL);
		buf.append(indentStr);
		buf.append(str);
		return this;
	}

	public CodeWriter startLine(char c) {
		buf.append(NL);
		buf.append(indentStr);
		buf.append(c);
		return this;
	}

	public CodeWriter startLine(int ind, String str) {
		buf.append(NL);
		buf.append(indentStr);
		for (int i = 0; i < ind; i++)
			buf.append(INDENT);
		buf.append(str);
		return this;
	}

	public CodeWriter startLine() {
		buf.append(NL);
		buf.append(indentStr);
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

	public CodeWriter add(CodeWriter mthsCode) {
		buf.append(mthsCode.toString());
		return this;
	}

	public CodeWriter endl() {
		buf.append(NL);
		return this;
	}

	private static final String[] indentCache = new String[] {
			"",
			INDENT,
			INDENT + INDENT,
			INDENT + INDENT + INDENT,
			INDENT + INDENT + INDENT + INDENT,
			INDENT + INDENT + INDENT + INDENT + INDENT,
	};

	private void updateIndent() {
		if (indent < 6) {
			this.indentStr = indentCache[indent];
		} else {
			StringBuilder s = new StringBuilder(indent * INDENT.length());
			for (int i = 0; i < indent; i++) {
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

	private static String removeFirstEmptyLine(String str) {
		if (str.startsWith(NL)) {
			return str.substring(NL.length());
		} else {
			return str;
		}
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
			if (cutAt <= 0)
				name = name.substring(0, MAX_FILENAME_LENGTH - 1);
			else
				name = name.substring(0, cutAt) + name.substring(dotIndex);
			file = new File(file.getParentFile(), name);
		}

		PrintWriter out = null;
		try {
			makeDirsForFile(file);
			out = new PrintWriter(file, "UTF-8");
			String code = buf.toString();
			code = removeFirstEmptyLine(code);
			out.print(code);
		} catch (Exception e) {
			LOG.error("Save file error", e);
		} finally {
			if (out != null)
				out.close();
			buf = null;
		}
	}

	private void makeDirsForFile(File file) {
		File dir = file.getParentFile();
		if (!dir.exists()) {
			// if directory already created in other thread mkdirs will return false,
			// so check dir existence again
			if (!dir.mkdirs() && !dir.exists())
				throw new JadxRuntimeException("Can't create directory " + dir);
		}
	}

}
