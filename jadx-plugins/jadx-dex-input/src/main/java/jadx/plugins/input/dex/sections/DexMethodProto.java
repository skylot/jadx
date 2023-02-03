package jadx.plugins.input.dex.sections;

import java.util.List;

import jadx.api.plugins.input.data.IMethodProto;
import jadx.api.plugins.utils.Utils;

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
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof IMethodProto)) {
			return false;
		}
		IMethodProto that = (IMethodProto) other;
		return argTypes.equals(that.getArgTypes())
				&& returnType.equals(that.getReturnType());
	}

	@Override
	public int hashCode() {
		return 31 * argTypes.hashCode() + returnType.hashCode();
	}

	@Override
	public String toString() {
		return "(" + Utils.listToStr(argTypes) + ")" + returnType;
	}
}
