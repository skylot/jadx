package jadx.api.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import jadx.api.CodePosition;
import jadx.api.ICodeInfo;
import jadx.api.ICodeWriter;
import jadx.api.JadxArgs;
import jadx.core.dex.attributes.nodes.LineAttrNode;
import jadx.core.utils.StringUtils;

public class AnnotatedCodeWriter extends SimpleCodeWriter implements ICodeWriter {

	private int line = 1;
	private int offset;
	private Map<CodePosition, Object> annotations = Collections.emptyMap();
	private Map<Integer, Integer> lineMap = Collections.emptyMap();

	public AnnotatedCodeWriter() {
	}

	public AnnotatedCodeWriter(JadxArgs args) {
		super(args);
	}

	@Override
	public boolean isMetadataSupported() {
		return true;
	}

	@Override
	public AnnotatedCodeWriter addMultiLine(String str) {
		if (str.contains(NL)) {
			buf.append(str.replace(NL, NL + indentStr));
			line += StringUtils.countMatches(str, NL);
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
		if ((!(cw instanceof AnnotatedCodeWriter))) {
			buf.append(cw.getCodeStr());
			return this;
		}
		AnnotatedCodeWriter code = ((AnnotatedCodeWriter) cw);
		line--;
		int startLine = line;
		int startPos = getLength();
		for (Map.Entry<CodePosition, Object> entry : code.annotations.entrySet()) {
			CodePosition codePos = entry.getKey();
			int newLine = startLine + codePos.getLine();
			int newPos = startPos + codePos.getPos();
			attachAnnotation(entry.getValue(), new CodePosition(newLine, codePos.getOffset(), newPos));
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
		buf.append(NL);
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

	private static final class DefinitionWrapper {
		private final LineAttrNode node;

		private DefinitionWrapper(LineAttrNode node) {
			this.node = node;
		}

		public LineAttrNode getNode() {
			return node;
		}
	}

	@Override
	public void attachDefinition(LineAttrNode obj) {
		attachAnnotation(obj);
		attachAnnotation(new DefinitionWrapper(obj), new CodePosition(line, offset, getLength()));
	}

	@Override
	public void attachAnnotation(Object obj) {
		attachAnnotation(obj, new CodePosition(line, offset + 1, getLength()));
	}

	@Override
	public void attachLineAnnotation(Object obj) {
		if (obj == null) {
			return;
		}
		attachAnnotation(obj, new CodePosition(line, 0, getLength() - offset));
	}

	private void attachAnnotation(Object obj, CodePosition pos) {
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
		removeFirstEmptyLine();
		processDefinitionAnnotations();
		String code = buf.toString();
		buf = null;
		return new AnnotatedCodeInfo(code, lineMap, annotations);
	}

	@Override
	public Map<CodePosition, Object> getRawAnnotations() {
		return annotations;
	}

	private void processDefinitionAnnotations() {
		if (!annotations.isEmpty()) {
			annotations.entrySet().removeIf(entry -> {
				Object v = entry.getValue();
				if (v instanceof DefinitionWrapper) {
					LineAttrNode l = ((DefinitionWrapper) v).getNode();
					CodePosition codePos = entry.getKey();
					l.setDecompiledLine(codePos.getLine());
					l.setDefPosition(codePos.getPos());
					return true;
				}
				return false;
			});
		}
	}
}
