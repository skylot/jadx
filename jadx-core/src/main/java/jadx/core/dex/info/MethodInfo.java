package jadx.core.dex.info;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.android.dex.MethodId;
import com.android.dex.ProtoId;

import jadx.core.codegen.TypeGen;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.Utils;

public final class MethodInfo implements Comparable<MethodInfo> {

	private final String name;
	private final ArgType retType;
	private final List<ArgType> argTypes;
	private final ClassInfo declClass;
	private final String shortId;
	private String alias;
	private boolean aliasFromPreset;

	private MethodInfo(ClassInfo declClass, String name, List<ArgType> args, ArgType retType) {
		this.name = name;
		this.alias = name;
		this.aliasFromPreset = false;
		this.declClass = declClass;
		this.argTypes = args;
		this.retType = retType;
		this.shortId = makeShortId(name, argTypes, retType);
	}

	public static MethodInfo fromDex(DexNode dex, int mthIndex) {
		MethodInfo storageMth = dex.root().getInfoStorage().getMethod(dex, mthIndex);
		if (storageMth != null) {
			return storageMth;
		}
		MethodId mthId = dex.getMethodId(mthIndex);
		String mthName = dex.getString(mthId.getNameIndex());
		ClassInfo parentClass = ClassInfo.fromDex(dex, mthId.getDeclaringClassIndex());

		ProtoId proto = dex.getProtoId(mthId.getProtoIndex());
		ArgType returnType = dex.getType(proto.getReturnTypeIndex());
		List<ArgType> args = dex.readParamList(proto.getParametersOffset());
		MethodInfo newMth = new MethodInfo(parentClass, mthName, args, returnType);
		return dex.root().getInfoStorage().putMethod(dex, mthIndex, newMth);
	}

	public static MethodInfo fromDetails(RootNode rootNode, ClassInfo declClass, String name, List<ArgType> args, ArgType retType) {
		MethodInfo newMth = new MethodInfo(declClass, name, args, retType);
		return rootNode.getInfoStorage().putMethod(newMth);
	}

	public String makeSignature(boolean includeRetType) {
		return makeSignature(false, includeRetType);
	}

	public String makeSignature(boolean useAlias, boolean includeRetType) {
		return makeShortId(useAlias ? alias : name,
				argTypes,
				includeRetType ? retType : null);
	}

	public static String makeShortId(String name, List<ArgType> argTypes, @Nullable ArgType retType) {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append('(');
		for (ArgType arg : argTypes) {
			sb.append(TypeGen.signature(arg));
		}
		sb.append(')');
		if (retType != null) {
			sb.append(TypeGen.signature(retType));
		}
		return sb.toString();
	}

	public boolean isOverloadedBy(MethodInfo otherMthInfo) {
		return argTypes.size() == otherMthInfo.argTypes.size()
				&& name.equals(otherMthInfo.name)
				&& !Objects.equals(this.shortId, otherMthInfo.shortId);
	}

	public String getName() {
		return name;
	}

	public String getFullName() {
		return declClass.getFullName() + '.' + name;
	}

	public String getFullId() {
		return declClass.getFullName() + '.' + shortId;
	}

	public String getRawFullId() {
		return declClass.makeRawFullName() + '.' + shortId;
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
		return argTypes;
	}

	public int getArgsCount() {
		return argTypes.size();
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

	public boolean hasAlias() {
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
		return shortId.hashCode() + 31 * declClass.hashCode();
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
				&& declClass.equals(other.declClass);
	}

	@Override
	public int compareTo(MethodInfo other) {
		int clsCmp = declClass.compareTo(other.declClass);
		if (clsCmp != 0) {
			return clsCmp;
		}
		return shortId.compareTo(other.shortId);
	}

	@Override
	public String toString() {
		return declClass.getFullName() + '.' + name
				+ '(' + Utils.listToString(argTypes) + "):" + retType;
	}
}
