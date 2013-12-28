package jadx.core.dex.info;

import jadx.core.codegen.TypeGen;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.DexNode;
import jadx.core.utils.Utils;

import java.util.List;

import com.android.dx.io.MethodId;
import com.android.dx.io.ProtoId;

public final class MethodInfo {

	private final String name;
	private final ArgType retType;
	private final List<ArgType> args;
	private final ClassInfo declClass;
	private final String shortId;

	public static MethodInfo fromDex(DexNode dex, int mthIndex) {
		return new MethodInfo(dex, mthIndex);
	}

	private MethodInfo(DexNode dex, int mthIndex) {
		MethodId mthId = dex.getMethodId(mthIndex);
		name = dex.getString(mthId.getNameIndex());
		declClass = ClassInfo.fromDex(dex, mthId.getDeclaringClassIndex());

		ProtoId proto = dex.getProtoId(mthId.getProtoIndex());
		retType = dex.getType(proto.getReturnTypeIndex());
		args = dex.readParamList(proto.getParametersOffset());

		StringBuilder signature = new StringBuilder();
		signature.append(name);
		signature.append('(');
		for (ArgType arg : args) {
			signature.append(TypeGen.signature(arg));
		}
		signature.append(')');
		signature.append(TypeGen.signature(retType));

		shortId = signature.toString();
	}

	public String getName() {
		return name;
	}

	public String getFullName() {
		return declClass.getFullName() + "." + name;
	}

	public String getFullId() {
		return declClass.getFullName() + "." + shortId;
	}

	/**
	 * Method name and signature
	 */
	public String getShortId() {
		return shortId;
	}

	public ClassInfo getDeclClass() {
		return declClass;
	}

	public ArgType getReturnType() {
		return retType;
	}

	public List<ArgType> getArgumentsTypes() {
		return args;
	}

	public int getArgsCount() {
		return args.size();
	}

	public boolean isConstructor() {
		return name.equals("<init>");
	}

	public boolean isClassInit() {
		return name.equals("<clinit>");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + declClass.hashCode();
		result = prime * result + retType.hashCode();
		result = prime * result + shortId.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		MethodInfo other = (MethodInfo) obj;
		if (!shortId.equals(other.shortId)) return false;
		if (!retType.equals(other.retType)) return false;
		if (!declClass.equals(other.declClass)) return false;
		return true;
	}

	@Override
	public String toString() {
		return declClass.getFullName() + "." + name
				+ "(" + Utils.listToString(args) + "):" + retType;
	}

}
