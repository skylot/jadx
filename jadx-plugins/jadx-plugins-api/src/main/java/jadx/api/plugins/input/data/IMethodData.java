package jadx.api.plugins.input.data;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;

public interface IMethodData {

	IMethodRef getMethodRef();

	int getAccessFlags();

	@Nullable
	ICodeReader getCodeReader();

	String disassembleMethod();

	List<IJadxAttribute> getAttributes();
}
