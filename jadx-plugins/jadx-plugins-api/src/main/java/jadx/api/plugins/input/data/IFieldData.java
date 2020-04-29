package jadx.api.plugins.input.data;

import java.util.List;

import jadx.api.plugins.input.data.annotations.IAnnotation;

public interface IFieldData {
	String getParentClassType();

	String getType();

	String getName();

	int getAccessFlags();

	List<IAnnotation> getAnnotations();
}
