package jadx.core.xmlgen.entry;

import java.util.List;

public final class ResourceEntry {

	private final int id;
	private final String pkgName;
	private final String typeName;
	private final String keyName;

	private int parentRef;
	private RawValue simpleValue;
	private List<RawNamedValue> namedValues;
	private EntryConfig config;

	public ResourceEntry(int id, String pkgName, String typeName, String keyName) {
		this.id = id;
		this.pkgName = pkgName;
		this.typeName = typeName;
		this.keyName = keyName;
	}

	public ResourceEntry(int id) {
		this(id, "", "", "");
	}

	public int getId() {
		return id;
	}

	public String getPkgName() {
		return pkgName;
	}

	public String getTypeName() {
		return typeName;
	}

	public String getKeyName() {
		return keyName;
	}

	public void setParentRef(int parentRef) {
		this.parentRef = parentRef;
	}

	public int getParentRef() {
		return parentRef;
	}

	public RawValue getSimpleValue() {
		return simpleValue;
	}

	public void setSimpleValue(RawValue simpleValue) {
		this.simpleValue = simpleValue;
	}

	public void setNamedValues(List<RawNamedValue> namedValues) {
		this.namedValues = namedValues;
	}

	public List<RawNamedValue> getNamedValues() {
		return namedValues;
	}

	public void setConfig(EntryConfig config) {
		this.config = config;
	}

	public EntryConfig getConfig() {
		return config;
	}

	@Override
	public String toString() {
		return "  0x" + Integer.toHexString(id) + " (" + id + ")" + config + " = " + typeName + "." + keyName;
	}
}
