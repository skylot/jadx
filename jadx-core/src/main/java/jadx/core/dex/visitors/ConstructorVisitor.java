package jadx.core.dex.visitors;

import java.util.ArrayList;

import jadx.core.codegen.TypeGen;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.ssa.SSATransform;
import jadx.core.dex.visitors.typeinference.TypeInferenceVisitor;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InstructionRemover;

@JadxVisitor(
		name = "ConstructorVisitor",
		desc = "Replace invoke with constructor call",
		runAfter = SSATransform.class,
		runBefore = TypeInferenceVisitor.class
)
public class ConstructorVisitor extends AbstractVisitor {
	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}

		replaceInvoke(mth);
	}

	private static void replaceInvoke(MethodNode mth) {
		InstructionRemover remover = new InstructionRemover(mth);
		for (BlockNode block : mth.getBasicBlocks()) {
			remover.setBlock(block);
			int size = block.getInstructions().size();
			for (int i = 0; i < size; i++) {
				InsnNode insn = block.getInstructions().get(i);
				if (insn.getType() == InsnType.INVOKE) {
					processInvoke(mth, block, i, remover);
				}
			}
			remover.perform();
		}
	}

	private static void processInvoke(MethodNode mth, BlockNode block, int indexInBlock, InstructionRemover remover) {
		ClassNode parentClass = mth.getParentClass();
		InsnNode insn = block.getInstructions().get(indexInBlock);
		InvokeNode inv = (InvokeNode) insn;
		MethodInfo callMth = inv.getCallMth();
		if (!callMth.isConstructor()) {
			return;
		}
		InsnNode instArgAssignInsn = ((RegisterArg) inv.getArg(0)).getAssignInsn();
		ConstructorInsn co = new ConstructorInsn(mth, inv);
		boolean remove = false;
		if (co.isSuper() && (co.getArgsCount() == 0 || parentClass.isEnum())) {
			remove = true;
		} else if (co.isThis() && co.getArgsCount() == 0) {
			MethodNode defCo = parentClass.searchMethodByName(callMth.getShortId());
			if (defCo == null || defCo.isNoCode()) {
				// default constructor not implemented
				remove = true;
			}
		}
		// remove super() call in instance initializer
		if (parentClass.isAnonymous() && mth.isDefaultConstructor() && co.isSuper()) {
			remove = true;
		}
		if (remove) {
			remover.add(insn);
			return;
		}
		if (co.isNewInstance()) {
			InsnNode newInstInsn = removeAssignChain(mth, instArgAssignInsn, remover, InsnType.NEW_INSTANCE);
			if (newInstInsn != null) {
				RegisterArg instArg = newInstInsn.getResult();
				RegisterArg resultArg = co.getResult();
				if (!resultArg.equals(instArg)) {
					// replace all usages of 'instArg' with result of this constructor instruction
					for (RegisterArg useArg : new ArrayList<>(instArg.getSVar().getUseList())) {
						RegisterArg dup = resultArg.duplicate();
						InsnNode parentInsn = useArg.getParentInsn();
						parentInsn.replaceArg(useArg, dup);
						dup.setParentInsn(parentInsn);
						resultArg.getSVar().use(dup);
					}
				}
				newInstInsn.setResult(null); // don't unbind result arg on remove
			}
		}
		ConstructorInsn replace = processConstructor(mth, co);
		if (replace != null) {
			co = replace;
		}
		BlockUtils.replaceInsn(block, indexInBlock, co);
	}

	/**
	 * Replace call of synthetic constructor
	 */
	private static ConstructorInsn processConstructor(MethodNode mth, ConstructorInsn co) {
		MethodNode callMth = mth.dex().resolveMethod(co.getCallMth());
		if (callMth == null
				|| !callMth.getAccessFlags().isSynthetic()
				|| !allArgsNull(co)) {
			return null;
		}
		ClassNode classNode = mth.dex().resolveClass(callMth.getParentClass().getClassInfo());
		if (classNode == null) {
			return null;
		}
		RegisterArg instanceArg = co.getInstanceArg();
		boolean passThis = instanceArg.isThis();
		String ctrId = "<init>(" + (passThis ? TypeGen.signature(instanceArg.getInitType()) : "") + ")V";
		MethodNode defCtr = classNode.searchMethodByName(ctrId);
		if (defCtr == null) {
			return null;
		}
		ConstructorInsn newInsn = new ConstructorInsn(defCtr.getMethodInfo(), co.getCallType(), instanceArg);
		newInsn.setResult(co.getResult());
		return newInsn;
	}

	private static boolean allArgsNull(ConstructorInsn insn) {
		for (InsnArg insnArg : insn.getArguments()) {
			if (insnArg.isLiteral()) {
				LiteralArg lit = (LiteralArg) insnArg;
				if (lit.getLiteral() != 0) {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	/**
	 * Remove instructions on 'move' chain until instruction with type 'insnType'
	 */
	private static InsnNode removeAssignChain(MethodNode mth, InsnNode insn, InstructionRemover remover, InsnType insnType) {
		if (insn == null) {
			return null;
		}
		if (insn.isAttrStorageEmpty()) {
			remover.add(insn);
		} else {
			BlockUtils.replaceInsn(mth, insn, new InsnNode(InsnType.NOP, 0));
		}
		InsnType type = insn.getType();
		if (type == insnType) {
			return insn;
		}
		if (type == InsnType.MOVE) {
			RegisterArg arg = (RegisterArg) insn.getArg(0);
			return removeAssignChain(mth, arg.getAssignInsn(), remover, insnType);
		}
		return null;
	}
}
