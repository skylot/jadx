package jadx.core.deobf;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;

import static java.nio.charset.StandardCharsets.UTF_8;

class DeobfPresets {
	private static final Logger LOG = LoggerFactory.getLogger(DeobfPresets.class);

	private static final Charset MAP_FILE_CHARSET = UTF_8;

	private final Deobfuscator deobfuscator;
	private final Path deobfMapFile;

	private final Map<String, String> clsPresetMap = new HashMap<>();
	private final Map<String, String> fldPresetMap = new HashMap<>();
	private final Map<String, String> mthPresetMap = new HashMap<>();

	public DeobfPresets(Deobfuscator deobfuscator, Path deobfMapFile) {
		this.deobfuscator = deobfuscator;
		this.deobfMapFile = deobfMapFile;
	}

	/**
	 * Loads deobfuscator presets
	 */
	public void load() {
		if (!Files.exists(deobfMapFile)) {
			return;
		}
		LOG.info("Loading obfuscation map from: {}", deobfMapFile.toAbsolutePath());
		try {
			List<String> lines = Files.readAllLines(deobfMapFile, MAP_FILE_CHARSET);
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
			LOG.error("Failed to load deobfuscation map file '{}'", deobfMapFile.toAbsolutePath(), e);
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
			if (Files.exists(deobfMapFile)) {
				if (forceSave) {
					dumpMapping();
				} else {
					LOG.warn("Deobfuscation map file '{}' exists. Use command line option '--deobf-rewrite-cfg' to rewrite it",
							deobfMapFile.toAbsolutePath());
				}
			} else {
				dumpMapping();
			}
		} catch (IOException e) {
			LOG.error("Failed to load deobfuscation map file '{}'", deobfMapFile.toAbsolutePath(), e);
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
						deobfClsInfo.getCls().getClassInfo().makeRawFullName(), deobfClsInfo.getAlias()));
			}
		}
		for (FieldInfo fld : deobfuscator.getFldMap().keySet()) {
			list.add(String.format("f %s = %s", fld.getRawFullId(), fld.getAlias()));
		}
		for (MethodInfo mth : deobfuscator.getMthMap().keySet()) {
			list.add(String.format("m %s = %s", mth.getRawFullId(), mth.getAlias()));
		}
		Collections.sort(list);
		Files.write(deobfMapFile, list, MAP_FILE_CHARSET);
		if (LOG.isDebugEnabled()) {
			LOG.debug("Deobfuscation map file saved as: {}", deobfMapFile);
		}
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
		return clsPresetMap.get(cls.makeRawFullName());
	}

	public String getForFld(FieldInfo fld) {
		return fldPresetMap.get(fld.getRawFullId());
	}

	public String getForMth(MethodInfo mth) {
		return mthPresetMap.get(mth.getRawFullId());
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
