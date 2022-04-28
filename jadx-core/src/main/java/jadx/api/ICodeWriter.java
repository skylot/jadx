package jadx.api;

import java.util.Map;

import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeDefinition;

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

	void attachDefinition(ICodeDefinition obj);

	void attachAnnotation(ICodeAnnotation obj);

	void attachLineAnnotation(ICodeAnnotation obj);

	void attachSourceLine(int sourceLine);

	ICodeInfo finish();

	String getCodeStr();

	int getLength();

	StringBuilder getRawBuf();

	Map<CodePosition, ICodeAnnotation> getRawAnnotations();
}
