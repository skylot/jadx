package jadx.gui.cache.usage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;

class RawUsageData {

	private final Map<String, ClsUsageData> clsMap = new HashMap<>();
	private List<String> classesWithoutData = Collections.emptyList();

	public Map<String, ClsUsageData> getClsMap() {
		return clsMap;
	}

	public List<String> getClassesWithoutData() {
		return classesWithoutData;
	}

	public ClsUsageData getClassData(ClassNode cls) {
		return getClassData(cls.getRawName());
	}

	public ClsUsageData getClassData(String clsRawName) {
		return clsMap.computeIfAbsent(clsRawName, ClsUsageData::new);
	}

	public MthUsageData getMethodData(MethodNode mth) {
		ClassNode parentClass = mth.getParentClass();
		String shortId = mth.getMethodInfo().getShortId();
		return getClassData(parentClass).getMthUsage().computeIfAbsent(shortId,
				m -> new MthUsageData(new MthRef(parentClass.getRawName(), shortId)));
	}

	public FldUsageData getFieldData(FieldNode fld) {
		ClassNode parentClass = fld.getParentClass();
		String shortId = fld.getFieldInfo().getShortId();
		return getClassData(parentClass).getFldUsage().computeIfAbsent(shortId,
				m -> new FldUsageData(new FldRef(parentClass.getRawName(), shortId)));
	}

	public void collectClassesWithoutData() {
		Set<String> allClasses = new HashSet<>(clsMap.size() * 2);
		for (ClsUsageData usageData : clsMap.values()) {
			List<String> deps = usageData.getClsDeps();
			if (deps != null) {
				allClasses.addAll(deps);
			}
			List<String> usage = usageData.getClsUsage();
			if (usage != null) {
				allClasses.addAll(usage);
			}
		}
		allClasses.removeAll(clsMap.keySet());
		classesWithoutData = new ArrayList<>(allClasses);
		Collections.sort(classesWithoutData);
	}
}
