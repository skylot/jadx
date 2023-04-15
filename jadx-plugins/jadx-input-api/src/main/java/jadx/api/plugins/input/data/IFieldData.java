package jadx.api.plugins.input.data;

import java.util.List;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;

public interface IFieldData extends IFieldRef {

	int getAccessFlags();

	List<IJadxAttribute> getAttributes();
}
