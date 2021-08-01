package jadx.plugins.input.java.data;

import java.util.List;

import jadx.api.plugins.input.data.IMethodProto;
import jadx.api.plugins.utils.Utils;

public class JavaMethodProto implements IMethodProto {

	private String returnType;
	private List<String> argTypes;

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
	public String toString() {
		return "(" + Utils.listToStr(argTypes) + ")" + returnType;
	}
}
