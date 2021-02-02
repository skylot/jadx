package jadx.core.dex.visitors;

import java.util.ArrayList;

import org.jetbrains.annotations.Nullable;

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
import jadx.core.utils.InsnRemover;

@JadxVisitor(
		name = "ConstructorVisitor",
		desc = "Replace invoke with constructor call",
		runAfter = { SSATransform.class, MoveInlineVisitor.class },
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
		InsnRemover remover = new InsnRemover(mth);
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

	private static void processInvoke(MethodNode mth, BlockNode block, int indexInBlock, InsnRemover remover) {
		ClassNode parentClass = mth.getParentClass();
		InsnNode insn = block.getInstructions().get(indexInBlock);
		InvokeNode inv = (InvokeNode) insn;
		MethodInfo callMth = inv.getCallMth();
		if (!callMth.isConstructor()) {
			return;
		}
		RegisterArg instanceArg = ((RegisterArg) inv.getArg(0));
		InsnNode instArgAssignInsn = instanceArg.getAssignInsn();
		ConstructorInsn co = new ConstructorInsn(mth, inv);
		if (co.isNewInstance()) {
			co.setResult(instanceArg);
			// convert from 'use' to 'assign'
			instanceArg.getSVar().setAssign(instanceArg);
		}
		instanceArg.getSVar().removeUse(instanceArg);

		co.rebindArgs();
		boolean remove = false;
		if (co.isSuper() && (co.getArgsCount() == 0 || parentClass.isEnum())) {
			remove = true;
		} else if (co.isThis() && co.getArgsCount() == 0) {
			MethodNode defCo = parentClass.searchMethodByShortId(callMth.getShortId());
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
			remover.addAndUnbind(insn);
			return;
		}
		if (co.isNewInstance()) {
			InsnNode newInstInsn = removeAssignChain(mth, instArgAssignInsn, remover, InsnType.NEW_INSTANCE);
			if (newInstInsn != null) {
				remover.addWithoutUnbind(newInstInsn);
				RegisterArg instArg = newInstInsn.getResult();
				RegisterArg resultArg = co.getResult();
				if (!resultArg.equals(instArg)) {
					// replace all usages of 'instArg' with result of this constructor instruction
					for (RegisterArg useArg : new ArrayList<>(instArg.getSVar().getUseList())) {
						InsnNode parentInsn = useArg.getParentInsn();
						if (parentInsn != null) {
							parentInsn.replaceArg(useArg, resultArg.duplicate());
						}
					}
				}
			}
		}
		ConstructorInsn replace = processConstructor(mth, co);
		if (replace != null) {
			remover.addAndUnbind(co);
			co = replace;
		}
		BlockUtils.replaceInsn(mth, block, indexInBlock, co);
	}

	/**
	 * Replace call of synthetic constructor with all 'null' args
	 * to a non-synthetic or default constructor if possible.
	 *
	 * @return insn for replacement or null if replace not needed or not possible.
	 */
	@Nullable
	private static ConstructorInsn processConstructor(MethodNode mth, ConstructorInsn co) {
		MethodNode callMth = mth.root().resolveMethod(co.getCallMth());
		if (callMth == null
				|| !callMth.getAccessFlags().isSynthetic()
				|| !allArgsNull(co)) {
			return null;
		}
		ClassNode classNode = mth.root().resolveClass(callMth.getParentClass().getClassInfo());
		if (classNode == null) {
			return null;
		}
		RegisterArg instanceArg = co.getResult();
		if (instanceArg == null) {
			return null;
		}
		boolean passThis = instanceArg.isThis();
		String ctrId = "<init>(" + (passThis ? TypeGen.signature(instanceArg.getInitType()) : "") + ")V";
		MethodNode defCtr = classNode.searchMethodByShortId(ctrId);
		if (defCtr == null || defCtr.equals(callMth) || defCtr.getAccessFlags().isSynthetic()) {
			return null;
		}
		ConstructorInsn newInsn = new ConstructorInsn(defCtr.getMethodInfo(), co.getCallType());
		newInsn.setResult(co.getResult().duplicate());
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
	private static InsnNode removeAssignChain(MethodNode mth, InsnNode insn, InsnRemover remover, InsnType insnType) {
		if (insn == null) {
			return null;
		}
		InsnType type = insn.getType();
		if (type == insnType) {
			return insn;
		}
		if (insn.isAttrStorageEmpty()) {
			remover.addWithoutUnbind(insn);
		} else {
			BlockUtils.replaceInsn(mth, insn, new InsnNode(InsnType.NOP, 0));
		}
		if (type == InsnType.MOVE) {
			RegisterArg arg = (RegisterArg) insn.getArg(0);
			return removeAssignChain(mth, arg.getAssignInsn(), remover, insnType);
		}
		return null;
	}
}
