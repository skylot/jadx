package jadx.core.dex.instructions.args;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.AttrNode;
import jadx.core.dex.attributes.nodes.RegDebugInfoAttr;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.typeinference.TypeInfo;

public class SSAVar extends AttrNode {
	private final int regNum;
	private final int version;

	@NotNull
	private RegisterArg assign;
	private final List<RegisterArg> useList = new ArrayList<>(2);
	@Nullable
	private PhiInsn usedInPhi;

	private TypeInfo typeInfo = new TypeInfo();
	private VarName varName;

	public SSAVar(int regNum, int v, @NotNull RegisterArg assign) {
		this.regNum = regNum;
		this.version = v;
		this.assign = assign;

		assign.setSVar(this);
	}

	public int getRegNum() {
		return regNum;
	}

	public int getVersion() {
		return version;
	}

	@NotNull
	public RegisterArg getAssign() {
		return assign;
	}

	public void setAssign(@NotNull RegisterArg assign) {
		this.assign = assign;
	}

	public List<RegisterArg> getUseList() {
		return useList;
	}

	public int getUseCount() {
		return useList.size();
	}

	public void use(RegisterArg arg) {
		if (arg.getSVar() != null) {
			arg.getSVar().removeUse(arg);
		}
		arg.setSVar(this);
		useList.add(arg);
	}

	public void removeUse(RegisterArg arg) {
		for (int i = 0, useListSize = useList.size(); i < useListSize; i++) {
			if (useList.get(i) == arg) {
				useList.remove(i);
				break;
			}
		}
	}

	public void setUsedInPhi(@Nullable PhiInsn usedInPhi) {
		this.usedInPhi = usedInPhi;
	}

	@Nullable
	public PhiInsn getUsedInPhi() {
		return usedInPhi;
	}

	public boolean isUsedInPhi() {
		return usedInPhi != null;
	}

	public int getVariableUseCount() {
		if (usedInPhi == null) {
			return useList.size();
		}
		return useList.size() + usedInPhi.getResult().getSVar().getUseCount();
	}

	public void setName(String name) {
		if (name != null) {
			if (varName == null) {
				varName = new VarName();
			}
			varName.setName(name);
		}
	}

	public String getName() {
		if (varName == null) {
			return null;
		}
		return varName.getName();
	}

	public VarName getVarName() {
		return varName;
	}

	public void setVarName(VarName varName) {
		this.varName = varName;
	}

	public TypeInfo getTypeInfo() {
		return typeInfo;
	}

	public void setTypeInfo(TypeInfo typeInfo) {
		this.typeInfo = typeInfo;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SSAVar)) {
			return false;
		}
		SSAVar ssaVar = (SSAVar) o;
		return regNum == ssaVar.regNum && version == ssaVar.version;
	}

	@Override
	public int hashCode() {
		return 31 * regNum + version;
	}

	@Override
	public String toString() {
		return "r" + regNum + ":" + version + " " + typeInfo.getType();
	}

	public String getDetailedVarInfo(MethodNode mth) {
		Set<ArgType> types = new HashSet<>();
		Set<String> names = Collections.emptySet();

		List<RegisterArg> useArgs = new ArrayList<>(1 + useList.size());
		useArgs.add(assign);
		useArgs.addAll(useList);

		if (mth.contains(AType.LOCAL_VARS_DEBUG_INFO)) {
			names = new HashSet<>();
			for (RegisterArg arg : useArgs) {
				RegDebugInfoAttr debugInfoAttr = arg.get(AType.REG_DEBUG_INFO);
				if (debugInfoAttr != null) {
					names.add(debugInfoAttr.getName());
					types.add(debugInfoAttr.getRegType());
				}
			}
		}

		for (RegisterArg arg : useArgs) {
			ArgType initType = arg.getInitType();
			if (initType.isTypeKnown()) {
				types.add(initType);
			}
			ArgType type = arg.getType();
			if (type.isTypeKnown()) {
				types.add(type);
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append('r').append(regNum).append('v').append(version);
		if (!names.isEmpty()) {
			sb.append(", names: ").append(names);
		}
		if (!types.isEmpty()) {
			sb.append(", types: ").append(types);
		}
		return sb.toString();
	}
}
