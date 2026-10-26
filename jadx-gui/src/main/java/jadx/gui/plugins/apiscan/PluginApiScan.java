package jadx.gui.plugins.apiscan;

import org.jetbrains.annotations.Nullable;

import jadx.api.JadxDecompiler;
import jadx.api.plugins.pass.JadxPassInfo;
import jadx.api.plugins.pass.impl.SimpleJadxPassInfo;
import jadx.api.plugins.pass.types.JadxDecompilePass;
import jadx.api.plugins.pass.types.JadxPreparePass;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;

// Scan opened jadx plugin for dangerous API usage to help with plugin code review
public class PluginApiScan {
	private static final String PLUGIN_SERVICE_FILE = "META-INF/services/jadx.api.plugins.JadxPlugin";

	private volatile @Nullable ApiScanResult result;

	public void register(JadxDecompiler decompiler) {
		result = null;
		decompiler.addCustomPass(new ScanPass());
		decompiler.addCustomPass(new CommentsPass());
	}

	public void reset() {
		result = null;
	}

	// Scan result, null if opened files is not a jadx plugin
	public @Nullable ApiScanResult getResult() {
		return result;
	}

	private static boolean isJadxPlugin(RootNode root) {
		JadxDecompiler decompiler = root.getDecompiler();
		return decompiler != null
				&& decompiler.getResources().stream()
						.anyMatch(res -> res.getOriginalName().equals(PLUGIN_SERVICE_FILE));
	}

	private final class ScanPass implements JadxPreparePass {
		@Override
		public JadxPassInfo getInfo() {
			return new SimpleJadxPassInfo("PluginApiScan", "Scan jadx plugin for dangerous API usage");
		}

		@Override
		public void init(RootNode root) {
			result = isJadxPlugin(root) ? new ApiScanner(root).scan() : null;
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
