package jadx.gui.cache.usage;

import java.util.List;

import jadx.api.plugins.input.data.IMethodRef;

public class CachedMethodRef implements IMethodRef {

	private String parentClassType;
	private String name;
	private String returnType;
	private List<String> argTypes;

	public CachedMethodRef(String parentClassType, String name, String returnType, List<String> argTypes) {
		this.parentClassType = parentClassType;
		this.name = name;
		this.returnType = returnType;
		this.argTypes = argTypes;
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
	public String getReturnType() {
		return returnType;
	}

	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}

	@Override
	public List<String> getArgTypes() {
		return argTypes;
	}

	public void setArgTypes(List<String> argTypes) {
		this.argTypes = argTypes;
	}

	@Override
	public int getUniqId() {
		throw new UnsupportedOperationException("Unimplemented method 'getUniqId'");
	}

	@Override
	public void load() {
		throw new UnsupportedOperationException("Unimplemented method 'load'");
	}

}
