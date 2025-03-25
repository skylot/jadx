package jadx.cli.commands;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import jadx.api.plugins.JadxPluginInfo;
import jadx.cli.JCommanderWrapper;
import jadx.cli.LogHelper;
import jadx.core.utils.StringUtils;
import jadx.plugins.tools.JadxPluginsList;
import jadx.plugins.tools.JadxPluginsTools;
import jadx.plugins.tools.data.JadxPluginMetadata;
import jadx.plugins.tools.data.JadxPluginUpdate;

@Parameters(commandDescription = "manage jadx plugins")
public class CommandPlugins implements ICommand {

	@Parameter(names = { "-i", "--install" }, description = "install plugin with locationId", defaultValueDescription = "<locationId>")
	protected String install;

	@Parameter(names = { "-j", "--install-jar" }, description = "install plugin from jar file", defaultValueDescription = "<path-to.jar>")
	protected String installJar;

	@Parameter(names = { "-l", "--list" }, description = "list installed plugins")
	protected boolean list;

	@Parameter(names = { "-a", "--available" }, description = "list available plugins from jadx-plugins-list (aka marketplace)")
	protected boolean available;

	@Parameter(names = { "-u", "--update" }, description = "update installed plugins")
	protected boolean update;

	@Parameter(names = { "--uninstall" }, description = "uninstall plugin with pluginId", defaultValueDescription = "<pluginId>")
	protected String uninstall;

	@Parameter(names = { "--disable" }, description = "disable plugin with pluginId", defaultValueDescription = "<pluginId>")
	protected String disable;

	@Parameter(names = { "--enable" }, description = "enable plugin with pluginId", defaultValueDescription = "<pluginId>")
	protected String enable;

	@Parameter(names = { "--list-all" }, description = "list all plugins including bundled and dropins")
	protected boolean listAll;

	@Parameter(
			names = { "--list-versions" },
			description = "fetch latest versions of plugin from locationId (will download all artefacts, limited to 10)",
			defaultValueDescription = "<locationId>"
	)
	protected String listVersions;

	@Parameter(names = { "-h", "--help" }, description = "print this help", help = true)
	protected boolean printHelp = false;

	@Override
	public String name() {
		return "plugins";
	}

	@SuppressWarnings("UnnecessaryReturnStatement")
	@Override
	public void process(JCommanderWrapper jcw, JCommander subCommander) {
		if (printHelp) {
			jcw.printUsage(subCommander);
			return;
		}
		Set<String> unknownOptions = new HashSet<>(subCommander.getUnknownOptions());
		boolean verbose = unknownOptions.remove("-v") || unknownOptions.remove("--verbose");
		LogHelper.setLogLevel(verbose ? LogHelper.LogLevelEnum.DEBUG : LogHelper.LogLevelEnum.INFO);

		if (!unknownOptions.isEmpty()) {
			System.out.println("Error: found unknown options: " + unknownOptions);
		}

		if (install != null) {
			installPlugin(install);
			return;
		}
		if (installJar != null) {
			installPlugin("file:" + installJar);
			return;
		}
		if (uninstall != null) {
			boolean uninstalled = JadxPluginsTools.getInstance().uninstall(uninstall);
			System.out.println(uninstalled ? "Uninstalled" : "Plugin not found");
			return;
		}
		if (update) {
			List<JadxPluginUpdate> updates = JadxPluginsTools.getInstance().updateAll();
			if (updates.isEmpty()) {
				System.out.println("No updates");
			} else {
				System.out.println("Installed updates: " + updates.size());
				for (JadxPluginUpdate update : updates) {
					System.out.println("  " + update.getPluginId() + ": " + update.getOldVersion() + " -> " + update.getNewVersion());
				}
			}
			return;
		}
		if (list) {
			printPlugins(JadxPluginsTools.getInstance().getInstalled());
			return;
		}
		if (listAll) {
			printAllPlugins();
			return;
		}
		if (listVersions != null) {
			printVersions(listVersions, 10);
			return;
		}
		if (available) {
			List<JadxPluginMetadata> availableList = JadxPluginsList.getInstance().get();
			System.out.println("Available plugins: " + availableList.size());
			for (JadxPluginMetadata plugin : availableList) {
				System.out.println(" - " + plugin.getName() + ": " + plugin.getDescription()
						+ " (" + plugin.getLocationId() + ")");
			}
			return;
		}

		if (disable != null) {
			if (JadxPluginsTools.getInstance().changeDisabledStatus(disable, true)) {
				System.out.println("Plugin '" + disable + "' disabled.");
			} else {
				System.out.println("Plugin '" + disable + "' already disabled.");
			}
			return;
		}
		if (enable != null) {
			if (JadxPluginsTools.getInstance().changeDisabledStatus(enable, false)) {
				System.out.println("Plugin '" + enable + "' enabled.");
			} else {
				System.out.println("Plugin '" + enable + "' already enabled.");
			}
			return;
		}
	}

	private static void printPlugins(List<JadxPluginMetadata> installed) {
		System.out.println("Installed plugins: " + installed.size());
		for (JadxPluginMetadata plugin : installed) {
			StringBuilder sb = new StringBuilder();
			sb.append(" - ").append(plugin.getPluginId());
			String version = plugin.getVersion();
			if (version != null) {
				sb.append(" (").append(version).append(')');
			}
			if (plugin.isDisabled()) {
				sb.append(" (disabled)");
			}
			sb.append(" - ").append(plugin.getName());
			sb.append(": ").append(formatDescription(plugin.getDescription()));
			System.out.println(sb);
		}
	}

	private void printVersions(String locationId, int limit) {
		System.out.println("Loading ...");
		List<JadxPluginMetadata> versions = JadxPluginsTools.getInstance().getVersionsByLocation(locationId, 1, limit);
		if (versions.isEmpty()) {
			System.out.println("No versions found");
			return;
		}
		JadxPluginMetadata plugin = versions.get(0);
		System.out.println("Versions for plugin id: " + plugin.getPluginId());
		for (JadxPluginMetadata version : versions) {
			StringBuilder sb = new StringBuilder();
			sb.append(" - ").append(version.getVersion());
			String reqVer = version.getRequiredJadxVersion();
			if (StringUtils.notBlank(reqVer)) {
				sb.append(", require jadx: ").append(reqVer);
			}
			System.out.println(sb);
		}
	}

	private static void printAllPlugins() {
		List<JadxPluginMetadata> installed = JadxPluginsTools.getInstance().getInstalled();
		printPlugins(installed);
		Set<String> installedSet = installed.stream().map(JadxPluginMetadata::getPluginId).collect(Collectors.toSet());

		List<JadxPluginInfo> plugins = JadxPluginsTools.getInstance().getAllPluginsInfo();
		System.out.println("Other plugins: " + plugins.size());
		for (JadxPluginInfo plugin : plugins) {
			if (!installedSet.contains(plugin.getPluginId())) {
				System.out.println(" - " + plugin.getPluginId()
						+ " - " + plugin.getName()
						+ ": " + formatDescription(plugin.getDescription()));
			}
		}
	}

	private static String formatDescription(String desc) {
		if (desc.contains("\n")) {
			// remove new lines
			desc = desc.replaceAll("\\R+", " ");
		}
		int maxLen = 512;
		if (desc.length() > maxLen) {
			// truncate very long descriptions
			desc = desc.substring(0, maxLen) + " ...";
		}
		return desc;
	}

	private void installPlugin(String locationId) {
		JadxPluginMetadata plugin = JadxPluginsTools.getInstance().install(locationId);
		System.out.println("Plugin installed: " + plugin.getPluginId() + ":" + plugin.getVersion());
	}
}
