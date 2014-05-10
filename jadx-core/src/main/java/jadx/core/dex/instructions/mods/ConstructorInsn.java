package jadx.core.dex.instructions.mods;

import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;

public class ConstructorInsn extends InsnNode {

	private final MethodInfo callMth;
	private final CallType callType;
	private final RegisterArg instanceArg;

	private static enum CallType {
		CONSTRUCTOR, // just new instance
		SUPER, // super call
		THIS, // call constructor from other constructor
		SELF // call itself
	}

	public ConstructorInsn(MethodNode mth, InvokeNode invoke) {
		super(InsnType.CONSTRUCTOR, invoke.getArgsCount() - 1);
		this.callMth = invoke.getCallMth();
		ClassInfo classType = callMth.getDeclClass();
		instanceArg = (RegisterArg) invoke.getArg(0);

		if (instanceArg.isThis()) {
			if (classType.equals(mth.getParentClass().getClassInfo())) {
				if (callMth.getShortId().equals(mth.getMethodInfo().getShortId())) {
					// self constructor
					callType = CallType.SELF;
				} else {
					callType = CallType.THIS;
				}
			} else {
				callType = CallType.SUPER;
			}
		} else {
			callType = CallType.CONSTRUCTOR;
			setResult(instanceArg);
			// convert from 'use' to 'assign'
			instanceArg.getSVar().setAssign(instanceArg);
		}
		instanceArg.getSVar().removeUse(instanceArg);
		for (int i = 1; i < invoke.getArgsCount(); i++) {
			addArg(invoke.getArg(i));
		}
		offset = invoke.getOffset();
	}

	public MethodInfo getCallMth() {
		return callMth;
	}

	public RegisterArg getInstanceArg() {
		return instanceArg;
	}

	public ClassInfo getClassType() {
		return callMth.getDeclClass();
	}

	public boolean isSuper() {
		return callType == CallType.SUPER;
	}

	public boolean isThis() {
		return callType == CallType.THIS;
	}

	public boolean isSelf() {
		return callType == CallType.SELF;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ConstructorInsn) || !super.equals(o)) {
			return false;
		}
		ConstructorInsn that = (ConstructorInsn) o;
		return callMth.equals(that.callMth)
				&& callType == that.callType
				&& instanceArg.equals(that.instanceArg);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + callMth.hashCode();
		result = 31 * result + callType.hashCode();
		result = 31 * result + instanceArg.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return super.toString() + " " + callMth + " " + callType;
	}
}
