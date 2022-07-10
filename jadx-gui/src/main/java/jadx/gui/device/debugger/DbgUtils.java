package jadx.gui.device.debugger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			JClass jc = mainWindow.getCacheObject().getNodeCache().makeFrom(cls);
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
		int pos; // current position
		int actionPos = 0; // last found action's index
		String actionTag = "<action android:name=\"android.intent.action.MAIN\"";
		int actionTagLen = 0; // beginning offset. suggested length set after first iteration
		while (actionPos > -1) {
			pos = content.indexOf(actionTag, actionPos + actionTagLen);
			actionPos = pos;
			int activityPos = content.lastIndexOf("<activity ", pos);
			if (activityPos > -1) {
				int aliasPos = content.lastIndexOf("<activity-alias ", pos);
				boolean isAnAlias = aliasPos > -1 && aliasPos > activityPos;
				String classPathAttribute = " android:" + (isAnAlias ? "targetActivity" : "name") + "=\"";
				pos = content.indexOf(classPathAttribute, isAnAlias ? aliasPos : activityPos);
				if (pos > -1) {
					pos += classPathAttribute.length();
					String classFullName = content.substring(pos, content.indexOf("\"", pos));
					// in case the MainActivity class has been renamed before, we need raw name.
					JavaClass cls = mainWindow.getWrapper().getDecompiler().searchJavaClassByAliasFullName(classFullName);
					JNode jNode = mainWindow.getCacheObject().getNodeCache().makeFrom(cls);
					if (jNode != null) {
						return jNode.getRootClass();
					}
				}
			}
			if (actionTagLen == 0) {
				actionTagLen = actionTag.length();
			}
		}
		return null;
	}

	// TODO: parse AndroidManifest.xml instead of looking for keywords
	private static String getManifestContent(MainWindow mainWindow) {
		try {
			ResourceFile androidManifest = mainWindow.getWrapper().getResources()
					.stream()
					.filter(res -> res.getType() == ResourceType.MANIFEST)
					.findFirst()
					.orElse(null);

			if (androidManifest != null) {
				return androidManifest.loadContent().getText().getCodeStr();
			}
		} catch (Exception e) {
			LOG.error("AndroidManifest.xml search error", e);
		}
		return "";
	}

	public static boolean isPrintableChar(int c) {
		return 32 <= c && c <= 126;
	}
}
