package jadx.api.plugins.input.data;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.annotations.IAnnotation;

public interface IClassData {
	IClassData copy();

	String getType();

	int getAccessFlags();

	@Nullable
	String getSuperType();

	List<String> getInterfacesTypes();

	String getSourceFile();

	Path getInputPath();

	void visitFieldsAndMethods(Consumer<IFieldData> fieldsConsumer, Consumer<IMethodData> mthConsumer);

	List<EncodedValue> getStaticFieldInitValues();

	List<IAnnotation> getAnnotations();

	// TODO: make api methods to get dissembled code
	int getClassDefOffset();
}
