package jadx.gui.plugins.apiscan;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxDecompiler;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.api.plugins.pass.JadxPassInfo;
import jadx.api.plugins.pass.impl.OrderedJadxPassInfo;
import jadx.api.plugins.pass.impl.SimpleJadxPassInfo;
import jadx.api.plugins.pass.types.JadxDecompilePass;
import jadx.api.plugins.pass.types.JadxPreparePass;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.zip.IZipEntry;
import jadx.zip.ZipContent;

/**
 * Scan opened jadx plugin for dangerous API usage to help with plugin code review
 */
public class PluginApiScan implements JadxPlugin {
	private static final Logger LOG = LoggerFactory.getLogger(PluginApiScan.class);
	private static final String PLUGIN_SERVICE_FILE = "META-INF/services/jadx.api.plugins.JadxPlugin";

	private final List<String> entryClasses;
	private volatile @Nullable ApiScanResult result;

	public static void register(JadxDecompiler decompiler) {
		List<String> entryClasses = getPluginEntryClasses(decompiler);
		if (entryClasses != null) {
			decompiler.registerPlugin(new PluginApiScan(entryClasses));
		}
	}

	/**
	 * Plugin classes listed in services file, null if input is not a jadx plugin
	 */
	private static @Nullable List<String> getPluginEntryClasses(JadxDecompiler decompiler) {
		List<File> inputFiles = decompiler.getArgs().getInputFiles();
		if (inputFiles.size() == 1) {
			File inputFile = inputFiles.get(0);
			if (inputFile.getName().toLowerCase(Locale.ROOT).endsWith(".jar")) {
				try (ZipContent jar = decompiler.getZipReader().open(inputFile)) {
					IZipEntry serviceFile = jar.searchEntry(PLUGIN_SERVICE_FILE);
					if (serviceFile != null) {
						return parseServiceFile(serviceFile);
					}
				} catch (IOException e) {
					LOG.warn("Failed to load input jar: {}", inputFile, e);
				}
			}
		}
		return null;
	}

	private static List<String> parseServiceFile(IZipEntry serviceFile) {
		List<String> classes = new ArrayList<>();
		String content = new String(serviceFile.getBytes(), StandardCharsets.UTF_8);
		for (String line : content.split("\n")) {
			int commentStart = line.indexOf('#');
			String clsName = (commentStart == -1 ? line : line.substring(0, commentStart)).trim();
			if (!clsName.isEmpty()) {
				classes.add(clsName);
			}
		}
		return classes;
	}

	private PluginApiScan(List<String> entryClasses) {
		this.entryClasses = entryClasses;
	}

	@Override
	public JadxPluginInfo getPluginInfo() {
		return JadxPluginInfoBuilder.pluginId("jadx-plugin-api-scan")
				.name("Jadx Plugin API Scan")
				.description("Scan jadx plugin for dangerous API usage")
				.build();
	}

	@Override
	public void init(JadxPluginContext context) {
		context.addPass(new ScanPass());
		context.addPass(new CommentsPass());
	}

	@Override
	public void unload() {
		result = null;
	}

	// Scan result, null if opened files is not a jadx plugin
	public @Nullable ApiScanResult getResult() {
		return result;
	}

	private final class ScanPass implements JadxPreparePass {
		@Override
		public JadxPassInfo getInfo() {
			// anonymous classes marked by this pass are used for reachability
			return new OrderedJadxPassInfo("PluginApiScan", "Scan jadx plugin for dangerous API usage")
					.after("ProcessAnonymous");
		}

		@Override
		public void init(RootNode root) {
			result = new ApiScanner(root, entryClasses).scan();
		}
	}

	// Code comments removed on method unload, so add them on every decompilation
	private final class CommentsPass implements JadxDecompilePass {
		@Override
		public JadxPassInfo getInfo() {
			return new SimpleJadxPassInfo("PluginApiScanComments", "Add dangerous API usage comments");
		}

		@Override
		public void init(RootNode root) {
		}

		@Override
		public boolean visit(ClassNode cls) {
			return result != null;
		}

		@Override
		public void visit(MethodNode mth) {
			ApiScanResult scanResult = result;
			if (scanResult != null) {
				scanResult.getComments(mth).forEach(mth::addCodeComment);
			}
		}
	}
}
