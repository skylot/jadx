package jadx.dex.instructions.mods;

import jadx.dex.info.ClassInfo;
import jadx.dex.info.MethodInfo;
import jadx.dex.instructions.InsnType;
import jadx.dex.instructions.InvokeNode;
import jadx.dex.instructions.args.RegisterArg;
import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;

public class ConstructorInsn extends InsnNode {

	private final MethodInfo callMth;

	private static enum CallType {
		CONSTRUCTOR, // just new instance
		SUPER, // super call
		THIS, // call constructor from other constructor
		SELF // call itself
	}

	private final CallType callType;

	public ConstructorInsn(MethodNode mth, InvokeNode invoke) {
		super(InsnType.CONSTRUCTOR, invoke.getArgsCount() - 1);
		this.callMth = invoke.getCallMth();
		ClassInfo classType = callMth.getDeclClass();

		if (invoke.getArg(0).isThis()) {
			if (classType.equals(mth.getParentClass().getClassInfo())) {
				// self constructor
				if (callMth.getShortId().equals(mth.getMethodInfo().getShortId())) {
					callType = CallType.SELF;
				} else {
					callType = CallType.THIS;
				}
			} else {
				callType = CallType.SUPER;
			}
		} else {
			callType = CallType.CONSTRUCTOR;
			setResult((RegisterArg) invoke.getArg(0));
		}

		for (int i = 1; i < invoke.getArgsCount(); i++) {
			addArg(invoke.getArg(i));
		}
		offset = invoke.getOffset();
	}

	public MethodInfo getCallMth() {
		return callMth;
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
	public String toString() {
		return super.toString() + " " + callMth + " " + callType;
	}
}
