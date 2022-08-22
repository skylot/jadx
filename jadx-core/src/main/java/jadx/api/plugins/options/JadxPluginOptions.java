package jadx.api.plugins.options;

import java.util.List;
import java.util.Map;

public interface JadxPluginOptions {

	void setOptions(Map<String, String> options);

	List<OptionDescription> getOptionsDescriptions();
}
