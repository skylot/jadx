package jadx.api.plugins.input.data;

import java.util.List;

import jadx.api.plugins.input.data.annotations.EncodedValue;

public interface ICallSite {

	List<EncodedValue> getValues();

	void load();
}
