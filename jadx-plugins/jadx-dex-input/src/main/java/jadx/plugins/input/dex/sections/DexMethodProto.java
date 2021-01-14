package jadx.plugins.input.dex.sections;

import java.util.List;

import jadx.api.plugins.input.data.IMethodProto;
import jadx.plugins.input.dex.utils.Utils;

public class DexMethodProto implements IMethodProto {
	private final List<String> argTypes;
	private final String returnType;

	public DexMethodProto(List<String> argTypes, String returnType) {
		this.returnType = returnType;
		this.argTypes = argTypes;
	}

	@Override
	public List<String> getArgTypes() {
		return argTypes;
	}

	@Override
	public String getReturnType() {
		return returnType;
	}

	@Override
	public String toString() {
		return "(" + Utils.listToStr(argTypes) + ")" + returnType;
	}
}
