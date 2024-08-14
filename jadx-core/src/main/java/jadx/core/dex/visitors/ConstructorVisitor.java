package jadx.core.dex.visitors;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.core.codegen.TypeGen;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.PhiInsn;
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
import jadx.core.utils.exceptions.JadxRuntimeException;

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
		if (replaceInvoke(mth)) {
			MoveInlineVisitor.moveInline(mth);
		}
	}

	private static boolean replaceInvoke(MethodNode mth) {
		boolean replaced = false;
		InsnRemover remover = new InsnRemover(mth);
		for (BlockNode block : mth.getBasicBlocks()) {
			remover.setBlock(block);
			int size = block.getInstructions().size();
			for (int i = 0; i < size; i++) {
				InsnNode insn = block.getInstructions().get(i);
				if (insn.getType() == InsnType.INVOKE) {
					replaced |= processInvoke(mth, block, i, remover);
				}
			}
			remover.perform();
		}
		return replaced;
	}

	private static boolean processInvoke(MethodNode mth, BlockNode block, int indexInBlock, InsnRemover remover) {
		InvokeNode inv = (InvokeNode) block.getInstructions().get(indexInBlock);
		if (!inv.getCallMth().isConstructor()) {
			return false;
		}
		ConstructorInsn co = new ConstructorInsn(mth, inv);
		if (canRemoveConstructor(mth, co)) {
			remover.addAndUnbind(inv);
			return false;
		}
		co.inheritMetadata(inv);

		RegisterArg instanceArg = (RegisterArg) inv.getArg(0);
		instanceArg.getSVar().removeUse(instanceArg);
		if (co.isNewInstance()) {
			InsnNode assignInsn = instanceArg.getAssignInsn();
			if (assignInsn != null) {
				if (assignInsn.getType() == InsnType.CONSTRUCTOR) {
					// arg already used in another constructor instruction
					// insert new PHI insn to merge these branched constructors results
					instanceArg = insertPhiInsn(mth, block, instanceArg, ((ConstructorInsn) assignInsn));
				} else {
					InsnNode newInstInsn = removeAssignChain(mth, assignInsn, remover, InsnType.NEW_INSTANCE);
					if (newInstInsn != null) {
						co.inheritMetadata(newInstInsn);
						newInstInsn.add(AFlag.REMOVE);
						remover.addWithoutUnbind(newInstInsn);
					}
				}
			}
			// convert instance arg from 'use' to 'assign'
			co.setResult(instanceArg.duplicate());
		}
		co.rebindArgs();
		ConstructorInsn replace = processConstructor(mth, co);
		if (replace != null) {
			remover.addAndUnbind(co);
			BlockUtils.replaceInsn(mth, block, indexInBlock, replace);
		} else {
			BlockUtils.replaceInsn(mth, block, indexInBlock, co);
		}
		return true;
	}

	private static RegisterArg insertPhiInsn(MethodNode mth, BlockNode curBlock,
			RegisterArg instArg, ConstructorInsn otherCtr) {
		BlockNode otherBlock = BlockUtils.getBlockByInsn(mth, otherCtr);
		if (otherBlock == null) {
			throw new JadxRuntimeException("Block not found by insn: " + otherCtr);
		}
		BlockNode crossBlock = BlockUtils.getPathCross(mth, curBlock, otherBlock);
		if (crossBlock == null) {
			// no path cross => PHI insn not needed
			// use new SSA var on usage from current path
			RegisterArg newResArg = instArg.duplicateWithNewSSAVar(mth);
			List<BlockNode> pathBlocks = BlockUtils.collectAllSuccessors(mth, curBlock, true);
			for (RegisterArg useReg : instArg.getSVar().getUseList()) {
				InsnNode parentInsn = useReg.getParentInsn();
				if (parentInsn != null) {
					BlockNode useBlock = BlockUtils.getBlockByInsn(mth, parentInsn, pathBlocks);
					if (useBlock != null) {
						parentInsn.replaceArg(useReg, newResArg.duplicate());
					}
				}
			}
			return newResArg;
		}
		RegisterArg newResArg = instArg.duplicateWithNewSSAVar(mth);
		RegisterArg useArg = otherCtr.getResult();
		RegisterArg otherResArg = useArg.duplicateWithNewSSAVar(mth);

		PhiInsn phiInsn = SSATransform.addPhi(mth, crossBlock, useArg.getRegNum());
		phiInsn.setResult(useArg.duplicate());
		phiInsn.bindArg(newResArg.duplicate(), BlockUtils.getPrevBlockOnPath(mth, crossBlock, curBlock));
		phiInsn.bindArg(otherResArg.duplicate(), BlockUtils.getPrevBlockOnPath(mth, crossBlock, otherBlock));
		phiInsn.rebindArgs();

		otherCtr.setResult(otherResArg.duplicate());
		otherCtr.rebindArgs();
		return newResArg;
	}

	private static boolean canRemoveConstructor(MethodNode mth, ConstructorInsn co) {
		ClassNode parentClass = mth.getParentClass();
		if (co.isSuper() && (co.getArgsCount() == 0 || parentClass.isEnum())) {
			return true;
		}
		if (co.isThis() && co.getArgsCount() == 0) {
			MethodNode defCo = parentClass.searchMethodByShortId(co.getCallMth().getShortId());
			if (defCo == null || defCo.isNoCode()) {
				// default constructor not implemented
				return true;
			}
		}
		// remove super() call in instance initializer
		return parentClass.isAnonymous() && mth.isDefaultConstructor() && co.isSuper();
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
		newInsn.inheritMetadata(co);
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
