package jadx.api.plugins.input.data.impl;

import jadx.api.plugins.input.data.IFieldRef;

public class JadxFieldRef implements IFieldRef {
	private String parentClassType;
	private String name;
	private String type;

	public JadxFieldRef() {
	}

	public JadxFieldRef(String parentClassType, String name, String type) {
		this.parentClassType = parentClassType;
		this.name = name;
		this.type = type;
	}

	@Override
	public String getParentClassType() {
		return parentClassType;
	}

	public void setParentClassType(String parentClassType) {
		this.parentClassType = parentClassType;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return parentClassType + "->" + name + ":" + type;
	}
}
