package jadx.core.dex.instructions;

import jadx.api.plugins.input.data.IMethodProto;
import jadx.api.plugins.input.insns.InsnData;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnUtils;

public class InvokePolymorphicNode extends InvokeNode {
	private final IMethodProto proto;
	private final MethodInfo baseCallRef;

	public InvokePolymorphicNode(MethodInfo callMth, InsnData insn, IMethodProto proto, MethodInfo baseRef, boolean isRange) {
		super(callMth, insn, InvokeType.POLYMORPHIC, true, isRange);
		this.proto = proto;
		this.baseCallRef = baseRef;
	}

	public InvokePolymorphicNode(MethodInfo callMth, int argsCount, IMethodProto proto, MethodInfo baseRef) {
		super(callMth, InvokeType.POLYMORPHIC, argsCount);
		this.proto = proto;
		this.baseCallRef = baseRef;
	}

	public IMethodProto getProto() {
		return proto;
	}

	public MethodInfo getBaseCallRef() {
		return baseCallRef;
	}

	@Override
	public InsnNode copy() {
		InvokePolymorphicNode copy = new InvokePolymorphicNode(getCallMth(), getArgsCount(), proto, baseCallRef);
		copyCommonParams(copy);
		return copy;
	}

	@Override
	public boolean isSame(InsnNode obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof InvokePolymorphicNode) || !super.isSame(obj)) {
			return false;
		}
		InvokePolymorphicNode other = (InvokePolymorphicNode) obj;
		return proto.equals(other.proto);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(InsnUtils.formatOffset(offset)).append(": INVOKE_POLYMORPHIC ");
		if (getResult() != null) {
			sb.append(getResult()).append(" = ");
		}
		if (!appendArgs(sb)) {
			sb.append('\n');
		}
		appendAttributes(sb);
		sb.append(" base: ").append(baseCallRef).append('\n');
		sb.append(" proto: ").append(proto).append('\n');
		return sb.toString();
	}
}
