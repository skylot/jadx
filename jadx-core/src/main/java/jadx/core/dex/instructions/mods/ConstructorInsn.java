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

	private static enum CallType {
		CONSTRUCTOR, // just new instance
		SUPER, // super call
		THIS, // call constructor from other constructor
		SELF // call itself
	}

	private CallType callType;

	public ConstructorInsn(MethodNode mth, InvokeNode invoke) {
		super(InsnType.CONSTRUCTOR, invoke.getArgsCount() - 1);
		this.callMth = invoke.getCallMth();
		ClassInfo classType = callMth.getDeclClass();

		if (invoke.getArg(0).isThis()) {
			if (classType.equals(mth.getParentClass().getClassInfo())) {
				if (callMth.getShortId().equals(mth.getMethodInfo().getShortId())) {
					// self constructor
					callType = CallType.SELF;
				} else if (mth.getMethodInfo().isConstructor()) {
					callType = CallType.THIS;
				}
			} else {
				callType = CallType.SUPER;
			}
		}
		if (callType == null) {
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
