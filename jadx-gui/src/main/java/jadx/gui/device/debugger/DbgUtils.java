package jadx.gui.device.debugger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jetbrains.annotations.Nullable;

import jadx.api.JavaClass;
import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.gui.device.debugger.smali.Smali;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;

public class DbgUtils {

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
		int pos = fullSig.indexOf("(");
		if (pos != -1) {
			pos = fullSig.lastIndexOf(".", pos);
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
			JClass jc = (JClass) mainWindow.getCacheObject().getNodeCache().makeFrom(cls);
			return jc.getRootClass();
		}
		return null;
	}

	public static ClassNode getClassNodeBySig(String clsSig, MainWindow mainWindow) {
		clsSig = DbgUtils.classSigToFullName(clsSig);
		return mainWindow.getWrapper().getDecompiler().searchClassNodeByOrigFullName(clsSig);
	}

	public static String searchPackageName(MainWindow mainWindow) {
		String content = getManifestContent(mainWindow);
		int pos = content.indexOf("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" ");
		if (pos > -1) {
			pos = content.lastIndexOf(">", pos);
			if (pos > -1) {
				pos = content.indexOf(" package=\"", pos);
				if (pos > -1) {
					pos += " package=\"".length();
					return content.substring(pos, content.indexOf("\"", pos));
				}
			}
		}
		return "";
	}

	/**
	 * @return the Activity class for android.intent.action.MAIN.
	 */
	@Nullable
	public static JClass searchMainActivity(MainWindow mainWindow) {
		String content = getManifestContent(mainWindow);
		int pos = content.indexOf("<action android:name=\"android.intent.action.MAIN\"");
		if (pos > -1) {
			pos = content.lastIndexOf("<activity ", pos);
			if (pos > -1) {
				pos = content.indexOf(" android:name=\"", pos);
				if (pos > -1) {
					pos += " android:name=\"".length();
					String classFullName = content.substring(pos, content.indexOf("\"", pos));
					// in case the MainActivity class has been renamed before, we need raw name.
					JavaClass cls = mainWindow.getWrapper().getDecompiler().searchJavaClassByAliasFullName(classFullName);
					JNode jNode = mainWindow.getCacheObject().getNodeCache().makeFrom(cls);
					if (jNode != null) {
						return jNode.getRootClass();
					}
				}
			}
		}
		return null;
	}

	// TODO: parse AndroidManifest.xml instead of looking for keywords
	private static String getManifestContent(MainWindow mainWindow) {
		try {
			ResourceFile androidManifest = mainWindow.getWrapper().getDecompiler().getResources()
					.stream()
					.filter(res -> res.getType() == ResourceType.MANIFEST)
					.findFirst()
					.orElse(null);

			if (androidManifest != null) {
				return androidManifest.loadContent().getText().getCodeStr();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public static boolean isPrintableChar(int c) {
		return 32 <= c && c <= 126;
	}
}
