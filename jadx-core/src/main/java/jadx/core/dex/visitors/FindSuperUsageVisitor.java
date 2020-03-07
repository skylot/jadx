package jadx.core.dex.visitors;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.blocksmaker.BlockSplitter;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "FindSuperUsageVisitor",
		desc = "Finds variables where a member of the super class is used and marks them.",
		runBefore = BlockSplitter.class
)
public class FindSuperUsageVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		process(mth);
	}

	private static void process(MethodNode methodNode) {
		ArgType superClass = methodNode.getParentClass().getSuperClass();
		if (superClass == null) {
			return;
		}
		String superClassName = superClass.getObject();
		if (superClassName.equals("java.lang.Object")) {
			return;
		}
		for (InsnNode instruction : methodNode.getInstructions()) {
			if (instruction != null) {
				for (InsnArg argument : instruction.getArguments()) {
					if (argument.isRegister()) {
						ArgType argumentType = ((RegisterArg) argument).getInitType();
						if (argumentType.isObject() && argumentType.getObject().equals(superClassName)) {
							argument.add(AFlag.SUPER);
						}
					}
				}
			}
		}
	}
}
