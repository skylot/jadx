package jadx.core.dex.instructions.args;

import jadx.core.dex.instructions.PhiInsn;

import java.util.ArrayList;
import java.util.List;

public class SSAVar {

	private final int regNum;
	private final int version;

	private RegisterArg assign;
	private final List<RegisterArg> useList = new ArrayList<RegisterArg>(2);
	private PhiInsn usedInPhi;

	private ArgType type;

	public SSAVar(int regNum, int v, RegisterArg assign) {
		this.regNum = regNum;
		this.version = v;
		this.assign = assign;

		if (assign != null) {
			mergeName(assign);
			assign.setSVar(this);
		}
	}

	public int getRegNum() {
		return regNum;
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
		mergeName(arg);
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
		if (assign != null) {
			assign.setName(name);
		}
		for (int i = 0, useListSize = useList.size(); i < useListSize; i++) {
			useList.get(i).setName(name);
		}
	}

	public void setVariableName(String name) {
		setName(name);
		if (isUsedInPhi()) {
			PhiInsn phi = getUsedInPhi();
			phi.getResult().getSVar().setVariableName(name);
			for (InsnArg arg : phi.getArguments()) {
				if (arg.isRegister()) {
					RegisterArg reg = (RegisterArg) arg;
					SSAVar sVar = reg.getSVar();
					if (sVar != this && !name.equals(reg.getName())) {
						sVar.setVariableName(name);
					}
				}
			}
		}
	}

	public void mergeName(RegisterArg arg) {
		if (arg.getName() != null) {
			setName(arg.getName());
		}
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
