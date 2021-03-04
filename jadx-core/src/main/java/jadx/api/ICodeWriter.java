package jadx.api;

import java.util.Map;

import jadx.core.dex.attributes.nodes.LineAttrNode;

public interface ICodeWriter {
	String NL = System.getProperty("line.separator");
	String INDENT_STR = "    ";

	boolean isMetadataSupported();

	ICodeWriter startLine();

	ICodeWriter startLine(char c);

	ICodeWriter startLine(String str);

	ICodeWriter startLineWithNum(int sourceLine);

	ICodeWriter addMultiLine(String str);

	ICodeWriter add(String str);

	ICodeWriter add(char c);

	ICodeWriter add(ICodeWriter code);

	ICodeWriter newLine();

	ICodeWriter addIndent();

	void incIndent();

	void decIndent();

	int getIndent();

	void setIndent(int indent);

	int getLine();

	void attachDefinition(LineAttrNode obj);

	void attachAnnotation(Object obj);

	void attachLineAnnotation(Object obj);

	void attachSourceLine(int sourceLine);

	ICodeInfo finish();

	String getCodeStr();

	int getLength();

	StringBuilder getRawBuf();

	Map<CodePosition, Object> getRawAnnotations();
}
