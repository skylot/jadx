package jadx.core.dex.visitors.regions;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.ArithOp;
import jadx.core.dex.instructions.IfOp;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.InvokeType;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.conditions.Compare;
import jadx.core.dex.regions.conditions.IfCondition;
import jadx.core.dex.regions.loops.ForEachLoop;
import jadx.core.dex.regions.loops.ForLoop;
import jadx.core.dex.regions.loops.LoopRegion;
import jadx.core.dex.regions.loops.LoopType;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.CodeShrinker;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InstructionRemover;
import jadx.core.utils.RegionUtils;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopRegionVisitor extends AbstractVisitor implements IRegionVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(LoopRegionVisitor.class);

	@Override
	public void visit(MethodNode mth) {
		DepthRegionTraversal.traverse(mth, this);
	}

	@Override
	public boolean enterRegion(MethodNode mth, IRegion region) {
		if (region instanceof LoopRegion) {
			processLoopRegion(mth, (LoopRegion) region);
		}
		return true;
	}

	private static void processLoopRegion(MethodNode mth, LoopRegion loopRegion) {
		if (loopRegion.isConditionAtEnd()) {
			return;
		}
		IfCondition condition = loopRegion.getCondition();
		if (condition == null) {
			return;
		}
		if (checkForIndexedLoop(mth, loopRegion, condition)) {
			return;
		}
		if (checkIterableForEach(mth, loopRegion, condition)) {
			return;
		}
	}

	/**
	 * Check for indexed loop.
	 */
	private static boolean checkForIndexedLoop(MethodNode mth, LoopRegion loopRegion, IfCondition condition) {
		InsnNode incrInsn = RegionUtils.getLastInsn(loopRegion);
		if (incrInsn == null) {
			return false;
		}
		RegisterArg incrArg = incrInsn.getResult();
		if (incrArg == null
				|| incrArg.getSVar() == null
				|| !incrArg.getSVar().isUsedInPhi()) {
			return false;
		}
		PhiInsn phiInsn = incrArg.getSVar().getUsedInPhi();
		if (phiInsn == null
				|| phiInsn.getArgsCount() != 2
				|| !phiInsn.getArg(1).equals(incrArg)
				|| incrArg.getSVar().getUseCount() != 1) {
			return false;
		}
		RegisterArg arg = phiInsn.getResult();
		List<RegisterArg> condArgs = condition.getRegisterArgs();
		if (!condArgs.contains(arg) || arg.getSVar().isUsedInPhi()) {
			return false;
		}
		RegisterArg initArg = phiInsn.getArg(0);
		InsnNode initInsn = initArg.getAssignInsn();
		if (initInsn == null || initArg.getSVar().getUseCount() != 1) {
			return false;
		}
		if (!usedOnlyInLoop(mth, loopRegion, arg)) {
			return false;
		}
		// can't make loop if argument from increment instruction is assign in loop
		List<RegisterArg> args = new LinkedList<RegisterArg>();
		incrInsn.getRegisterArgs(args);
		for (RegisterArg iArg : args) {
			if (assignOnlyInLoop(mth, loopRegion, iArg)) {
				return false;
			}
		}

		// all checks passed
		initInsn.add(AFlag.SKIP);
		incrInsn.add(AFlag.SKIP);
		LoopType arrForEach = checkArrayForEach(mth, initInsn, incrInsn, condition);
		if (arrForEach != null) {
			loopRegion.setType(arrForEach);
			return true;
		}
		loopRegion.setType(new ForLoop(initInsn, incrInsn));
		return true;
	}

	private static LoopType checkArrayForEach(MethodNode mth, InsnNode initInsn, InsnNode incrInsn,
			IfCondition condition) {
		if (!(incrInsn instanceof ArithNode)) {
			return null;
		}
		ArithNode arithNode = (ArithNode) incrInsn;
		if (arithNode.getOp() != ArithOp.ADD) {
			return null;
		}
		InsnArg lit = incrInsn.getArg(1);
		if (!lit.isLiteral() || ((LiteralArg) lit).getLiteral() != 1) {
			return null;
		}
		if (initInsn.getType() != InsnType.CONST
				|| !initInsn.getArg(0).isLiteral()
				|| ((LiteralArg) initInsn.getArg(0)).getLiteral() != 0) {
			return null;
		}

		InsnArg condArg = incrInsn.getArg(0);
		if (!condArg.isRegister()) {
			return null;
		}
		SSAVar sVar = ((RegisterArg) condArg).getSVar();
		List<RegisterArg> args = sVar.getUseList();
		if (args.size() != 3 || args.get(2) != condArg) {
			return null;
		}
		condArg = args.get(0);
		RegisterArg arrIndex = args.get(1);
		InsnNode arrGetInsn = arrIndex.getParentInsn();
		if (arrGetInsn == null || arrGetInsn.getType() != InsnType.AGET) {
			return null;
		}
		if (!condition.isCompare()) {
			return null;
		}
		Compare compare = condition.getCompare();
		if (compare.getOp() != IfOp.LT || compare.getA() != condArg) {
			return null;
		}
		InsnNode len;
		InsnArg bCondArg = compare.getB();
		if (bCondArg.isInsnWrap()) {
			len = ((InsnWrapArg) bCondArg).getWrapInsn();
		} else if (bCondArg.isRegister()) {
			len = ((RegisterArg) bCondArg).getAssignInsn();
		} else {
			return null;
		}
		if (len == null || len.getType() != InsnType.ARRAY_LENGTH) {
			return null;
		}
		InsnArg arrayArg = len.getArg(0);
		if (!arrayArg.equals(arrGetInsn.getArg(0))) {
			return null;
		}
		RegisterArg iterVar = arrGetInsn.getResult();
		if (iterVar == null) {
			return null;
		}

		// array for each loop confirmed
		len.add(AFlag.SKIP);
		arrGetInsn.add(AFlag.SKIP);
		InstructionRemover.unbindInsn(mth, len);

		// inline array variable
		CodeShrinker.shrinkMethod(mth);
		if (arrGetInsn.contains(AFlag.WRAPPED)) {
			InsnArg wrapArg = BlockUtils.searchWrappedInsnParent(mth, arrGetInsn);
			if (wrapArg != null && wrapArg.getParentInsn() != null) {
				wrapArg.getParentInsn().replaceArg(wrapArg, iterVar);
			} else {
				LOG.debug(" checkArrayForEach: Wrapped insn not found: {}, mth: {}", arrGetInsn, mth);
			}
		}
		return new ForEachLoop(iterVar, len.getArg(0));
	}

	private static boolean checkIterableForEach(MethodNode mth, LoopRegion loopRegion, IfCondition condition) {
		List<RegisterArg> condArgs = condition.getRegisterArgs();
		if (condArgs.size() != 1) {
			return false;
		}
		RegisterArg iteratorArg = condArgs.get(0);
		SSAVar sVar = iteratorArg.getSVar();
		if (sVar == null || sVar.isUsedInPhi()) {
			return false;
		}
		List<RegisterArg> useList = sVar.getUseList();
		InsnNode assignInsn = iteratorArg.getAssignInsn();
		if (useList.size() != 2
				|| assignInsn == null
				|| !checkInvoke(assignInsn, null, "iterator()Ljava/util/Iterator;", 0)) {
			return false;
		}
		InsnArg iterableArg = assignInsn.getArg(0);
		InsnNode hasNextCall = useList.get(0).getParentInsn();
		InsnNode nextCall = useList.get(1).getParentInsn();
		if (hasNextCall == null || nextCall == null
				|| !checkInvoke(hasNextCall, "java.util.Iterator", "hasNext()Z", 0)
				|| !checkInvoke(nextCall, "java.util.Iterator", "next()Ljava/lang/Object;", 0)) {
			return false;
		}
		List<InsnNode> toSkip = new LinkedList<InsnNode>();
		RegisterArg iterVar = nextCall.getResult();
		if (iterVar == null) {
			return false;
		}
		if (nextCall.contains(AFlag.WRAPPED)) {
			InsnArg wrapArg = BlockUtils.searchWrappedInsnParent(mth, nextCall);
			if (wrapArg != null && wrapArg.getParentInsn() != null) {
				InsnNode parentInsn = wrapArg.getParentInsn();
				if (parentInsn.getType() != InsnType.CHECK_CAST) {
					if (!fixIterableType(mth, iterableArg, iterVar)) {
						return false;
					}
					parentInsn.replaceArg(wrapArg, iterVar);
				} else {
					iterVar = parentInsn.getResult();
					if (iterVar == null || !fixIterableType(mth, iterableArg, iterVar)) {
						return false;
					}
					InsnArg castArg = BlockUtils.searchWrappedInsnParent(mth, parentInsn);
					if (castArg != null && castArg.getParentInsn() != null) {
						castArg.getParentInsn().replaceArg(castArg, iterVar);
					} else {
						// cast not inlined
						toSkip.add(parentInsn);
					}
				}
			} else {
				LOG.warn(" checkIterableForEach: Wrapped insn not found: {}, mth: {}", nextCall, mth);
				return false;
			}
		} else {
			toSkip.add(nextCall);
		}

		assignInsn.add(AFlag.SKIP);
		for (InsnNode insnNode : toSkip) {
			insnNode.add(AFlag.SKIP);
		}
		loopRegion.setType(new ForEachLoop(iterVar, iterableArg));
		return true;
	}

	private static boolean fixIterableType(MethodNode mth, InsnArg iterableArg, RegisterArg iterVar) {
		ArgType iterableType = iterableArg.getType();
		ArgType varType = iterVar.getType();
		if (iterableType.isGeneric()) {
			ArgType[] genericTypes = iterableType.getGenericTypes();
			if (genericTypes == null || genericTypes.length != 1) {
				return false;
			}
			ArgType gType = genericTypes[0];
			if (gType.equals(varType)) {
				return true;
			}
			if (gType.isGenericType()) {
				iterVar.setType(gType);
				return true;
			}
			if (ArgType.isInstanceOf(mth.dex(), gType, varType)) {
				return true;
			}
			ArgType wildcardType = gType.getWildcardType();
			if (wildcardType != null
					&& gType.getWildcardBounds() == 1
					&& ArgType.isInstanceOf(mth.dex(), wildcardType, varType)) {
				return true;
			}
			LOG.warn("Generic type differs: '{}' and '{}' in {}", gType, varType, mth);
			return false;
		}
		if (!iterableArg.isRegister()) {
			return true;
		}
		// TODO: add checks
		iterableType = ArgType.generic(iterableType.getObject(), new ArgType[]{varType});
		iterableArg.setType(iterableType);
		return true;
	}

	/**
	 * Check if instruction is a interface invoke with corresponding parameters.
	 */
	private static boolean checkInvoke(InsnNode insn, String declClsFullName, String mthId, int argsCount) {
		if (insn.getType() == InsnType.INVOKE) {
			InvokeNode inv = (InvokeNode) insn;
			MethodInfo callMth = inv.getCallMth();
			if (callMth.getArgsCount() == argsCount
					&& callMth.getShortId().equals(mthId)
					&& inv.getInvokeType() == InvokeType.INTERFACE) {
				return declClsFullName == null || callMth.getDeclClass().getFullName().equals(declClsFullName);
			}
		}
		return false;
	}

	private static boolean assignOnlyInLoop(MethodNode mth, LoopRegion loopRegion, RegisterArg arg) {
		InsnNode assignInsn = arg.getAssignInsn();
		if (assignInsn == null) {
			return true;
		}
		if (!argInLoop(mth, loopRegion, assignInsn.getResult())) {
			return false;
		}
		if (assignInsn instanceof PhiInsn) {
			PhiInsn phiInsn = (PhiInsn) assignInsn;
			for (InsnArg phiArg : phiInsn.getArguments()) {
				if (!assignOnlyInLoop(mth, loopRegion, (RegisterArg) phiArg)) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean usedOnlyInLoop(MethodNode mth, LoopRegion loopRegion, RegisterArg arg) {
		List<RegisterArg> useList = arg.getSVar().getUseList();
		for (RegisterArg useArg : useList) {
			if (!argInLoop(mth, loopRegion, useArg)) {
				return false;
			}
		}
		return true;
	}

	private static boolean argInLoop(MethodNode mth, LoopRegion loopRegion, RegisterArg arg) {
		InsnNode parentInsn = arg.getParentInsn();
		if (parentInsn == null) {
			return false;
		}
		BlockNode block = BlockUtils.getBlockByInsn(mth, parentInsn);
		if (block == null) {
			LOG.debug(" LoopRegionVisitor: instruction not found: {}, mth: {}", parentInsn, mth);
			return false;
		}
		return RegionUtils.isRegionContainsBlock(loopRegion, block);
	}

	@Override
	public void leaveRegion(MethodNode mth, IRegion region) {
	}

	@Override
	public void processBlock(MethodNode mth, IBlock container) {
	}
}
