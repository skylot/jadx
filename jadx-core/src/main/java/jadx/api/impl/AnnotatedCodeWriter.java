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
		for (Map.Entry<CodePosition, Object> entry : code.annotations.entrySet()) {
			Object val = entry.getValue();
			if (val instanceof DefinitionWrapper) {
				LineAttrNode node = ((DefinitionWrapper) val).getNode();
				node.setDefPosition(node.getDefPosition() + this.buf.length());
			}
			CodePosition pos = entry.getKey();
			int usagePos = pos.getUsagePosition() + getLength();
			attachAnnotation(val,
					new CodePosition(line + pos.getLine(), pos.getOffset())
							.setUsagePosition(usagePos));
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
		obj.setDefPosition(buf.length());
		attachAnnotation(obj);
		attachAnnotation(new DefinitionWrapper(obj), new CodePosition(line, offset));
	}

	@Override
	public void attachAnnotation(Object obj) {
		attachAnnotation(obj, new CodePosition(line, offset + 1).setUsagePosition(getLength()));
	}

	@Override
	public void attachLineAnnotation(Object obj) {
		attachAnnotation(obj, new CodePosition(line, 0));
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
}
