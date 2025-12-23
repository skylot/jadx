package jadx.gui.report;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.ui.MainWindow;
import jadx.plugins.tools.JadxPluginsTools;
import jadx.plugins.tools.data.JadxPluginMetadata;

import static jadx.plugins.tools.JadxExternalPluginsLoader.JADX_PLUGIN_CLASSLOADER_PREFIX;

public class JadxExceptionHandler implements Thread.UncaughtExceptionHandler {
	private static final Logger LOG = LoggerFactory.getLogger(JadxExceptionHandler.class);

	public static final String MAIN_PROJECT_STRING = "skylot/jadx";

	public static void register(MainWindow mainWindow) {
		Thread.setDefaultUncaughtExceptionHandler(new JadxExceptionHandler(mainWindow));
	}

	private final MainWindow mainWindow;

	private JadxExceptionHandler(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		LOG.error("Exception was thrown", ex);
		new ExceptionDialog(mainWindow, buildExceptionData(ex));
	}

	private ExceptionData buildExceptionData(Throwable ex) {
		for (StackTraceElement stackTraceElement : ex.getStackTrace()) {
			String classLoaderName = stackTraceElement.getClassLoaderName();
			if (classLoaderName != null && classLoaderName.startsWith(JADX_PLUGIN_CLASSLOADER_PREFIX)) {
				// plugin exception
				String jarName = classLoaderName.substring(JADX_PLUGIN_CLASSLOADER_PREFIX.length());
				String pluginProject = resolvePluginByJarName(jarName);
				LOG.debug("Report exception in plugin: {}", pluginProject);
				return new ExceptionData(ex, pluginProject);
			}
		}
		return new ExceptionData(ex, MAIN_PROJECT_STRING);
	}

	private String resolvePluginByJarName(String jarName) {
		for (JadxPluginMetadata jadxPluginMetadata : JadxPluginsTools.getInstance().getInstalled()) {
			if (jadxPluginMetadata.getJar().equals(jarName)) {
				String githubProject = getGithubProject(jadxPluginMetadata.getLocationId());
				return githubProject != null ? githubProject : "";
			}
		}
		return "";
	}

	private static @Nullable String getGithubProject(String locationId) {
		if (locationId.startsWith("github:")) {
			return locationId.substring("github:".length()).replace(':', '/');
		}
		return null;
	}
}
