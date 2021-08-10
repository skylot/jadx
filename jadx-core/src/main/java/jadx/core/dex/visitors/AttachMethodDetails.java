package jadx.core.dex.visitors;

import jadx.core.dex.instructions.BaseInvokeNode;
import jadx.core.dex.nodes.IMethodDetails;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.nodes.utils.MethodUtils;
import jadx.core.dex.visitors.blocks.BlockSplitter;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "Attach Method Details",
		desc = "Attach method details for invoke instructions",
		runBefore = {
				BlockSplitter.class,
				MethodInvokeVisitor.class
		}
)
public class AttachMethodDetails extends AbstractVisitor {

	private MethodUtils methodUtils;

	@Override
	public void init(RootNode root) {
		methodUtils = root.getMethodUtils();
	}

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		for (InsnNode insn : mth.getInstructions()) {
			if (insn instanceof BaseInvokeNode) {
				attachMethodDetails((BaseInvokeNode) insn);
			}
		}
	}

	private void attachMethodDetails(BaseInvokeNode insn) {
		IMethodDetails methodDetails = methodUtils.getMethodDetails(insn.getCallMth());
		if (methodDetails != null) {
			insn.addAttr(methodDetails);
		}
	}
}
