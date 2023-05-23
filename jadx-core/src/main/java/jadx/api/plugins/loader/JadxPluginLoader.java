package jadx.api.plugins.loader;

import java.io.Closeable;
import java.util.List;

import jadx.api.plugins.JadxPlugin;

public interface JadxPluginLoader extends Closeable {

	List<JadxPlugin> load();
}
