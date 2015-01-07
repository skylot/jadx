package jadx.core.xmlgen;

import jadx.core.utils.Utils;
import jadx.core.xmlgen.entry.ResourceEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceStorage {

	private static final Comparator<ResourceEntry> COMPARATOR = new Comparator<ResourceEntry>() {
		@Override
		public int compare(ResourceEntry a, ResourceEntry b) {
			return Utils.compare(a.getId(), b.getId());
		}
	};

	private final List<ResourceEntry> list = new ArrayList<ResourceEntry>();
	private String appPackage;

	public Collection<ResourceEntry> getResources() {
		return list;
	}

	public void add(ResourceEntry ri) {
		list.add(ri);
	}

	public void finish() {
		Collections.sort(list, COMPARATOR);
	}

	public ResourceEntry getByRef(int refId) {
		ResourceEntry key = new ResourceEntry(refId);
		int index = Collections.binarySearch(list, key, COMPARATOR);
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
		Map<Integer, String> map = new HashMap<Integer, String>();
		for (ResourceEntry entry : list) {
			map.put(entry.getId(), entry.getTypeName() + "/" + entry.getKeyName());
		}
		return map;
	}
}
