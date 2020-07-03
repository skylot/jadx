package jadx.api.plugins.input.data;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.annotations.IAnnotation;

public interface IMethodData {
	String getParentClassType();

	String getName();

	int getAccessFlags();

	String getReturnType();

	List<String> getArgTypes();

	boolean isDirect();

	@Nullable
	ICodeReader getCodeReader();

	String disassembleMethod();

	List<IAnnotation> getAnnotations();

	List<List<IAnnotation>> getParamsAnnotations();
}
