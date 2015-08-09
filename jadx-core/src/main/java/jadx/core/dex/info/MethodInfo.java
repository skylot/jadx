package jadx.core.dex.info;

import jadx.core.codegen.TypeGen;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.DexNode;
import jadx.core.utils.Utils;

import java.util.List;

import com.android.dex.MethodId;
import com.android.dex.ProtoId;

public final class MethodInfo {

	private final String name;
	private final ArgType retType;
	private final List<ArgType> args;
	private final ClassInfo declClass;
	private final String shortId;
	private String alias;
	private boolean aliasFromPreset;

	private MethodInfo(DexNode dex, int mthIndex) {
		MethodId mthId = dex.getMethodId(mthIndex);
		name = dex.getString(mthId.getNameIndex());
		alias = name;
		aliasFromPreset = false;
		declClass = ClassInfo.fromDex(dex, mthId.getDeclaringClassIndex());

		ProtoId proto = dex.getProtoId(mthId.getProtoIndex());
		retType = dex.getType(proto.getReturnTypeIndex());
		args = dex.readParamList(proto.getParametersOffset());
		shortId = makeSignature(true);
	}

	public static MethodInfo fromDex(DexNode dex, int mthIndex) {
		MethodInfo mth = dex.getInfoStorage().getMethod(mthIndex);
		if (mth != null) {
			return mth;
		}
		mth = new MethodInfo(dex, mthIndex);
		return dex.getInfoStorage().putMethod(mthIndex, mth);
	}

	public String makeSignature(boolean includeRetType) {
		StringBuilder signature = new StringBuilder();
		signature.append(name);
		signature.append('(');
		for (ArgType arg : args) {
			signature.append(TypeGen.signature(arg));
		}
		signature.append(')');
		if (includeRetType) {
			signature.append(TypeGen.signature(retType));
		}
		return signature.toString();
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

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public boolean isRenamed() {
		return !name.equals(alias);
	}

	public boolean isAliasFromPreset() {
		return aliasFromPreset;
	}

	public void setAliasFromPreset(boolean value) {
		aliasFromPreset = value;
	}

	@Override
	public int hashCode() {
		int result = declClass.hashCode();
		result = 31 * result + retType.hashCode();
		result = 31 * result + shortId.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof MethodInfo)) {
			return false;
		}
		MethodInfo other = (MethodInfo) obj;
		return shortId.equals(other.shortId)
				&& retType.equals(other.retType)
				&& declClass.equals(other.declClass);
	}

	@Override
	public String toString() {
		return declClass.getFullName() + "." + name
				+ "(" + Utils.listToString(args) + "):" + retType;
	}

}
