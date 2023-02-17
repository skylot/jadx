package jadx.core.utils;

import java.util.ArrayList;
import java.util.List;

import jadx.api.ICodeWriter;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.PhiListAttr;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.instructions.mods.TernaryInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.dex.visitors.PrepareForCodeGen;
import jadx.core.dex.visitors.rename.RenameVisitor;
import jadx.core.utils.exceptions.JadxRuntimeException;

/**
 * Check invariants and information consistency for registers and SSA variables
 */
public class DebugChecks {

	public static boolean /* not final! */ checksEnabled = false;

	public static void runChecksAfterVisitor(MethodNode mth, IDexTreeVisitor visitor) {
		Class<? extends IDexTreeVisitor> visitorCls = visitor.getClass();
		if (visitorCls == PrepareForCodeGen.class || visitorCls == RenameVisitor.class) {
			return;
		}
		try {
			checkMethod(mth);
		} catch (Exception e) {
			throw new JadxRuntimeException("Debug check failed after visitor: " + visitorCls.getSimpleName(), e);
		}
	}

	public static void checkMethod(MethodNode mth) {
		List<BlockNode> basicBlocks = mth.getBasicBlocks();
		if (Utils.isEmpty(basicBlocks)) {
			return;
		}
		for (BlockNode block : basicBlocks) {
			for (InsnNode insn : block.getInstructions()) {
				checkInsn(mth, insn);
			}
		}
		checkSSAVars(mth);
		// checkPHI(mth);
	}

	private static void checkInsn(MethodNode mth, InsnNode insn) {
		if (insn.getResult() != null) {
			checkVar(mth, insn, insn.getResult());
		}
		for (InsnArg arg : insn.getArguments()) {
			if (arg instanceof RegisterArg) {
				checkVar(mth, insn, (RegisterArg) arg);
			} else if (arg.isInsnWrap()) {
				InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
				checkInsn(mth, wrapInsn);
			}
		}
		if (insn instanceof TernaryInsn) {
			TernaryInsn ternaryInsn = (TernaryInsn) insn;
			for (RegisterArg arg : ternaryInsn.getCondition().getRegisterArgs()) {
				checkVar(mth, insn, arg);
			}
		}
	}

	private static void checkVar(MethodNode mth, InsnNode insn, RegisterArg reg) {
		checkRegisterArg(mth, reg);

		SSAVar sVar = reg.getSVar();
		if (sVar == null) {
			if (reg.contains(AFlag.DONT_GENERATE) || insn.contains(AFlag.DONT_GENERATE)) {
				return;
			}
			if (Utils.notEmpty(mth.getSVars())) {
				throw new JadxRuntimeException("Null SSA var in " + reg + " at " + insn);
			}
			return;
		}
		if (Utils.indexInListByRef(mth.getSVars(), sVar) == -1) {
			throw new JadxRuntimeException("SSA var not present in method vars list, var: " + sVar + " from insn: " + insn);
		}
		RegisterArg resArg = insn.getResult();
		List<RegisterArg> useList = sVar.getUseList();
		if (resArg == reg) {
			if (sVar.getAssignInsn() != insn) {
				throw new JadxRuntimeException("Incorrect assign in ssa var: " + sVar
						+ ICodeWriter.NL + " expected: " + sVar.getAssignInsn()
						+ ICodeWriter.NL + " got: " + insn);
			}
		} else {
			if (!Utils.containsInListByRef(useList, reg)) {
				throw new JadxRuntimeException("Incorrect use list in ssa var: " + sVar + ", register not listed."
						+ ICodeWriter.NL + " insn: " + insn);
			}
		}
		for (RegisterArg useArg : useList) {
			checkRegisterArg(mth, useArg);
		}
	}

	private static void checkSSAVars(MethodNode mth) {
		for (SSAVar ssaVar : mth.getSVars()) {
			RegisterArg assignArg = ssaVar.getAssign();
			if (assignArg.contains(AFlag.REMOVE)) {
				// ignore removed vars
				continue;
			}
			InsnNode assignInsn = assignArg.getParentInsn();
			if (assignInsn != null) {
				if (insnMissing(mth, assignInsn)) {
					throw new JadxRuntimeException("Insn not found for assign arg in SSAVar: " + ssaVar + ", insn: " + assignInsn);
				}
				RegisterArg resArg = assignInsn.getResult();
				if (resArg == null) {
					throw new JadxRuntimeException("SSA assign insn result missing. SSAVar: " + ssaVar + ", insn: " + assignInsn);
				}
				SSAVar assignVar = resArg.getSVar();
				if (!assignVar.equals(ssaVar)) {
					throw new JadxRuntimeException("Unexpected SSAVar in assign. "
							+ "Expected: " + ssaVar + ", got: " + assignVar + ", insn: " + assignInsn);
				}
			}
			for (RegisterArg arg : ssaVar.getUseList()) {
				InsnNode useInsn = arg.getParentInsn();
				if (useInsn == null) {
					throw new JadxRuntimeException("Parent insn can't be null for arg in use list of SSAVar: " + ssaVar);
				}
				if (insnMissing(mth, useInsn)) {
					throw new JadxRuntimeException("Insn not found for use arg for SSAVar: " + ssaVar + ", insn: " + useInsn);
				}
				int argIndex = useInsn.getArgIndex(arg);
				if (argIndex == -1) {
					throw new JadxRuntimeException("Use arg not found in insn for SSAVar: " + ssaVar + ", insn: " + useInsn);
				}
				InsnArg foundArg = useInsn.getArg(argIndex);
				if (!foundArg.equals(arg)) {
					throw new JadxRuntimeException(
							"Incorrect use arg in insn for SSAVar: " + ssaVar + ", insn: " + useInsn + ", arg: " + foundArg);
				}
			}
		}
	}

	private static boolean insnMissing(MethodNode mth, InsnNode insn) {
		if (insn.contains(AFlag.HIDDEN)) {
			// skip search
			return false;
		}
		BlockNode block = BlockUtils.getBlockByInsn(mth, insn);
		return block == null;
	}

	private static void checkRegisterArg(MethodNode mth, RegisterArg reg) {
		InsnNode parentInsn = reg.getParentInsn();
		if (parentInsn == null) {
			if (reg.contains(AFlag.METHOD_ARGUMENT)) {
				return;
			}
			throw new JadxRuntimeException("Null parentInsn for reg: " + reg);
		}
		if (!parentInsn.contains(AFlag.HIDDEN)) {
			if (parentInsn.getResult() != reg && !parentInsn.containsArg(reg)) {
				throw new JadxRuntimeException("Incorrect parentInsn: " + parentInsn + ", must contains arg: " + reg);
			}
			BlockNode parentInsnBlock = BlockUtils.getBlockByInsn(mth, parentInsn);
			if (parentInsnBlock == null) {
				throw new JadxRuntimeException("Parent insn not found in blocks tree for: " + reg
						+ ICodeWriter.NL + " insn: " + parentInsn);
			}
		}
	}

	private static void checkPHI(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			List<PhiInsn> phis = new ArrayList<>();
			for (InsnNode insn : block.getInstructions()) {
				if (insn.getType() == InsnType.PHI) {
					PhiInsn phi = (PhiInsn) insn;
					phis.add(phi);
					if (phi.getArgsCount() == 0) {
						throw new JadxRuntimeException("No args and binds in PHI");
					}
					for (InsnArg arg : insn.getArguments()) {
						if (arg instanceof RegisterArg) {
							BlockNode b = phi.getBlockByArg((RegisterArg) arg);
							if (b == null) {
								throw new JadxRuntimeException("Predecessor block not found");
							}
						} else {
							throw new JadxRuntimeException("Not register in phi insn");
						}
					}
				}
			}
			PhiListAttr phiListAttr = block.get(AType.PHI_LIST);
			if (phiListAttr == null) {
				if (!phis.isEmpty()) {
					throw new JadxRuntimeException("Missing PHI list attribute");
				}
			} else {
				List<PhiInsn> phiList = phiListAttr.getList();
				if (phiList.isEmpty()) {
					throw new JadxRuntimeException("Empty PHI list attribute");
				}
				if (!phis.containsAll(phiList) || !phiList.containsAll(phis)) {
					throw new JadxRuntimeException("Instructions not match");
				}
			}
		}
		for (SSAVar ssaVar : mth.getSVars()) {
			for (PhiInsn usedInPhi : ssaVar.getUsedInPhi()) {
				boolean found = false;
				for (RegisterArg useArg : ssaVar.getUseList()) {
					InsnNode parentInsn = useArg.getParentInsn();
					if (parentInsn != null && parentInsn == usedInPhi) {
						found = true;
						break;
					}
				}
				if (!found) {
					throw new JadxRuntimeException("Used in phi incorrect");
				}
			}
		}
	}
}
