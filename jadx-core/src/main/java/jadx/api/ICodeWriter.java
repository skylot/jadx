package jadx.api;

import java.util.Map;

import org.jetbrains.annotations.ApiStatus;

import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeNodeRef;

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

	/**
	 * Return current line (only if metadata is supported)
	 */
	int getLine();

	/**
	 * Return start line position (only if metadata is supported)
	 */
	int getLineStartPos();

	void attachDefinition(ICodeNodeRef obj);

	void attachAnnotation(ICodeAnnotation obj);

	void attachLineAnnotation(ICodeAnnotation obj);

	void attachSourceLine(int sourceLine);

	ICodeInfo finish();

	String getCodeStr();

	int getLength();

	StringBuilder getRawBuf();

	@ApiStatus.Internal
	Map<Integer, ICodeAnnotation> getRawAnnotations();
}
