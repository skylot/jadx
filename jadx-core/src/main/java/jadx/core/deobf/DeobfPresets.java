package jadx.core.deobf;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.nodes.VariableNode;
import jadx.core.utils.files.FileUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DeobfPresets {
	private static final Logger LOG = LoggerFactory.getLogger(DeobfPresets.class);

	private static final Charset MAP_FILE_CHARSET = UTF_8;

	private final Path deobfMapFile;

	private final Map<String, String> pkgPresetMap = new HashMap<>();
	private final Map<String, String> clsPresetMap = new HashMap<>();
	private final Map<String, String> fldPresetMap = new HashMap<>();
	private final Map<String, String> mthPresetMap = new HashMap<>();
	private final Map<String, Set<String>> varPresetMap = new HashMap<>();

	@Nullable
	public static DeobfPresets build(RootNode root) {
		Path deobfMapPath = getPathDeobfMapPath(root);
		if (deobfMapPath == null) {
			return null;
		}
		LOG.info("Deobfuscation map file set to: {}", deobfMapPath);
		return new DeobfPresets(deobfMapPath);
	}

	@Nullable
	private static Path getPathDeobfMapPath(RootNode root) {
		JadxArgs jadxArgs = root.getArgs();
		File deobfMapFile = jadxArgs.getDeobfuscationMapFile();
		if (deobfMapFile != null) {
			return deobfMapFile.toPath();
		}
		List<File> inputFiles = jadxArgs.getInputFiles();
		if (inputFiles.isEmpty()) {
			return null;
		}
		Path inputFilePath = inputFiles.get(0).getAbsoluteFile().toPath();
		String baseName = FileUtils.getPathBaseName(inputFilePath);
		return inputFilePath.getParent().resolve(baseName + ".jobf");
	}

	private DeobfPresets(Path deobfMapFile) {
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
				switch (l.charAt(0)) {
					case 'p':
						pkgPresetMap.put(origName, alias);
						break;
					case 'c':
						clsPresetMap.put(origName, alias);
						break;
					case 'f':
						fldPresetMap.put(origName, alias);
						break;
					case 'm':
						mthPresetMap.put(origName, alias);
						break;
					case 'v':
						String[] mthIDAndVarIndex = origName.split(VariableNode.VAR_SEPARATOR);
						if (mthIDAndVarIndex.length == 2) {
							Set<String> nameList = varPresetMap.computeIfAbsent(mthIDAndVarIndex[0], k -> new HashSet<>());
							nameList.add(makeVarSecIndex(mthIDAndVarIndex[1], alias));
						}
						break;
				}
			}
		} catch (Exception e) {
			LOG.error("Failed to load deobfuscation map file '{}'", deobfMapFile.toAbsolutePath(), e);
		}
	}

	public static String makeVarSecIndex(String indexes, String name) {
		return indexes + VariableNode.VAR_SEPARATOR + name;
	}

	private static String[] splitAndTrim(String str) {
		String[] v = str.substring(2).split("=");
		for (int i = 0; i < v.length; i++) {
			v[i] = v[i].trim();
		}
		return v;
	}

	public void save() throws IOException {
		List<String> list = new ArrayList<>();
		for (Map.Entry<String, String> pkgEntry : pkgPresetMap.entrySet()) {
			list.add(String.format("p %s = %s", pkgEntry.getKey(), pkgEntry.getValue()));
		}
		for (Map.Entry<String, String> clsEntry : clsPresetMap.entrySet()) {
			list.add(String.format("c %s = %s", clsEntry.getKey(), clsEntry.getValue()));
		}
		for (Map.Entry<String, String> fldEntry : fldPresetMap.entrySet()) {
			list.add(String.format("f %s = %s", fldEntry.getKey(), fldEntry.getValue()));
		}
		for (Map.Entry<String, String> mthEntry : mthPresetMap.entrySet()) {
			list.add(String.format("m %s = %s", mthEntry.getKey(), mthEntry.getValue()));
		}
		for (Map.Entry<String, Set<String>> varEntry : varPresetMap.entrySet()) {
			for (String val : varEntry.getValue()) {
				String[] indexAndName = val.split(VariableNode.VAR_SEPARATOR);
				if (indexAndName.length == 2) {
					list.add(String.format("v %s%s%s = %s",
							varEntry.getKey(), VariableNode.VAR_SEPARATOR, indexAndName[0], indexAndName[1]));
				}
			}
		}
		Collections.sort(list);
		Files.write(deobfMapFile, list, MAP_FILE_CHARSET,
				StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		if (LOG.isDebugEnabled()) {
			LOG.debug("Deobfuscation map file saved as: {}", deobfMapFile);
		}
	}

	public String getForCls(ClassInfo cls) {
		if (clsPresetMap.isEmpty()) {
			return null;
		}
		return clsPresetMap.get(cls.makeRawFullName());
	}

	public String getForFld(FieldInfo fld) {
		if (fldPresetMap.isEmpty()) {
			return null;
		}
		return fldPresetMap.get(fld.getRawFullId());
	}

	public String getForMth(MethodInfo mth) {
		if (mthPresetMap.isEmpty()) {
			return null;
		}
		return mthPresetMap.get(mth.getRawFullId());
	}

	public Set<String> getForVars(MethodInfo mth) {
		if (varPresetMap.isEmpty()) {
			return null;
		}
		return varPresetMap.get(mth.getRawFullId());
	}

	public void clear() {
		clsPresetMap.clear();
		fldPresetMap.clear();
		mthPresetMap.clear();
		varPresetMap.clear();
	}

	public Path getDeobfMapFile() {
		return deobfMapFile;
	}

	public Map<String, String> getPkgPresetMap() {
		return pkgPresetMap;
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

	public Map<String, Set<String>> getVarPresetMap() {
		return varPresetMap;
	}

	public void updateVariableName(VariableNode node, String name) {
		String key = node.getRenameKey();
		key = key.substring(0, key.indexOf(VariableNode.VAR_SEPARATOR));
		String newIndex = makeVarSecIndex(node.makeVarIndex(), name);
		String oldIndex = makeVarSecIndex(node.makeVarIndex(), node.getName());
		Set<String> indexSet = varPresetMap.computeIfAbsent(key, k -> new HashSet<>());
		indexSet.remove(oldIndex);
		indexSet.add(newIndex);
	}
}
