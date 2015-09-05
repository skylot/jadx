package jadx.core.dex.visitors.ssa;

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
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.dex.visitors.blocksmaker.BlockFinish;
import jadx.core.utils.InsnList;
import jadx.core.utils.InstructionRemover;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@JadxVisitor(
		name = "SSATransform",
		desc = "Calculate Single Side Assign (SSA) variables",
		runAfter = BlockFinish.class
)
public class SSATransform extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		process(mth);
	}

	private static void process(MethodNode mth) {
		LiveVarAnalysis la = new LiveVarAnalysis(mth);
		la.runAnalysis();
		int regsCount = mth.getRegsCount();
		for (int i = 0; i < regsCount; i++) {
			placePhi(mth, i, la);
		}
		renameVariables(mth);

		fixLastAssignInTry(mth);
		removeBlockerInsns(mth);

		boolean repeatFix;
		int k = 0;
		do {
			repeatFix = fixUselessPhi(mth);
			if (k++ > 50) {
				throw new JadxRuntimeException("Phi nodes fix limit reached!");
			}
		} while (repeatFix);
	}

	private static void placePhi(MethodNode mth, int regNum, LiveVarAnalysis la) {
		List<BlockNode> blocks = mth.getBasicBlocks();
		int blocksCount = blocks.size();
		BitSet hasPhi = new BitSet(blocksCount);
		BitSet processed = new BitSet(blocksCount);
		Deque<BlockNode> workList = new LinkedList<BlockNode>();

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
			for (RegisterArg arg : mth.getArguments(true)) {
				if (arg.getRegNum() == regNum) {
					size++;
					break;
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
		if (!mth.getSVars().isEmpty()) {
			throw new JadxRuntimeException("SSA rename variables already executed");
		}
		int regsCount = mth.getRegsCount();
		SSAVar[] vars = new SSAVar[regsCount];
		int[] versions = new int[regsCount];
		// init method arguments
		for (RegisterArg arg : mth.getArguments(true)) {
			int regNum = arg.getRegNum();
			vars[regNum] = newSSAVar(mth, versions, arg, regNum);
		}
		BlockNode enterBlock = mth.getEnterBlock();
		initPhiInEnterBlock(vars, enterBlock);
		renameVar(mth, vars, versions, enterBlock);
	}

	private static SSAVar newSSAVar(MethodNode mth, int[] versions, RegisterArg arg, int regNum) {
		int version = versions[regNum]++;
		return mth.makeNewSVar(regNum, version, arg);
	}

	private static void initPhiInEnterBlock(SSAVar[] vars, BlockNode enterBlock) {
		PhiListAttr phiList = enterBlock.get(AType.PHI_LIST);
		if (phiList != null) {
			for (PhiInsn phiInsn : phiList.getList()) {
				int regNum = phiInsn.getResult().getRegNum();
				SSAVar var = vars[regNum];
				if (var == null) {
					continue;
				}
				RegisterArg arg = phiInsn.bindArg(enterBlock);
				var.use(arg);
				var.setUsedInPhi(phiInsn);
			}
		}
	}

	private static void renameVar(MethodNode mth, SSAVar[] vars, int[] vers, BlockNode block) {
		SSAVar[] inputVars = Arrays.copyOf(vars, vars.length);
		for (InsnNode insn : block.getInstructions()) {
			if (insn.getType() != InsnType.PHI) {
				for (InsnArg arg : insn.getArguments()) {
					if (!arg.isRegister()) {
						continue;
					}
					RegisterArg reg = (RegisterArg) arg;
					int regNum = reg.getRegNum();
					SSAVar var = vars[regNum];
					if (var == null) {
						throw new JadxRuntimeException("Not initialized variable reg: " + regNum
								+ ", insn: " + insn + ", block:" + block + ", method: " + mth);
					}
					var.use(reg);
				}
			}
			RegisterArg result = insn.getResult();
			if (result != null) {
				int regNum = result.getRegNum();
				vars[regNum] = newSSAVar(mth, vers, result, regNum);
			}
		}
		for (BlockNode s : block.getSuccessors()) {
			PhiListAttr phiList = s.get(AType.PHI_LIST);
			if (phiList == null) {
				continue;
			}
			for (PhiInsn phiInsn : phiList.getList()) {
				int regNum = phiInsn.getResult().getRegNum();
				SSAVar var = vars[regNum];
				if (var == null) {
					continue;
				}
				RegisterArg arg = phiInsn.bindArg(block);
				var.use(arg);
				var.setUsedInPhi(phiInsn);
			}
		}
		for (BlockNode domOn : block.getDominatesOn()) {
			renameVar(mth, vars, vers, domOn);
		}
		System.arraycopy(inputVars, 0, vars, 0, vars.length);
	}

	/**
	 * Fix last try/catch assign instruction
	 */
	private static void fixLastAssignInTry(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			PhiListAttr phiList = block.get(AType.PHI_LIST);
			if (phiList != null && block.contains(AType.EXC_HANDLER)) {
				for (PhiInsn phi : phiList.getList()) {
					fixPhiInTryCatch(phi);
				}
			}
		}
	}

	private static void fixPhiInTryCatch(PhiInsn phi) {
		int argsCount = phi.getArgsCount();
		int k = 0;
		while (k < argsCount) {
			RegisterArg arg = phi.getArg(k);
			InsnNode parentInsn = arg.getAssignInsn();
			if (parentInsn != null
					&& parentInsn.getResult() != null
					&& parentInsn.contains(AFlag.TRY_LEAVE)) {
				if (phi.removeArg(arg)) {
					argsCount--;
					continue;
				}
			}
			k++;
		}
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
						InstructionRemover.remove(mth, block, parentInsn);
						removed = true;
					}
				}
			}
		}
		return removed;
	}

	private static boolean fixUselessPhi(MethodNode mth) {
		boolean changed = false;
		List<PhiInsn> insnToRemove = new ArrayList<PhiInsn>();
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
			InstructionRemover.remove(mth, block, phi);
			return true;
		}
		boolean allSame = phi.getArgsCount() == 1 || isSameArgs(phi);
		if (!allSame) {
			return false;
		}
		return replacePhiWithMove(mth, block, phi, phi.getArg(0));
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
							sVar.setUsedInPhi(null);
						}
					}
					InstructionRemover.remove(mth, block, phiInsn);
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
			argVar.setUsedInPhi(null);
		}
		// try inline
		if (inlinePhiInsn(mth, block, phi)) {
			insns.remove(phiIndex);
		} else {
			assign.setUsedInPhi(null);

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
		for (RegisterArg useArg : new ArrayList<RegisterArg>(useList)) {
			InsnNode useInsn = useArg.getParentInsn();
			if (useInsn == null || useInsn == phi) {
				return false;
			}
			useArg.getSVar().removeUse(useArg);
			RegisterArg inlArg = arg.duplicate();
			if (!useInsn.replaceArg(useArg, inlArg)) {
				return false;
			}
			inlArg.getSVar().use(inlArg);
			inlArg.setName(useArg.getName());
			inlArg.setType(useArg.getType());
		}
		if (block.contains(AType.EXC_HANDLER)) {
			// don't inline into exception handler
			InsnNode assignInsn = arg.getAssignInsn();
			if (assignInsn != null) {
				assignInsn.add(AFlag.DONT_INLINE);
			}
		}
		InstructionRemover.unbindInsn(mth, phi);
		return true;
	}
}
