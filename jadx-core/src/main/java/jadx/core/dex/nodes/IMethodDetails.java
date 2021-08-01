package jadx.core.dex.nodes;

import java.util.List;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.utils.Utils;

public interface IMethodDetails extends IJadxAttribute {

	MethodInfo getMethodInfo();

	ArgType getReturnType();

	List<ArgType> getArgTypes();

	List<ArgType> getTypeParameters();

	List<ArgType> getThrows();

	boolean isVarArg();

	int getRawAccessFlags();

	@Override
	default AType<IMethodDetails> getAttrType() {
		return AType.METHOD_DETAILS;
	}

	@Override
	default String toAttrString() {
		StringBuilder sb = new StringBuilder();
		sb.append("MD:");
		if (Utils.notEmpty(getTypeParameters())) {
			sb.append('<');
			sb.append(Utils.listToString(getTypeParameters()));
			sb.append(">:");
		}
		sb.append('(');
		sb.append(Utils.listToString(getArgTypes()));
		sb.append("):");
		sb.append(getReturnType());
		if (isVarArg()) {
			sb.append(" VARARG");
		}
		List<ArgType> throwsList = getThrows();
		if (Utils.notEmpty(throwsList)) {
			sb.append(" throws ").append(Utils.listToString(throwsList));
		}
		return sb.toString();
	}
}
