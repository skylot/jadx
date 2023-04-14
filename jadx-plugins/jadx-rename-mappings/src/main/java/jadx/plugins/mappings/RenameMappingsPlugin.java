package jadx.plugins.mappings;

import java.nio.file.Files;
import java.nio.file.Path;

import jadx.api.JadxArgs;
import jadx.api.args.UserRenamesMappingsMode;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.core.utils.files.FileUtils;
import jadx.plugins.mappings.load.ApplyMappingsPass;
import jadx.plugins.mappings.load.CodeMappingsPass;
import jadx.plugins.mappings.load.LoadMappingsPass;

public class RenameMappingsPlugin implements JadxPlugin {
	public static final String PLUGIN_ID = "rename-mappings";

	private final RenameMappingsOptions options = new RenameMappingsOptions();

	@Override
	public JadxPluginInfo getPluginInfo() {
		return new JadxPluginInfo(PLUGIN_ID, "Rename Mappings", "various mappings support");
	}

	@Override
	public void init(JadxPluginContext context) {
		context.registerOptions(options);
		JadxArgs args = context.getArgs();
		if (args.getUserRenamesMappingsMode() == UserRenamesMappingsMode.IGNORE) {
			return;
		}
		Path mappingsPath = args.getUserRenamesMappingsPath();
		if (mappingsPath == null || !Files.isReadable(mappingsPath)) {
			return;
		}
		context.addPass(new LoadMappingsPass(options));
		context.addPass(new ApplyMappingsPass());
		context.addPass(new CodeMappingsPass());

		// use mapping file time modification to check for changes
		context.registerInputsHashSupplier(() -> FileUtils.md5Sum(getInputsHashString(mappingsPath)));
	}

	private String getInputsHashString(Path mappingsPath) {
		return getFileHashString(mappingsPath) + ':' + options.getOptionsHashString();
	}

	private static String getFileHashString(Path mappingsPath) {
		try {
			return mappingsPath.toAbsolutePath().normalize()
					+ ":" + Files.getLastModifiedTime(mappingsPath).toMillis();
		} catch (Exception e) {
			return "";
		}
	}
}
