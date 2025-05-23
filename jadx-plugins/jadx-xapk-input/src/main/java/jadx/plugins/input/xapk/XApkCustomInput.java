package jadx.plugins.input.xapk;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import jadx.api.ResourceFile;
import jadx.api.ResourcesLoader;
import jadx.api.plugins.CustomResourcesLoader;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.input.ICodeLoader;
import jadx.api.plugins.input.JadxCodeInput;
import jadx.api.plugins.input.data.impl.EmptyCodeLoader;
import jadx.plugins.input.dex.DexInputPlugin;
import jadx.plugins.input.xapk.data.XApkData;

public class XApkCustomInput implements JadxCodeInput, CustomResourcesLoader {
	private final JadxPluginContext context;
	private final XApkLoader loader;

	public XApkCustomInput(JadxPluginContext context, XApkLoader loader) {
		this.context = context;
		this.loader = loader;
	}

	@Override
	public ICodeLoader loadFiles(List<Path> input) {
		List<Path> apks = new ArrayList<>();
		for (Path inputPath : input) {
			XApkData data = loader.checkAndLoad(inputPath);
			if (data != null) {
				apks.addAll(data.getApks());
			}
		}
		if (apks.isEmpty()) {
			return EmptyCodeLoader.INSTANCE;
		}
		DexInputPlugin dexInputPlugin = context.plugins().getInstance(DexInputPlugin.class);
		return dexInputPlugin.loadFiles(apks);
	}

	@Override
	public boolean load(ResourcesLoader resLoader, List<ResourceFile> list, File file) {
		XApkData xApkData = loader.checkAndLoad(file.toPath());
		if (xApkData == null) {
			return false;
		}
		for (Path apkPath : xApkData.getApks()) {
			resLoader.defaultLoadFile(list, apkPath.toFile(), apkPath.getFileName() + "/");
		}
		for (Path filePath : xApkData.getFiles()) {
			resLoader.defaultLoadFile(list, filePath.toFile(), "");
		}
		return true;
	}

	@Override
	public void close() throws IOException {
	}
}
