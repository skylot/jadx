package jadx.core.dex.visitors.ssa;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.PhiListAttr;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.dex.visitors.blocks.BlockProcessor;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnList;
import jadx.core.utils.InsnRemover;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;

@JadxVisitor(
		name = "SSATransform",
		desc = "Calculate Single Side Assign (SSA) variables",
		runAfter = BlockProcessor.class
)
public class SSATransform extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		process(mth);
	}

	public static void rerun(MethodNode mth) {
		mth.remove(AFlag.RERUN_SSA_TRANSFORM);
		resetSSAVars(mth);
		process(mth);
	}

	private static void process(MethodNode mth) {
		if (!mth.getSVars().isEmpty()) {
			return;
		}
		LiveVarAnalysis la = new LiveVarAnalysis(mth);
		la.runAnalysis();
		int regsCount = mth.getRegsCount();
		for (int i = 0; i < regsCount; i++) {
			placePhi(mth, i, la);
		}
		renameVariables(mth);
		fixLastAssignInTry(mth);
		removeBlockerInsns(mth);
		markThisArgs(mth.getThisArg());
		tryToFixUselessPhi(mth);
		hidePhiInsns(mth);
		removeUnusedInvokeResults(mth);
	}

	private static void placePhi(MethodNode mth, int regNum, LiveVarAnalysis la) {
		List<BlockNode> blocks = mth.getBasicBlocks();
		int blocksCount = blocks.size();
		BitSet hasPhi = new BitSet(blocksCount);
		BitSet processed = new BitSet(blocksCount);
		Deque<BlockNode> workList = new ArrayDeque<>();

		BitSet assignBlocks = la.getAssignBlocks(regNum);
		for (int id = assignBlocks.nextSetBit(0); id >= 0; id = assignBlocks.nextSetBit(id + 1)) {
			processed.set(id);
			workList.add(blocks.get(id));
		}
		while (!workList.isEmpty()) {
			BlockNode block = workList.pop();
			BitSet domFrontier = block.getDomFrontier();
			for (int id = domFrontier.nextSetBit(0); id >= 0; id = domFrontier.nextSetBit(id + 1)) {
				if (!hasPhi.get(id) && la.isLive(id, regNum)) {
					BlockNode df = blocks.get(id);
					addPhi(mth, df, regNum);
					hasPhi.set(id);
					if (!processed.get(id)) {
						processed.set(id);
						workList.add(df);
					}
				}
			}
		}
	}

	public static PhiInsn addPhi(MethodNode mth, BlockNode block, int regNum) {
		PhiListAttr phiList = block.get(AType.PHI_LIST);
		if (phiList == null) {
			phiList = new PhiListAttr();
			block.addAttr(phiList);
		}
		int size = block.getPredecessors().size();
		if (mth.getEnterBlock() == block) {
			RegisterArg thisArg = mth.getThisArg();
			if (thisArg != null && thisArg.getRegNum() == regNum) {
				size++;
			} else {
				for (RegisterArg arg : mth.getArgRegs()) {
					if (arg.getRegNum() == regNum) {
						size++;
						break;
					}
				}
			}
		}
		PhiInsn phiInsn = new PhiInsn(regNum, size);
		phiList.getList().add(phiInsn);
		phiInsn.setOffset(block.getStartOffset());
		block.getInstructions().add(0, phiInsn);
		return phiInsn;
	}

	private static void renameVariables(MethodNode mth) {
		RenameState initState = RenameState.init(mth);
		initPhiInEnterBlock(initState);

		Deque<RenameState> stack = new ArrayDeque<>();
		stack.push(initState);
		while (!stack.isEmpty()) {
			RenameState state = stack.pop();
			renameVarsInBlock(mth, state);
			for (BlockNode dominated : state.getBlock().getDominatesOn()) {
				stack.push(RenameState.copyFrom(state, dominated));
			}
		}
	}

	private static void initPhiInEnterBlock(RenameState initState) {
		PhiListAttr phiList = initState.getBlock().get(AType.PHI_LIST);
		if (phiList != null) {
			for (PhiInsn phiInsn : phiList.getList()) {
				bindPhiArg(initState, phiInsn);
			}
		}
	}

	private static void renameVarsInBlock(MethodNode mth, RenameState state) {
		BlockNode block = state.getBlock();
		for (InsnNode insn : block.getInstructions()) {
			if (insn.getType() != InsnType.PHI) {
				for (InsnArg arg : insn.getArguments()) {
					if (!arg.isRegister()) {
						continue;
					}
					RegisterArg reg = (RegisterArg) arg;
					int regNum = reg.getRegNum();
					SSAVar var = state.getVar(regNum);
					if (var == null) {
						// TODO: in most cases issue in incorrectly attached exception handlers
						mth.addWarnComment("Not initialized variable reg: " + regNum + ", insn: " + insn + ", block:" + block);
						var = state.startVar(reg);
					}
					var.use(reg);
				}
			}
			RegisterArg result = insn.getResult();
			if (result != null) {
				state.startVar(result);
			}
		}
		for (BlockNode s : block.getSuccessors()) {
			PhiListAttr phiList = s.get(AType.PHI_LIST);
			if (phiList == null) {
				continue;
			}
			for (PhiInsn phiInsn : phiList.getList()) {
				bindPhiArg(state, phiInsn);
			}
		}
	}

	private static void bindPhiArg(RenameState state, PhiInsn phiInsn) {
		int regNum = phiInsn.getResult().getRegNum();
		SSAVar var = state.getVar(regNum);
		if (var == null) {
			return;
		}
		RegisterArg arg = phiInsn.bindArg(state.getBlock());
		var.use(arg);
		var.addUsedInPhi(phiInsn);
	}

	/**
	 * Fix last try/catch assign instruction
	 */
	private static void fixLastAssignInTry(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			PhiListAttr phiList = block.get(AType.PHI_LIST);
			if (phiList != null) {
				ExcHandlerAttr handlerAttr = block.get(AType.EXC_HANDLER);
				if (handlerAttr != null) {
					for (PhiInsn phi : phiList.getList()) {
						fixPhiInTryCatch(mth, phi, handlerAttr);
					}
				}
			}
		}
	}

	private static void fixPhiInTryCatch(MethodNode mth, PhiInsn phi, ExcHandlerAttr handlerAttr) {
		int argsCount = phi.getArgsCount();
		int k = 0;
		while (k < argsCount) {
			RegisterArg arg = phi.getArg(k);
			if (shouldSkipInsnResult(mth, arg.getAssignInsn(), handlerAttr)) {
				phi.removeArg(arg);
				argsCount--;
			} else {
				k++;
			}
		}
		if (phi.getArgsCount() == 0) {
			throw new JadxRuntimeException("PHI empty after try-catch fix!");
		}
	}

	private static boolean shouldSkipInsnResult(MethodNode mth, InsnNode insn, ExcHandlerAttr handlerAttr) {
		if (insn != null
				&& insn.getResult() != null
				&& insn.contains(AFlag.TRY_LEAVE)) {
			CatchAttr catchAttr = BlockUtils.getCatchAttrForInsn(mth, insn);
			return catchAttr != null && catchAttr.getHandlers().contains(handlerAttr.getHandler());
		}
		return false;
	}

	private static boolean removeBlockerInsns(MethodNode mth) {
		boolean removed = false;
		for (BlockNode block : mth.getBasicBlocks()) {
			PhiListAttr phiList = block.get(AType.PHI_LIST);
			if (phiList == null) {
				continue;
			}
			// check if args must be removed
			for (PhiInsn phi : phiList.getList()) {
				for (int i = 0; i < phi.getArgsCount(); i++) {
					RegisterArg arg = phi.getArg(i);
					InsnNode parentInsn = arg.getAssignInsn();
					if (parentInsn != null && parentInsn.contains(AFlag.REMOVE)) {
						phi.removeArg(arg);
						InsnRemover.remove(mth, block, parentInsn);
						removed = true;
					}
				}
			}
		}
		return removed;
	}

	private static void tryToFixUselessPhi(MethodNode mth) {
		int k = 0;
		int maxTries = mth.getSVars().size() * 2;
		while (fixUselessPhi(mth)) {
			if (k++ > maxTries) {
				throw new JadxRuntimeException("Phi nodes fix limit reached!");
			}
		}
	}

	private static boolean fixUselessPhi(MethodNode mth) {
		boolean changed = false;
		List<PhiInsn> insnToRemove = new ArrayList<>();
		for (SSAVar var : mth.getSVars()) {
			// phi result not used
			if (var.getUseCount() == 0) {
				InsnNode assignInsn = var.getAssign().getParentInsn();
				if (assignInsn != null && assignInsn.getType() == InsnType.PHI) {
					insnToRemove.add((PhiInsn) assignInsn);
					changed = true;
				}
			}
		}
		for (BlockNode block : mth.getBasicBlocks()) {
			PhiListAttr phiList = block.get(AType.PHI_LIST);
			if (phiList == null) {
				continue;
			}
			Iterator<PhiInsn> it = phiList.getList().iterator();
			while (it.hasNext()) {
				PhiInsn phi = it.next();
				if (fixPhiWithSameArgs(mth, block, phi)) {
					it.remove();
					changed = true;
				}
			}
		}
		removePhiList(mth, insnToRemove);
		return changed;
	}

	private static boolean fixPhiWithSameArgs(MethodNode mth, BlockNode block, PhiInsn phi) {
		if (phi.getArgsCount() == 0) {
			for (RegisterArg useArg : phi.getResult().getSVar().getUseList()) {
				InsnNode useInsn = useArg.getParentInsn();
				if (useInsn != null && useInsn.getType() == InsnType.PHI) {
					phi.removeArg(useArg);
				}
			}
			InsnRemover.remove(mth, block, phi);
			return true;
		}
		boolean allSame = phi.getArgsCount() == 1 || isSameArgs(phi);
		if (allSame) {
			return replacePhiWithMove(mth, block, phi, phi.getArg(0));
		}
		return false;
	}

	private static boolean isSameArgs(PhiInsn phi) {
		boolean allSame = true;
		SSAVar var = null;
		for (int i = 0; i < phi.getArgsCount(); i++) {
			RegisterArg arg = phi.getArg(i);
			if (var == null) {
				var = arg.getSVar();
			} else if (var != arg.getSVar()) {
				allSame = false;
				break;
			}
		}
		return allSame;
	}

	private static boolean removePhiList(MethodNode mth, List<PhiInsn> insnToRemove) {
		for (BlockNode block : mth.getBasicBlocks()) {
			PhiListAttr phiList = block.get(AType.PHI_LIST);
			if (phiList == null) {
				continue;
			}
			List<PhiInsn> list = phiList.getList();
			for (PhiInsn phiInsn : insnToRemove) {
				if (list.remove(phiInsn)) {
					for (InsnArg arg : phiInsn.getArguments()) {
						if (arg == null) {
							continue;
						}
						SSAVar sVar = ((RegisterArg) arg).getSVar();
						if (sVar != null) {
							sVar.removeUsedInPhi(phiInsn);
						}
					}
					InsnRemover.remove(mth, block, phiInsn);
				}
			}
			if (list.isEmpty()) {
				block.remove(AType.PHI_LIST);
			}
		}
		insnToRemove.clear();
		return true;
	}

	private static boolean replacePhiWithMove(MethodNode mth, BlockNode block, PhiInsn phi, RegisterArg arg) {
		List<InsnNode> insns = block.getInstructions();
		int phiIndex = InsnList.getIndex(insns, phi);
		if (phiIndex == -1) {
			return false;
		}
		SSAVar assign = phi.getResult().getSVar();
		SSAVar argVar = arg.getSVar();
		if (argVar != null) {
			argVar.removeUse(arg);
			argVar.removeUsedInPhi(phi);
		}
		// try inline
		if (inlinePhiInsn(mth, block, phi)) {
			insns.remove(phiIndex);
		} else {
			assign.removeUsedInPhi(phi);

			InsnNode m = new InsnNode(InsnType.MOVE, 1);
			m.add(AFlag.SYNTHETIC);
			m.setResult(phi.getResult());
			m.addArg(arg);
			arg.getSVar().use(arg);
			insns.set(phiIndex, m);
		}
		return true;
	}

	private static boolean inlinePhiInsn(MethodNode mth, BlockNode block, PhiInsn phi) {
		SSAVar resVar = phi.getResult().getSVar();
		if (resVar == null) {
			return false;
		}
		RegisterArg arg = phi.getArg(0);
		if (arg.getSVar() == null) {
			return false;
		}
		List<RegisterArg> useList = resVar.getUseList();
		for (RegisterArg useArg : new ArrayList<>(useList)) {
			InsnNode useInsn = useArg.getParentInsn();
			if (useInsn == null || useInsn == phi || useArg.getRegNum() != arg.getRegNum()) {
				return false;
			}
			// replace SSAVar in 'useArg' to SSAVar from 'arg'
			// no need to replace whole RegisterArg
			useArg.getSVar().removeUse(useArg);
			arg.getSVar().use(useArg);
		}
		if (block.contains(AType.EXC_HANDLER)) {
			// don't inline into exception handler
			InsnNode assignInsn = arg.getAssignInsn();
			if (assignInsn != null && !assignInsn.isConstInsn()) {
				assignInsn.add(AFlag.DONT_INLINE);
			}
		}
		InsnRemover.unbindInsn(mth, phi);
		return true;
	}

	private static void markThisArgs(RegisterArg thisArg) {
		if (thisArg != null) {
			markOneArgAsThis(thisArg);
			thisArg.getSVar().getUseList().forEach(SSATransform::markOneArgAsThis);
		}
	}

	private static void markOneArgAsThis(RegisterArg arg) {
		if (arg == null) {
			return;
		}
		arg.add(AFlag.THIS);
		arg.add(AFlag.IMMUTABLE_TYPE);
		// mark all moved 'this'
		InsnNode parentInsn = arg.getParentInsn();
		if (parentInsn != null
				&& parentInsn.getType() == InsnType.MOVE
				&& parentInsn.getArg(0) == arg) {
			RegisterArg resArg = parentInsn.getResult();
			if (resArg.getRegNum() != arg.getRegNum()
					&& !resArg.getSVar().isUsedInPhi()) {
				markThisArgs(resArg);
				parentInsn.add(AFlag.DONT_GENERATE);
			}
		}
	}

	private static void hidePhiInsns(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			block.getInstructions().removeIf(insn -> insn.getType() == InsnType.PHI);
		}
	}

	private static void resetSSAVars(MethodNode mth) {
		for (SSAVar ssaVar : mth.getSVars()) {
			ssaVar.getAssign().resetSSAVar();
			ssaVar.getUseList().forEach(RegisterArg::resetSSAVar);
		}
		for (BlockNode block : mth.getBasicBlocks()) {
			block.remove(AType.PHI_LIST);
		}
		mth.getSVars().clear();
	}

	private static void removeUnusedInvokeResults(MethodNode mth) {
		Iterator<SSAVar> it = mth.getSVars().iterator();
		while (it.hasNext()) {
			SSAVar ssaVar = it.next();
			if (ssaVar.getUseCount() == 0) {
				InsnNode parentInsn = ssaVar.getAssign().getParentInsn();
				if (parentInsn != null && parentInsn.getType() == InsnType.INVOKE) {
					parentInsn.setResult(null);
					it.remove();
				}
			}
		}
	}
}
