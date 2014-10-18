package jadx.core.dex.instructions.args;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.ConstClassNode;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.parser.FieldValueAttr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterArg extends InsnArg implements Named {
	private static final Logger LOG = LoggerFactory.getLogger(RegisterArg.class);

	protected final int regNum;
	protected SSAVar sVar;

	public RegisterArg(int rn) {
		this.regNum = rn;
	}

	public RegisterArg(int rn, ArgType type) {
		this.type = type;
		this.regNum = rn;
	}

	public int getRegNum() {
		return regNum;
	}

	@Override
	public boolean isRegister() {
		return true;
	}

	public SSAVar getSVar() {
		return sVar;
	}

	void setSVar(SSAVar sVar) {
		this.sVar = sVar;
	}

	public String getName() {
		if (sVar == null) {
			return null;
		}
		return sVar.getName();
	}

	public void setName(String name) {
		if (sVar != null) {
			sVar.setName(name);
		}
	}

	public boolean isNameEquals(InsnArg arg) {
		String n = getName();
		if (n == null || !(arg instanceof Named)) {
			return false;
		}
		return n.equals(((Named) arg).getName());
	}

	@Override
	public void setType(ArgType type) {
		if (sVar != null) {
			sVar.setType(type);
		}
	}

	public void mergeDebugInfo(ArgType type, String name) {
		setType(type);
		setName(name);
	}

	@Deprecated
	public void forceType(ArgType type) {
		this.type = type;
	}

	/**
	 * Return constant value from register assign or null if not constant
	 *
	 * @return LiteralArg, String or ArgType
	 */
	public Object getConstValue(DexNode dex) {
		InsnNode parInsn = getAssignInsn();
		if (parInsn == null) {
			return null;
		}
		InsnType insnType = parInsn.getType();
		switch (insnType) {
			case CONST:
				return parInsn.getArg(0);
			case CONST_STR:
				return ((ConstStringNode) parInsn).getString();
			case CONST_CLASS:
				return ((ConstClassNode) parInsn).getClsType();
			case SGET:
				FieldInfo f = (FieldInfo) ((IndexInsnNode) parInsn).getIndex();
				FieldNode fieldNode = dex.resolveField(f);
				if (fieldNode != null) {
					FieldValueAttr attr = fieldNode.get(AType.FIELD_VALUE);
					if (attr != null) {
						return attr.getValue();
					}
				} else {
					LOG.warn("Field {} not found in dex {}", f, dex);
				}
				break;
		}
		return null;
	}

	@Override
	public boolean isThis() {
		if ("this".equals(getName())) {
			return true;
		}
		// maybe it was moved from 'this' register
		InsnNode ai = getAssignInsn();
		if (ai != null && ai.getType() == InsnType.MOVE) {
			InsnArg arg = ai.getArg(0);
			if (arg != this) {
				return arg.isThis();
			}
		}
		return false;
	}

	public InsnNode getAssignInsn() {
		if (sVar == null) {
			return null;
		}
		RegisterArg assign = sVar.getAssign();
		if (assign != null) {
			return assign.getParentInsn();
		}
		return null;
	}

	public InsnNode getPhiAssignInsn() {
		PhiInsn usePhi = sVar.getUsedInPhi();
		if (usePhi != null) {
			return usePhi;
		}
		RegisterArg assign = sVar.getAssign();
		if (assign != null) {
			InsnNode parent = assign.getParentInsn();
			if (parent != null && parent.getType() == InsnType.PHI) {
				return parent;
			}
		}
		return null;
	}

	public boolean equalRegisterAndType(RegisterArg arg) {
		return regNum == arg.regNum && type.equals(arg.type);
	}

	@Override
	public int hashCode() {
		return (regNum * 31 + type.hashCode()) * 31 + (sVar != null ? sVar.hashCode() : 0);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof RegisterArg)) {
			return false;
		}
		RegisterArg other = (RegisterArg) obj;
		if (regNum != other.regNum) {
			return false;
		}
		if (!type.equals(other.type)) {
			return false;
		}
		if (sVar != null && !sVar.equals(other.getSVar())) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(r");
		sb.append(regNum);
		if (sVar != null) {
			sb.append("_").append(sVar.getVersion());
		}
		if (getName() != null) {
			sb.append(" '").append(getName()).append("'");
		}
		sb.append(" ");
		sb.append(type);
		sb.append(")");
		return sb.toString();
	}
}
