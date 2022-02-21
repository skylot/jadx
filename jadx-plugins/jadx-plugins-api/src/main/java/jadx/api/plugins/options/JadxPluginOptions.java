package jadx.api.plugins.options;

import java.util.List;
import java.util.Map;

import jadx.api.plugins.JadxPlugin;

public interface JadxPluginOptions extends JadxPlugin {

	void setOptions(Map<String, String> options);

	List<OptionDescription> getOptionsDescriptions();
}
