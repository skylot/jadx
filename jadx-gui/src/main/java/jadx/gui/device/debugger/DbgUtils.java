package jadx.gui.device.debugger;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.core.deobf.NameMapper;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.utils.android.AndroidManifestParser;
import jadx.core.utils.android.AppAttribute;
import jadx.core.utils.android.ApplicationParams;
import jadx.gui.device.debugger.smali.Smali;
import jadx.gui.treemodel.JClass;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class DbgUtils {
	private static final Logger LOG = LoggerFactory.getLogger(DbgUtils.class);

	private static Map<ClassInfo, Smali> smaliCache = Collections.emptyMap();

	protected static Smali getSmali(ClassNode topCls) {
		if (smaliCache == Collections.EMPTY_MAP) {
			smaliCache = new HashMap<>();
		}
		return smaliCache.computeIfAbsent(topCls.getTopParentClass().getClassInfo(),
				c -> Smali.disassemble(topCls));
	}

	public static String getSmaliCode(ClassNode topCls) {
		Smali smali = getSmali(topCls);
		if (smali != null) {
			return smali.getCode();
		}
		return null;
	}

	public static Entry<String, Integer> getCodeOffsetInfoByLine(JClass cls, int line) {
		Smali smali = getSmali(cls.getCls().getClassNode().getTopParentClass());
		if (smali != null) {
			return smali.getMthFullIDAndCodeOffsetByLine(line);
		}
		return null;
	}

	public static String[] sepClassAndMthSig(String fullSig) {
		int pos = fullSig.indexOf('(');
		if (pos != -1) {
			pos = fullSig.lastIndexOf('.', pos);
			if (pos != -1) {
				String[] sigs = new String[2];
				sigs[0] = fullSig.substring(0, pos);
				sigs[1] = fullSig.substring(pos + 1);
				return sigs;
			}
		}
		return null;
	}

	// doesn't replace $
	public static String classSigToRawFullName(String clsSig) {
		if (clsSig != null && clsSig.startsWith("L") && clsSig.endsWith(";")) {
			clsSig = clsSig.substring(1, clsSig.length() - 1)
					.replace("/", ".");
		}
		return clsSig;
	}

	// replaces $
	public static String classSigToFullName(String clsSig) {
		if (clsSig != null && clsSig.startsWith("L") && clsSig.endsWith(";")) {
			clsSig = clsSig.substring(1, clsSig.length() - 1)
					.replace("/", ".")
					.replace("$", ".");
		}
		return clsSig;
	}

	public static String getRawFullName(JClass topCls) {
		return topCls.getCls().getClassNode().getClassInfo().makeRawFullName();
	}

	public static boolean isStringObjectSig(String objectSig) {
		return objectSig.equals("Ljava/lang/String;");
	}

	public static JClass getTopClassBySig(String clsSig, MainWindow mainWindow) {
		clsSig = DbgUtils.classSigToFullName(clsSig);
		JavaClass cls = mainWindow.getWrapper().getDecompiler().searchJavaClassOrItsParentByOrigFullName(clsSig);
		if (cls != null) {
			JClass jc = mainWindow.getCacheObject().getNodeCache().makeFrom(cls);
			return jc.getRootClass();
		}
		return null;
	}

	public static JClass getJClass(JavaClass cls, MainWindow mainWindow) {
		return mainWindow.getCacheObject().getNodeCache().makeFrom(cls);
	}

	public static ClassNode getClassNodeBySig(String clsSig, MainWindow mainWindow) {
		clsSig = DbgUtils.classSigToFullName(clsSig);
		return mainWindow.getWrapper().getDecompiler().searchClassNodeByOrigFullName(clsSig);
	}

	public static boolean isPrintableChar(int c) {
		return 32 <= c && c <= 126;
	}

	public static final class AppData {
		private final String appPackage;
		private final JavaClass mainActivityCls;

		public AppData(String appPackage, JavaClass mainActivityCls) {
			this.appPackage = appPackage;
			this.mainActivityCls = mainActivityCls;
		}

		public String getAppPackage() {
			return appPackage;
		}

		public JavaClass getMainActivityCls() {
			return mainActivityCls;
		}

		public String getProcessName() {
			return appPackage + '/' + mainActivityCls.getClassNode().getClassInfo().getFullName();
		}
	}

	public static @Nullable AppData parseAppData(MainWindow mw) {
		JadxDecompiler decompiler = mw.getWrapper().getDecompiler();
		String appPkg = decompiler.getRoot().getAppPackage();
		if (appPkg == null) {
			UiUtils.errorMessage(mw, NLS.str("error_dialog.not_found", "App package"));
			return null;
		}
		AndroidManifestParser parser = new AndroidManifestParser(
				AndroidManifestParser.getAndroidManifest(decompiler.getResources()),
				EnumSet.of(AppAttribute.MAIN_ACTIVITY));
		if (!parser.isManifestFound()) {
			UiUtils.errorMessage(mw, NLS.str("error_dialog.not_found", "AndroidManifest.xml"));
			return null;
		}
		ApplicationParams results = parser.parse();
		String mainActivityName = results.getMainActivity();
		if (mainActivityName == null) {
			UiUtils.errorMessage(mw, NLS.str("adb_dialog.msg_read_mani_fail"));
			return null;
		}
		if (!NameMapper.isValidFullIdentifier(mainActivityName)) {
			UiUtils.errorMessage(mw, "Invalid main activity name");
			return null;
		}
		JavaClass mainActivityClass = results.getMainActivityJavaClass(decompiler);
		if (mainActivityClass == null) {
			UiUtils.errorMessage(mw, NLS.str("error_dialog.not_found", "Main activity class"));
			return null;
		}
		return new AppData(appPkg, mainActivityClass);
	}
}
