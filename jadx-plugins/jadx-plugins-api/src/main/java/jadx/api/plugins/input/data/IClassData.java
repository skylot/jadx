package jadx.api.plugins.input.data;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;

public interface IClassData {
	IClassData copy();

	String getInputFileName();

	String getType();

	int getAccessFlags();

	@Nullable
	String getSuperType();

	List<String> getInterfacesTypes();

	void visitFieldsAndMethods(Consumer<IFieldData> fieldsConsumer, Consumer<IMethodData> mthConsumer);

	List<IJadxAttribute> getAttributes();

	String getDisassembledCode();
}
