package jadx.api.impl;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.api.ICodeWriter;
import jadx.api.JadxArgs;
import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeNodeRef;
import jadx.core.utils.Utils;

/**
 * CodeWriter implementation without meta information support
 */
public class SimpleCodeWriter implements ICodeWriter {
	private static final Logger LOG = LoggerFactory.getLogger(SimpleCodeWriter.class);

	protected StringBuilder buf = new StringBuilder();
	protected String indentStr = "";
	protected int indent = 0;

	protected final boolean insertLineNumbers;
	protected final String singleIndentStr;
	protected final String newLineStr;

	public SimpleCodeWriter(JadxArgs args) {
		this.insertLineNumbers = args.isInsertDebugLines();
		this.singleIndentStr = args.getCodeIndentStr();
		this.newLineStr = args.getCodeNewLineStr();
		if (insertLineNumbers) {
			incIndent(3);
			add(indentStr);
		}
	}

	/**
	 * Constructor with JadxArgs should be used.
	 */
	@Deprecated
	public SimpleCodeWriter() {
		this.insertLineNumbers = false;
		this.singleIndentStr = JadxArgs.DEFAULT_INDENT_STR;
		this.newLineStr = JadxArgs.DEFAULT_NEW_LINE_STR;
	}

	@Override
	public boolean isMetadataSupported() {
		return false;
	}

	@Override
	public SimpleCodeWriter startLine() {
		addLine();
		addLineIndent();
		return this;
	}

	@Override
	public SimpleCodeWriter startLine(char c) {
		startLine();
		add(c);
		return this;
	}

	@Override
	public SimpleCodeWriter startLine(String str) {
		startLine();
		add(str);
		return this;
	}

	@Override
	public SimpleCodeWriter startLineWithNum(int sourceLine) {
		if (sourceLine == 0) {
			startLine();
			return this;
		}
		if (this.insertLineNumbers) {
			newLine();
			attachSourceLine(sourceLine);
			int start = getLength();
			add("/* ").add(Integer.toString(sourceLine)).add(" */ ");
			int len = getLength() - start;
			if (indentStr.length() > len) {
				add(indentStr.substring(len));
			}
		} else {
			startLine();
			attachSourceLine(sourceLine);
		}
		return this;
	}

	@Override
	public SimpleCodeWriter addMultiLine(String str) {
		if (str.contains(newLineStr)) {
			buf.append(str.replace(newLineStr, newLineStr + indentStr));
		} else {
			buf.append(str);
		}
		return this;
	}

	@Override
	public SimpleCodeWriter add(String str) {
		buf.append(str);
		return this;
	}

	@Override
	public SimpleCodeWriter add(char c) {
		buf.append(c);
		return this;
	}

	@Override
	public ICodeWriter add(ICodeWriter cw) {
		buf.append(cw.getCodeStr());
		return this;
	}

	@Override
	public SimpleCodeWriter newLine() {
		addLine();
		return this;
	}

	@Override
	public SimpleCodeWriter addIndent() {
		add(singleIndentStr);
		return this;
	}

	protected void addLine() {
		buf.append(newLineStr);
	}

	protected SimpleCodeWriter addLineIndent() {
		buf.append(indentStr);
		return this;
	}

	private void updateIndent() {
		this.indentStr = Utils.strRepeat(singleIndentStr, indent);
	}

	@Override
	public void incIndent() {
		incIndent(1);
	}

	@Override
	public void decIndent() {
		decIndent(1);
	}

	private void incIndent(int c) {
		this.indent += c;
		updateIndent();
	}

	private void decIndent(int c) {
		this.indent -= c;
		if (this.indent < 0) {
			LOG.warn("Indent < 0");
			this.indent = 0;
		}
		updateIndent();
	}

	@Override
	public int getIndent() {
		return indent;
	}

	@Override
	public void setIndent(int indent) {
		this.indent = indent;
		updateIndent();
	}

	@Override
	public int getLine() {
		return 0;
	}

	@Override
	public int getLineStartPos() {
		return 0;
	}

	@Override
	public void attachDefinition(ICodeNodeRef obj) {
		// no op
	}

	@Override
	public void attachAnnotation(ICodeAnnotation obj) {
		// no op
	}

	@Override
	public void attachLineAnnotation(ICodeAnnotation obj) {
		// no op
	}

	@Override
	public void attachSourceLine(int sourceLine) {
		// no op
	}

	@Override
	public ICodeInfo finish() {
		String code = getStringWithoutFirstEmptyLine();
		buf = null;
		return new SimpleCodeInfo(code);
	}

	private String getStringWithoutFirstEmptyLine() {
		int len = newLineStr.length();
		if (buf.length() > len && buf.substring(0, len).equals(newLineStr)) {
			return buf.substring(len);
		}
		return buf.toString();
	}

	@Override
	public int getLength() {
		return buf.length();
	}

	@Override
	public StringBuilder getRawBuf() {
		return buf;
	}

	@Override
	public Map<Integer, ICodeAnnotation> getRawAnnotations() {
		return Collections.emptyMap();
	}

	@Override
	public String getCodeStr() {
		return buf.toString();
	}

	@Override
	public String toString() {
		return getCodeStr();
	}
}
