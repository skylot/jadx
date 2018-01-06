package jadx.core.xmlgen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jadx.core.xmlgen.entry.ResourceEntry;

public class ResourceStorage {

	private final List<ResourceEntry> list = new ArrayList<>();
	private String appPackage;

	public Collection<ResourceEntry> getResources() {
		return list;
	}

	public void add(ResourceEntry ri) {
		list.add(ri);
	}

	public void finish() {
		list.sort(Comparator.comparingInt(ResourceEntry::getId));
	}

	public ResourceEntry getByRef(int refId) {
		ResourceEntry key = new ResourceEntry(refId);
		int index = Collections.binarySearch(list, key, Comparator.comparingInt(ResourceEntry::getId));
		if (index < 0) {
			return null;
		}
		return list.get(index);
	}

	public String getAppPackage() {
		return appPackage;
	}

	public void setAppPackage(String appPackage) {
		this.appPackage = appPackage;
	}

	public Map<Integer, String> getResourcesNames() {
		Map<Integer, String> map = new HashMap<>();
		for (ResourceEntry entry : list) {
			map.put(entry.getId(), entry.getTypeName() + "/" + entry.getKeyName());
		}
		return map;
	}
}
