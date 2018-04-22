package jadx.core.dex.instructions.args;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AttrNode;
import jadx.core.dex.instructions.PhiInsn;

public class SSAVar extends AttrNode {

	private final int regNum;
	private final int version;
	private VarName varName;

	private int startUseAddr;
	private int endUseAddr;

	@NotNull
	private RegisterArg assign;
	private final List<RegisterArg> useList = new ArrayList<>(2);
	@Nullable
	private PhiInsn usedInPhi;

	private ArgType type;
	private boolean typeImmutable;

	public SSAVar(int regNum, int v, @NotNull RegisterArg assign) {
		this.regNum = regNum;
		this.version = v;
		this.assign = assign;

		assign.setSVar(this);
		startUseAddr = -1;
		endUseAddr = -1;
	}

	public int getStartAddr() {
		if (startUseAddr == -1) {
			calcUsageAddrRange();
		}
		return startUseAddr;
	}

	public int getEndAddr() {
		if (endUseAddr == -1) {
			calcUsageAddrRange();
		}
		return endUseAddr;
	}

	private void calcUsageAddrRange() {
		int start = Integer.MAX_VALUE;
		int end = Integer.MIN_VALUE;

		if (assign.getParentInsn() != null) {
			int insnAddr = assign.getParentInsn().getOffset();
			if (insnAddr >= 0) {
				start = Math.min(insnAddr, start);
				end = Math.max(insnAddr, end);
			}
		}
		for (RegisterArg arg : useList) {
			if (arg.getParentInsn() != null) {
				int insnAddr = arg.getParentInsn().getOffset();
				if (insnAddr >= 0) {
					start = Math.min(insnAddr, start);
					end = Math.max(insnAddr, end);
				}
			}
		}
		if (start != Integer.MAX_VALUE && end != Integer.MIN_VALUE) {
			startUseAddr = start;
			endUseAddr = end;
		}
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

	public void setType(ArgType type) {
		ArgType acceptedType;
		if (typeImmutable) {
			// don't change type, just update types in useList
			acceptedType = this.type;
		} else {
			acceptedType = type;
			this.type = acceptedType;
		}
		assign.type = acceptedType;
		for (int i = 0, useListSize = useList.size(); i < useListSize; i++) {
			useList.get(i).type = acceptedType;
		}
	}

	public void setTypeImmutable(ArgType type) {
		setType(type);
		this.typeImmutable = true;
	}

	public boolean isTypeImmutable() {
		return typeImmutable;
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
		return "r" + regNum + "_" + version;
	}
}
