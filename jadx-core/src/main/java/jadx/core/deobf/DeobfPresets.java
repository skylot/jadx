package jadx.core.deobf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;

class DeobfPresets {
	private static final Logger LOG = LoggerFactory.getLogger(DeobfPresets.class);

	private static final String MAP_FILE_CHARSET = "UTF-8";

	private final Deobfuscator deobfuscator;
	private final File deobfMapFile;

	private final Map<String, String> clsPresetMap = new HashMap<>();
	private final Map<String, String> fldPresetMap = new HashMap<>();
	private final Map<String, String> mthPresetMap = new HashMap<>();

	public DeobfPresets(Deobfuscator deobfuscator, File deobfMapFile) {
		this.deobfuscator = deobfuscator;
		this.deobfMapFile = deobfMapFile;
	}

	/**
	 * Loads deobfuscator presets
	 */
	public void load() {
		if (!deobfMapFile.exists()) {
			return;
		}
		LOG.info("Loading obfuscation map from: {}", deobfMapFile.getAbsoluteFile());
		try {
			List<String> lines = FileUtils.readLines(deobfMapFile, MAP_FILE_CHARSET);
			for (String l : lines) {
				l = l.trim();
				if (l.isEmpty() || l.startsWith("#")) {
					continue;
				}
				String[] va = splitAndTrim(l);
				if (va.length != 2) {
					continue;
				}
				String origName = va[0];
				String alias = va[1];
				if (l.startsWith("p ")) {
					deobfuscator.addPackagePreset(origName, alias);
				} else if (l.startsWith("c ")) {
					clsPresetMap.put(origName, alias);
				} else if (l.startsWith("f ")) {
					fldPresetMap.put(origName, alias);
				} else if (l.startsWith("m ")) {
					mthPresetMap.put(origName, alias);
				}
			}
		} catch (IOException e) {
			LOG.error("Failed to load deobfuscation map file '{}'", deobfMapFile.getAbsolutePath(), e);
		}
	}

	private static String[] splitAndTrim(String str) {
		String[] v = str.substring(2).split("=");
		for (int i = 0; i < v.length; i++) {
			v[i] = v[i].trim();
		}
		return v;
	}

	public void save(boolean forceSave) {
		try {
			if (deobfMapFile.exists()) {
				if (forceSave) {
					dumpMapping();
				} else {
					LOG.warn("Deobfuscation map file '{}' exists. Use command line option '--deobf-rewrite-cfg' to rewrite it",
							deobfMapFile.getAbsolutePath());
				}
			} else {
				dumpMapping();
			}
		} catch (IOException e) {
			LOG.error("Failed to load deobfuscation map file '{}'", deobfMapFile.getAbsolutePath(), e);
		}
	}

	/**
	 * Saves DefaultDeobfuscator presets
	 */
	private void dumpMapping() throws IOException {
		List<String> list = new ArrayList<>();
		// packages
		for (PackageNode p : deobfuscator.getRootPackage().getInnerPackages()) {
			for (PackageNode pp : p.getInnerPackages()) {
				dfsPackageName(list, p.getName(), pp);
			}
			if (p.hasAlias()) {
				list.add(String.format("p %s = %s", p.getName(), p.getAlias()));
			}
		}
		// classes
		for (DeobfClsInfo deobfClsInfo : deobfuscator.getClsMap().values()) {
			if (deobfClsInfo.getAlias() != null) {
				list.add(String.format("c %s = %s",
						deobfClsInfo.getCls().getClassInfo().getFullName(), deobfClsInfo.getAlias()));
			}
		}
		for (FieldInfo fld : deobfuscator.getFldMap().keySet()) {
			list.add(String.format("f %s = %s", fld.getFullId(), fld.getAlias()));
		}
		for (MethodInfo mth : deobfuscator.getMthMap().keySet()) {
			list.add(String.format("m %s = %s", mth.getFullId(), mth.getAlias()));
		}
		Collections.sort(list);
		FileUtils.writeLines(deobfMapFile, MAP_FILE_CHARSET, list);
		list.clear();
	}

	private static void dfsPackageName(List<String> list, String prefix, PackageNode node) {
		for (PackageNode pp : node.getInnerPackages()) {
			dfsPackageName(list, prefix + '.' + node.getName(), pp);
		}
		if (node.hasAlias()) {
			list.add(String.format("p %s.%s = %s", prefix, node.getName(), node.getAlias()));
		}
	}

	public String getForCls(ClassInfo cls) {
		return clsPresetMap.get(cls.getFullName());
	}

	public String getForFld(FieldInfo fld) {
		return fldPresetMap.get(fld.getFullId());
	}

	public String getForMth(MethodInfo mth) {
		return mthPresetMap.get(mth.getFullId());
	}

	public void clear() {
		clsPresetMap.clear();
		fldPresetMap.clear();
		mthPresetMap.clear();
	}

	public Map<String, String> getClsPresetMap() {
		return clsPresetMap;
	}

	public Map<String, String> getFldPresetMap() {
		return fldPresetMap;
	}

	public Map<String, String> getMthPresetMap() {
		return mthPresetMap;
	}
}
