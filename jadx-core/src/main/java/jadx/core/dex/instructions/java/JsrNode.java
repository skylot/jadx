package jadx.core.dex.instructions.java;

import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.TargetInsnNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnUtils;

public class JsrNode extends TargetInsnNode {

	protected final int target;

	public JsrNode(int target) {
		this(InsnType.JAVA_JSR, target, 0);
	}

	protected JsrNode(InsnType type, int target, int argsCount) {
		super(type, argsCount);
		this.target = target;
	}

	public int getTarget() {
		return target;
	}

	@Override
	public InsnNode copy() {
		return copyCommonParams(new JsrNode(target));
	}

	@Override
	public String toString() {
		return baseString() + " -> " + InsnUtils.formatOffset(target) + attributesString();
	}
}
