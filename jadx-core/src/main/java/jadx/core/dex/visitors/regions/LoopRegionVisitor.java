package jadx.core.dex.visitors.regions;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.dex.visitors.regions.variables.ProcessVariables;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnRemover;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.exceptions.JadxOverflowException;

@JadxVisitor(
		name = "LoopRegionVisitor",
		desc = "Convert 'while' loops to 'for' loops (indexed or for-each)",
		runBefore = ProcessVariables.class
)
public class LoopRegionVisitor extends AbstractVisitor implements IRegionVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(LoopRegionVisitor.class);

	@Override
	public void visit(MethodNode mth) {
		DepthRegionTraversal.traverse(mth, this);
		if (mth.contains(AFlag.REQUEST_IF_REGION_OPTIMIZE)) {
			IfRegionVisitor.process(mth);
			mth.remove(AFlag.REQUEST_IF_REGION_OPTIMIZE);
		}
	}

	@Override
	public boolean enterRegion(MethodNode mth, IRegion region) {
		if (region instanceof LoopRegion) {
			if (processLoopRegion(mth, (LoopRegion) region)) {
				// optimize `if` block after instructions remove
				mth.add(AFlag.REQUEST_IF_REGION_OPTIMIZE);
			}
		}
		return true;
	}

	private static boolean processLoopRegion(MethodNode mth, LoopRegion loopRegion) {
		if (loopRegion.isConditionAtEnd()) {
			return false;
		}
		IfCondition condition = loopRegion.getCondition();
		if (condition == null) {
			return false;
		}
		if (checkForIndexedLoop(mth, loopRegion, condition)) {
			return true;
		}
		return checkIterableForEach(mth, loopRegion, condition);
	}

	/**
	 * Check for indexed loop.
	 */
	private static boolean checkForIndexedLoop(MethodNode mth, LoopRegion loopRegion, IfCondition condition) {
		BlockNode loopEndBlock = loopRegion.getInfo().getEnd();
		InsnNode incrInsn = BlockUtils.getLastInsn(BlockUtils.skipSyntheticPredecessor(loopEndBlock));
		if (incrInsn == null) {
			return false;
		}
		RegisterArg incrArg = incrInsn.getResult();
		if (incrArg == null
				|| incrArg.getSVar() == null
				|| !incrArg.getSVar().isUsedInPhi()) {
			return false;
		}
		List<PhiInsn> phiInsnList = incrArg.getSVar().getUsedInPhi();
		if (phiInsnList.size() != 1) {
			return false;
		}
		PhiInsn phiInsn = phiInsnList.get(0);
		if (phiInsn.getArgsCount() != 2
				|| !phiInsn.containsVar(incrArg)
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
		if (initInsn == null
				|| initInsn.contains(AFlag.DONT_GENERATE)
				|| initArg.getSVar().getUseCount() != 1) {
			return false;
		}
		if (!usedOnlyInLoop(mth, loopRegion, arg)) {
			return false;
		}
		// can't make loop if argument from increment instruction is assign in loop
		List<RegisterArg> args = new ArrayList<>();
		incrInsn.getRegisterArgs(args);
		for (RegisterArg iArg : args) {
			try {
				if (assignOnlyInLoop(mth, loopRegion, iArg)) {
					return false;
				}
			} catch (StackOverflowError error) {
				throw new JadxOverflowException("LoopRegionVisitor.assignOnlyInLoop endless recursion");
			}
		}

		// all checks passed
		initInsn.add(AFlag.DONT_GENERATE);
		incrInsn.add(AFlag.DONT_GENERATE);

		LoopType arrForEach = checkArrayForEach(mth, loopRegion, initInsn, incrInsn, condition);
		loopRegion.setType(arrForEach != null ? arrForEach : new ForLoop(initInsn, incrInsn));
		return true;
	}

	private static LoopType checkArrayForEach(MethodNode mth, LoopRegion loopRegion, InsnNode initInsn, InsnNode incrInsn,
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
		if (args.size() != 3) {
			return null;
		}
		condArg = InsnUtils.getRegFromInsn(args, InsnType.IF);
		if (condArg == null) {
			return null;
		}
		RegisterArg arrIndex = InsnUtils.getRegFromInsn(args, InsnType.AGET);
		if (arrIndex == null) {
			return null;
		}
		InsnNode arrGetInsn = arrIndex.getParentInsn();
		if (arrGetInsn == null || arrGetInsn.containsWrappedInsn()) {
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
		if (iterVar != null) {
			if (!usedOnlyInLoop(mth, loopRegion, iterVar)) {
				return null;
			}
		} else {
			if (!arrGetInsn.contains(AFlag.WRAPPED)) {
				return null;
			}
			// create new variable and replace wrapped insn
			InsnArg wrapArg = BlockUtils.searchWrappedInsnParent(mth, arrGetInsn);
			if (wrapArg == null || wrapArg.getParentInsn() == null) {
				mth.addWarnComment("checkArrayForEach: Wrapped insn not found: " + arrGetInsn);
				return null;
			}
			iterVar = mth.makeSyntheticRegArg(wrapArg.getType());
			InsnNode parentInsn = wrapArg.getParentInsn();
			parentInsn.replaceArg(wrapArg, iterVar.duplicate());
			parentInsn.rebindArgs();
		}

		// array for each loop confirmed
		incrInsn.getResult().add(AFlag.DONT_GENERATE);
		condArg.add(AFlag.DONT_GENERATE);
		bCondArg.add(AFlag.DONT_GENERATE);
		arrGetInsn.add(AFlag.DONT_GENERATE);
		compare.getInsn().add(AFlag.DONT_GENERATE);

		ForEachLoop forEachLoop = new ForEachLoop(iterVar, len.getArg(0));
		forEachLoop.injectFakeInsns(loopRegion);
		if (InsnUtils.dontGenerateIfNotUsed(len)) {
			InsnRemover.remove(mth, len);
		}
		CodeShrinkVisitor.shrinkMethod(mth);
		return forEachLoop;
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
		List<RegisterArg> itUseList = sVar.getUseList();
		InsnNode assignInsn = iteratorArg.getAssignInsn();
		if (itUseList.size() != 2) {
			return false;
		}
		if (!checkInvoke(assignInsn, null, "iterator()Ljava/util/Iterator;")) {
			return false;
		}
		InsnArg iterableArg = assignInsn.getArg(0);
		InsnNode hasNextCall = itUseList.get(0).getParentInsn();
		InsnNode nextCall = itUseList.get(1).getParentInsn();
		if (!checkInvoke(hasNextCall, "java.util.Iterator", "hasNext()Z")
				|| !checkInvoke(nextCall, "java.util.Iterator", "next()Ljava/lang/Object;")) {
			return false;
		}
		List<InsnNode> toSkip = new ArrayList<>();
		RegisterArg iterVar;
		if (nextCall.contains(AFlag.WRAPPED)) {
			InsnArg wrapArg = BlockUtils.searchWrappedInsnParent(mth, nextCall);
			if (wrapArg != null && wrapArg.getParentInsn() != null) {
				InsnNode parentInsn = wrapArg.getParentInsn();
				BlockNode block = BlockUtils.getBlockByInsn(mth, parentInsn);
				if (block == null) {
					return false;
				}
				if (!RegionUtils.isRegionContainsBlock(loopRegion, block)) {
					return false;
				}
				if (parentInsn.getType() == InsnType.CHECK_CAST) {
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
				} else {
					iterVar = nextCall.getResult();
					if (iterVar == null) {
						return false;
					}
					iterVar.remove(AFlag.REMOVE); // restore variable from inlined insn
					nextCall.add(AFlag.DONT_GENERATE);
					if (!fixIterableType(mth, iterableArg, iterVar)) {
						return false;
					}
					parentInsn.replaceArg(wrapArg, iterVar);
				}
			} else {
				LOG.warn(" checkIterableForEach: Wrapped insn not found: {}, mth: {}", nextCall, mth);
				return false;
			}
		} else {
			iterVar = nextCall.getResult();
			if (iterVar == null) {
				return false;
			}
			if (!usedOnlyInLoop(mth, loopRegion, iterVar)) {
				return false;
			}
			if (!assignOnlyInLoop(mth, loopRegion, iterVar)) {
				return false;
			}
			toSkip.add(nextCall);
		}

		assignInsn.add(AFlag.DONT_GENERATE);
		assignInsn.getResult().add(AFlag.DONT_GENERATE);

		for (InsnNode insnNode : toSkip) {
			insnNode.setResult(null);
			insnNode.add(AFlag.DONT_GENERATE);
		}
		for (RegisterArg itArg : itUseList) {
			itArg.add(AFlag.DONT_GENERATE);
		}
		ForEachLoop forEachLoop = new ForEachLoop(iterVar, iterableArg);
		forEachLoop.injectFakeInsns(loopRegion);
		loopRegion.setType(forEachLoop);
		return true;
	}

	private static boolean fixIterableType(MethodNode mth, InsnArg iterableArg, RegisterArg iterVar) {
		ArgType iterableType = iterableArg.getType();
		ArgType varType = iterVar.getType();
		if (iterableType.isGeneric()) {
			List<ArgType> genericTypes = iterableType.getGenericTypes();
			if (genericTypes == null || genericTypes.size() != 1) {
				return false;
			}
			ArgType gType = genericTypes.get(0);
			if (gType.equals(varType)) {
				return true;
			}
			if (gType.isGenericType()) {
				iterVar.setType(gType);
				return true;
			}
			if (ArgType.isInstanceOf(mth.root(), gType, varType)) {
				return true;
			}
			ArgType wildcardType = gType.getWildcardType();
			if (wildcardType != null
					&& gType.getWildcardBound() == ArgType.WildcardBound.EXTENDS
					&& ArgType.isInstanceOf(mth.root(), wildcardType, varType)) {
				return true;
			}
			LOG.warn("Generic type differs: '{}' and '{}' in {}", gType, varType, mth);
			return false;
		}
		if (!iterableArg.isRegister() || !iterableType.isObject()) {
			return true;
		}
		ArgType genericType = ArgType.generic(iterableType.getObject(), varType);
		if (iterableArg.isRegister()) {
			ArgType immutableType = ((RegisterArg) iterableArg).getImmutableType();
			if (immutableType != null && !immutableType.equals(genericType)) {
				// can't change type
				// allow to iterate over not generified collection only for Object vars
				return varType.equals(ArgType.OBJECT);
			}
		}
		iterableArg.setType(genericType);
		return true;
	}

	/**
	 * Check if instruction is a interface invoke with corresponding parameters.
	 */
	private static boolean checkInvoke(InsnNode insn, String declClsFullName, String mthId) {
		if (insn == null) {
			return false;
		}
		if (insn.getType() == InsnType.INVOKE) {
			InvokeNode inv = (InvokeNode) insn;
			MethodInfo callMth = inv.getCallMth();
			if (inv.getInvokeType() == InvokeType.INTERFACE
					&& callMth.getShortId().equals(mthId)) {
				if (declClsFullName == null) {
					return true;
				}
				return callMth.getDeclClass().getFullName().equals(declClsFullName);
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
