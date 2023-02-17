package jadx.core.dex.instructions.args;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.Consts;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.RegDebugInfoAttr;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.typeinference.TypeInfo;
import jadx.core.utils.StringUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class SSAVar {
	private static final Logger LOG = LoggerFactory.getLogger(SSAVar.class);

	private final int regNum;
	private final int version;

	private RegisterArg assign;
	private final List<RegisterArg> useList = new ArrayList<>(2);
	private List<PhiInsn> usedInPhi = null;

	private final TypeInfo typeInfo = new TypeInfo();

	@Nullable("Set in InitCodeVariables pass")
	private CodeVar codeVar;

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

	@Nullable
	public InsnNode getAssignInsn() {
		return assign.getParentInsn();
	}

	public void setAssign(@NotNull RegisterArg assign) {
		RegisterArg oldAssign = this.assign;
		if (oldAssign == null) {
			this.assign = assign;
		} else if (oldAssign != assign) {
			oldAssign.resetSSAVar();
			this.assign = assign;
		}
	}

	public List<RegisterArg> getUseList() {
		return useList;
	}

	public int getUseCount() {
		return useList.size();
	}

	@Nullable
	public ArgType getImmutableType() {
		if (isTypeImmutable()) {
			return assign.getInitType();
		}
		return null;
	}

	public boolean isTypeImmutable() {
		return assign.contains(AFlag.IMMUTABLE_TYPE);
	}

	public void markAsImmutable(ArgType type) {
		assign.add(AFlag.IMMUTABLE_TYPE);
		ArgType initType = assign.getInitType();
		if (!initType.equals(type)) {
			assign.forceSetInitType(type);
			if (Consts.DEBUG_TYPE_INFERENCE) {
				LOG.debug("Update immutable type at var {} assign with type: {} previous type: {}", this.toShortString(), type, initType);
			}
		}
	}

	public void setType(ArgType type) {
		ArgType imType = getImmutableType();
		if (imType != null && !imType.equals(type)) {
			throw new JadxRuntimeException("Can't change immutable type " + imType + " to " + type + " for " + this);
		}
		updateType(type);
	}

	public void forceSetType(ArgType type) {
		updateType(type);
	}

	private void updateType(ArgType type) {
		typeInfo.setType(type);
		if (codeVar != null) {
			codeVar.setType(type);
		}
	}

	public void use(RegisterArg arg) {
		if (arg.getSVar() != null) {
			arg.getSVar().removeUse(arg);
		}
		arg.setSVar(this);
		useList.add(arg);
	}

	public void removeUse(RegisterArg arg) {
		useList.removeIf(registerArg -> registerArg == arg);
	}

	public void addUsedInPhi(PhiInsn phiInsn) {
		if (usedInPhi == null) {
			usedInPhi = new ArrayList<>(1);
		}
		usedInPhi.add(phiInsn);
	}

	public void removeUsedInPhi(PhiInsn phiInsn) {
		if (usedInPhi != null) {
			usedInPhi.removeIf(insn -> insn == phiInsn);
			if (usedInPhi.isEmpty()) {
				usedInPhi = null;
			}
		}
	}

	public void updateUsedInPhiList() {
		this.usedInPhi = null;
		for (RegisterArg reg : useList) {
			InsnNode parentInsn = reg.getParentInsn();
			if (parentInsn != null && parentInsn.getType() == InsnType.PHI) {
				addUsedInPhi((PhiInsn) parentInsn);
			}
		}
	}

	@Nullable
	public PhiInsn getOnlyOneUseInPhi() {
		if (usedInPhi != null && usedInPhi.size() == 1) {
			return usedInPhi.get(0);
		}
		return null;
	}

	public List<PhiInsn> getUsedInPhi() {
		if (usedInPhi == null) {
			return Collections.emptyList();
		}
		return usedInPhi;
	}

	/**
	 * Concat assign PHI insn and usedInPhi
	 */
	public List<PhiInsn> getPhiList() {
		InsnNode assignInsn = getAssign().getParentInsn();
		if (assignInsn != null && assignInsn.getType() == InsnType.PHI) {
			PhiInsn assignPhi = (PhiInsn) assignInsn;
			if (usedInPhi == null) {
				return Collections.singletonList(assignPhi);
			}
			List<PhiInsn> list = new ArrayList<>(1 + usedInPhi.size());
			list.add(assignPhi);
			list.addAll(usedInPhi);
			return list;
		}
		if (usedInPhi == null) {
			return Collections.emptyList();
		}
		return usedInPhi;
	}

	public boolean isAssignInPhi() {
		InsnNode assignInsn = getAssignInsn();
		return assignInsn != null && assignInsn.getType() == InsnType.PHI;
	}

	public boolean isUsedInPhi() {
		return usedInPhi != null && !usedInPhi.isEmpty();
	}

	public void setName(String name) {
		if (name != null) {
			if (codeVar == null) {
				throw new JadxRuntimeException("CodeVar not initialized for name set in SSAVar: " + this);
			}
			codeVar.setName(name);
		}
	}

	public String getName() {
		if (codeVar == null) {
			return null;
		}
		return codeVar.getName();
	}

	public TypeInfo getTypeInfo() {
		return typeInfo;
	}

	@NotNull
	public CodeVar getCodeVar() {
		if (codeVar == null) {
			throw new JadxRuntimeException("Code variable not set in " + this);
		}
		return codeVar;
	}

	public void setCodeVar(@NotNull CodeVar codeVar) {
		this.codeVar = codeVar;
		codeVar.addSsaVar(this);
		ArgType imType = getImmutableType();
		if (imType != null) {
			codeVar.setType(imType);
		}
	}

	public void resetTypeAndCodeVar() {
		if (!isTypeImmutable()) {
			updateType(ArgType.UNKNOWN);
		}
		this.typeInfo.getBounds().clear();
		this.codeVar = null;
	}

	public boolean isCodeVarSet() {
		return codeVar != null;
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

	public String toShortString() {
		return "r" + regNum + 'v' + version;
	}

	@Override
	public String toString() {
		return toShortString()
				+ (StringUtils.notEmpty(getName()) ? " '" + getName() + "' " : "")
				+ ' ' + typeInfo.getType();
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
