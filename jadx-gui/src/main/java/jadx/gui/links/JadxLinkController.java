package jadx.gui.links;

import java.awt.Frame;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.filedialog.FileDialogWrapper;
import jadx.gui.ui.filedialog.FileOpenMode;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

/**
 * Records hashes of opened APKs and handles jadx:// links: locate the APK by hash, open it,
 * and jump to the linked class/method. Also builds links for the "Copy jadx Link" action.
 */
public class JadxLinkController {
	private static final Logger LOG = LoggerFactory.getLogger(JadxLinkController.class);

	private final MainWindow mainWindow;
	private final ApkHashStore store = new ApkHashStore();
	/** hash -> path for APKs in the current project */
	private volatile Map<String, Path> loadedApks = Collections.emptyMap();

	public JadxLinkController(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	public ApkHashStore getStore() {
		return store;
	}

	/** Called in a background thread after project files are loaded. */
	public void onFilesLoaded(List<Path> files) {
		Map<String, Path> map = new LinkedHashMap<>();
		for (Path file : files) {
			if (ApkHashStore.isApk(file)) {
				try {
					map.put(store.record(file), file);
				} catch (Exception e) {
					LOG.warn("Failed to hash APK: {}", file, e);
				}
			}
		}
		loadedApks = map;
	}

	public void reset() {
		loadedApks = Collections.emptyMap();
	}

	public void handleLink(String linkStr) {
		UiUtils.uiThreadGuard();
		toFront();
		JadxLink link;
		try {
			link = JadxLink.parse(linkStr);
		} catch (IllegalArgumentException e) {
			UiUtils.errorMessage(mainWindow, NLS.str("jadx_link.title"), NLS.str("jadx_link.invalid", linkStr, e.getMessage()));
			return;
		}
		LOG.info("Opening jadx link: {}", link);
		for (Map.Entry<String, Path> e : loadedApks.entrySet()) {
			if (e.getKey().startsWith(link.getHashPrefix())) {
				jumpTo(link); // already open
				return;
			}
		}
		UiUtils.bgRun(() -> {
			Path apk = store.findByHashPrefix(link.getHashPrefix());
			UiUtils.uiRun(() -> {
				if (apk != null) {
					mainWindow.openForLink(apk, () -> jumpTo(link));
				} else {
					locateApk(link);
				}
			});
		});
	}

	private void jumpTo(JadxLink link) {
		UiUtils.bgRun(() -> {
			JavaNode node = mainWindow.getWrapper().getCurrentDecompiler()
					.map(d -> new JadxLinkResolver(d).resolve(link)).orElse(null);
			UiUtils.uiRun(() -> {
				if (node == null) {
					UiUtils.errorMessage(mainWindow, NLS.str("jadx_link.title"), NLS.str("jadx_link.target_not_found", link.toString()));
				} else {
					mainWindow.getTabsController().codeJump(mainWindow.getCacheObject().getNodeCache().makeFrom(node));
				}
			});
		});
	}

	private void locateApk(JadxLink link) {
		int res = JOptionPane.showConfirmDialog(mainWindow, NLS.str("jadx_link.apk_not_found", link.getHashPrefix()),
				NLS.str("jadx_link.title"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (res != JOptionPane.OK_OPTION) {
			return;
		}
		FileDialogWrapper dialog = new FileDialogWrapper(mainWindow, FileOpenMode.CUSTOM_OPEN);
		dialog.setFileExtList(Collections.singletonList("apk"));
		List<Path> files = dialog.show();
		if (files.isEmpty()) {
			return;
		}
		Path apk = files.get(0);
		UiUtils.bgRun(() -> {
			String hash;
			try {
				hash = ApkHashStore.calcHash(apk);
			} catch (Exception e) {
				UiUtils.uiRun(() -> UiUtils.errorMessage(mainWindow, e.getMessage()));
				return;
			}
			UiUtils.uiRun(() -> {
				if (hash.startsWith(link.getHashPrefix())) {
					mainWindow.openForLink(apk, () -> jumpTo(link));
				} else {
					UiUtils.errorMessage(mainWindow, NLS.str("jadx_link.title"),
							NLS.str("jadx_link.hash_mismatch", hash, link.getHashPrefix()));
				}
			});
		});
	}

	/**
	 * Build a link for a class/method node, asking which APK if several are loaded. Null if not
	 * possible.
	 */
	public @Nullable String buildLink(JNode node) {
		JavaNode javaNode = node.getJavaNode();
		String hash = selectApkHash();
		if (hash == null) {
			return null;
		}
		if (javaNode instanceof JavaMethod) {
			return JadxLink.build(hash, ((JavaMethod) javaNode).getMethodNode().getMethodInfo());
		}
		if (javaNode instanceof JavaClass) {
			return JadxLink.build(hash, ((JavaClass) javaNode).getClassNode().getClassInfo().getFullName());
		}
		return null;
	}

	private @Nullable String selectApkHash() {
		Map<String, Path> apks = loadedApks;
		if (apks.isEmpty()) {
			UiUtils.errorMessage(mainWindow, NLS.str("jadx_link.title"), NLS.str("jadx_link.no_apk_loaded"));
			return null;
		}
		if (apks.size() == 1) {
			return apks.keySet().iterator().next();
		}
		// classes are not mapped to input files, so ask which APK
		List<String> hashes = new java.util.ArrayList<>(apks.keySet());
		String[] names = hashes.stream()
				.map(h -> apks.get(h).getFileName() + " (" + h.substring(0, JadxLink.HASH_PREFIX_LEN) + ')')
				.toArray(String[]::new);
		Object sel = JOptionPane.showInputDialog(mainWindow, NLS.str("jadx_link.select_apk"), NLS.str("jadx_link.title"),
				JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
		for (int i = 0; i < names.length; i++) {
			if (names[i].equals(sel)) {
				return hashes.get(i);
			}
		}
		return null;
	}

	private void toFront() {
		mainWindow.setExtendedState(mainWindow.getExtendedState() & ~Frame.ICONIFIED);
		mainWindow.toFront();
		mainWindow.requestFocus();
	}
}
