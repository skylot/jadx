package jadx.api.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import jadx.api.ICodeInfo;
import jadx.api.ICodeWriter;
import jadx.api.JadxArgs;
import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.core.utils.StringUtils;

public class AnnotatedCodeWriter extends SimpleCodeWriter implements ICodeWriter {

	private int line = 1;
	private int offset;
	private Map<Integer, ICodeAnnotation> annotations = Collections.emptyMap();
	private Map<Integer, Integer> lineMap = Collections.emptyMap();

	public AnnotatedCodeWriter(JadxArgs args) {
		super(args);
	}

	@Override
	public boolean isMetadataSupported() {
		return true;
	}

	@Override
	public AnnotatedCodeWriter addMultiLine(String str) {
		if (str.contains(newLineStr)) {
			buf.append(str.replace(newLineStr, newLineStr + indentStr));
			line += StringUtils.countMatches(str, newLineStr);
			offset = 0;
		} else {
			buf.append(str);
		}
		return this;
	}

	@Override
	public AnnotatedCodeWriter add(String str) {
		buf.append(str);
		offset += str.length();
		return this;
	}

	@Override
	public AnnotatedCodeWriter add(char c) {
		buf.append(c);
		offset++;
		return this;
	}

	@Override
	public ICodeWriter add(ICodeWriter cw) {
		if (!cw.isMetadataSupported()) {
			buf.append(cw.getCodeStr());
			return this;
		}
		AnnotatedCodeWriter code = (AnnotatedCodeWriter) cw;
		line--;
		int startPos = getLength();
		for (Map.Entry<Integer, ICodeAnnotation> entry : code.annotations.entrySet()) {
			int pos = entry.getKey();
			int newPos = startPos + pos;
			attachAnnotation(entry.getValue(), newPos);
		}
		for (Map.Entry<Integer, Integer> entry : code.lineMap.entrySet()) {
			attachSourceLine(line + entry.getKey(), entry.getValue());
		}
		line += code.line;
		offset = code.offset;
		buf.append(code.buf);
		return this;
	}

	@Override
	protected void addLine() {
		buf.append(newLineStr);
		line++;
		offset = 0;
	}

	@Override
	protected AnnotatedCodeWriter addLineIndent() {
		buf.append(indentStr);
		offset += indentStr.length();
		return this;
	}

	@Override
	public int getLine() {
		return line;
	}

	@Override
	public int getLineStartPos() {
		return getLength() - offset;
	}

	@Override
	public void attachDefinition(ICodeNodeRef obj) {
		if (obj == null) {
			return;
		}
		attachAnnotation(new NodeDeclareRef(obj));
	}

	@Override
	public void attachAnnotation(ICodeAnnotation obj) {
		if (obj == null) {
			return;
		}
		attachAnnotation(obj, getLength());
	}

	@Override
	public void attachLineAnnotation(ICodeAnnotation obj) {
		if (obj == null) {
			return;
		}
		attachAnnotation(obj, getLineStartPos());
	}

	private void attachAnnotation(ICodeAnnotation obj, int pos) {
		if (annotations.isEmpty()) {
			annotations = new HashMap<>();
		}
		annotations.put(pos, obj);
	}

	@Override
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

	@Override
	public ICodeInfo finish() {
		String code = buf.toString();
		buf = null;
		return new AnnotatedCodeInfo(code, lineMap, annotations);
	}

	@Override
	public Map<Integer, ICodeAnnotation> getRawAnnotations() {
		return annotations;
	}
}
