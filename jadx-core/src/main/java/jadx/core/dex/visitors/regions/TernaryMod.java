package jadx.core.dex.visitors.regions;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.mods.TernaryInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.conditions.IfRegion;
import jadx.core.dex.visitors.CodeShrinker;
import jadx.core.utils.InsnList;

import java.util.HashMap;
import java.util.Map;

public class TernaryMod {

	private TernaryMod() {
	}

	static boolean makeTernaryInsn(MethodNode mth, IfRegion ifRegion) {
		if (ifRegion.contains(AFlag.ELSE_IF_CHAIN)) {
			return false;
		}
		IContainer thenRegion = ifRegion.getThenRegion();
		IContainer elseRegion = ifRegion.getElseRegion();
		if (thenRegion == null || elseRegion == null) {
			return false;
		}
		BlockNode tb = getTernaryInsnBlock(thenRegion);
		BlockNode eb = getTernaryInsnBlock(elseRegion);
		if (tb == null || eb == null) {
			return false;
		}
		BlockNode header = ifRegion.getHeader();
		InsnNode t = tb.getInstructions().get(0);
		InsnNode e = eb.getInstructions().get(0);

		if (t.getSourceLine() != e.getSourceLine()) {
			if (t.getSourceLine() != 0 && e.getSourceLine() != 0) {
				// sometimes source lines incorrect
				if (!checkLineStats(t, e)) {
					return false;
				}
			} else {
				// no debug info
				if (containsTernary(t) || containsTernary(e)) {
					// don't make nested ternary by default
					// TODO: add addition checks
					return false;
				}
			}
		}

		if (t.getResult() != null && e.getResult() != null) {
			PhiInsn phi = t.getResult().getSVar().getUsedInPhi();
			if (phi == null || !t.getResult().equalRegisterAndType(e.getResult())) {
				return false;
			}
			if (!ifRegion.getParent().replaceSubBlock(ifRegion, header)) {
				return false;
			}
			InsnList.remove(tb, t);
			InsnList.remove(eb, e);

			RegisterArg resArg;
			if (phi.getArgsCount() == 2) {
				resArg = phi.getResult();
			} else {
				resArg = t.getResult();
				phi.removeArg(e.getResult());
			}
			TernaryInsn ternInsn = new TernaryInsn(ifRegion.getCondition(),
					resArg, InsnArg.wrapArg(t), InsnArg.wrapArg(e));
			ternInsn.setSourceLine(t.getSourceLine());

			// remove 'if' instruction
			header.getInstructions().clear();
			header.getInstructions().add(ternInsn);

			// shrink method again
			CodeShrinker.shrinkMethod(mth);
			return true;
		}

		if (!mth.getReturnType().equals(ArgType.VOID)
				&& t.getType() == InsnType.RETURN && e.getType() == InsnType.RETURN) {

			if (!ifRegion.getParent().replaceSubBlock(ifRegion, header)) {
				return false;
			}
			InsnList.remove(tb, t);
			InsnList.remove(eb, e);
			tb.remove(AFlag.RETURN);
			eb.remove(AFlag.RETURN);

			TernaryInsn ternInsn = new TernaryInsn(ifRegion.getCondition(), null, t.getArg(0), e.getArg(0));
			ternInsn.setSourceLine(t.getSourceLine());
			InsnNode retInsn = new InsnNode(InsnType.RETURN, 1);
			retInsn.addArg(InsnArg.wrapArg(ternInsn));

			header.getInstructions().clear();
			header.getInstructions().add(retInsn);
			header.add(AFlag.RETURN);

			CodeShrinker.shrinkMethod(mth);
			return true;
		}
		return false;
	}

	private static BlockNode getTernaryInsnBlock(IContainer thenRegion) {
		if (thenRegion instanceof Region) {
			Region r = (Region) thenRegion;
			if (r.getSubBlocks().size() == 1) {
				IContainer container = r.getSubBlocks().get(0);
				if (container instanceof BlockNode) {
					BlockNode block = (BlockNode) container;
					if (block.getInstructions().size() == 1) {
						return block;
					}
				}
			}
		}
		return null;
	}

	private static boolean containsTernary(InsnNode insn) {
		if (insn.getType() == InsnType.TERNARY) {
			return true;
		}
		for (int i = 0; i < insn.getArgsCount(); i++) {
			InsnArg arg = insn.getArg(i);
			if (arg.isInsnWrap()) {
				InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
				if (containsTernary(wrapInsn)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Return 'true' if there are several args with same source lines
	 */
	private static boolean checkLineStats(InsnNode t, InsnNode e) {
		if (t.getResult() == null || e.getResult() == null) {
			return false;
		}
		PhiInsn tPhi = t.getResult().getSVar().getUsedInPhi();
		PhiInsn ePhi = e.getResult().getSVar().getUsedInPhi();
		if (tPhi == null || ePhi == null || tPhi != ePhi) {
			return false;
		}
		Map<Integer, Integer> map = new HashMap<Integer, Integer>(tPhi.getArgsCount());
		for (InsnArg arg : tPhi.getArguments()) {
			if (!arg.isRegister()) {
				continue;
			}
			InsnNode assignInsn = ((RegisterArg) arg).getAssignInsn();
			if (assignInsn == null) {
				continue;
			}
			int sourceLine = assignInsn.getSourceLine();
			if (sourceLine != 0) {
				Integer count = map.get(sourceLine);
				if (count != null) {
					map.put(sourceLine, count + 1);
				} else {
					map.put(sourceLine, 1);
				}
			}
		}
		for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
			if (entry.getValue() >= 2) {
				return true;
			}
		}
		return false;
	}
}
