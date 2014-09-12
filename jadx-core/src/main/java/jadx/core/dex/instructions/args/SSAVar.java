package jadx.core.dex.instructions.args;

import jadx.core.dex.instructions.PhiInsn;

import java.util.ArrayList;
import java.util.List;

public class SSAVar {

	private final int regNum;
	private final int version;
	private VarName varName;

	private int startUseAddr;
	private int endUseAddr;

	private RegisterArg assign;
	private final List<RegisterArg> useList = new ArrayList<RegisterArg>(2);
	private PhiInsn usedInPhi;

	private ArgType type;

	public SSAVar(int regNum, int v, RegisterArg assign) {
		this.regNum = regNum;
		this.version = v;
		this.assign = assign;

		if (assign != null) {
			assign.setSVar(this);
		}

		startUseAddr = -1;
		endUseAddr = -1;
	}

	public int getRegNum() {
		return regNum;
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

		if (assign != null) {
			if (assign.getParentInsn() != null) {
				int insnAddr = assign.getParentInsn().getOffset();

				if (insnAddr >= 0) {
					start = Math.min(insnAddr, start);
					end = Math.max(insnAddr, end);
				}
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

		if ((start != Integer.MAX_VALUE) 
				&& (end != Integer.MIN_VALUE)) {
			startUseAddr = start;
			endUseAddr = end;
		}
	} 

	public int getVersion() {
		return version;
	}

	public RegisterArg getAssign() {
		return assign;
	}

	public void setAssign(RegisterArg assign) {
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

	public void setUsedInPhi(PhiInsn usedInPhi) {
		this.usedInPhi = usedInPhi;
	}

	public PhiInsn getUsedInPhi() {
		return usedInPhi;
	}

	public boolean isUsedInPhi() {
		return usedInPhi != null;
	}

	public int getVariableUseCount() {
		if (!isUsedInPhi()) {
			return useList.size();
		}
		return useList.size() + usedInPhi.getResult().getSVar().getUseCount();
	}

	public ArgType getType() {
		return type;
	}

	public void setType(ArgType type) {
		this.type = type;
		if (assign != null) {
			assign.type = type;
		}
		for (int i = 0, useListSize = useList.size(); i < useListSize; i++) {
			useList.get(i).type = type;
		}
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
