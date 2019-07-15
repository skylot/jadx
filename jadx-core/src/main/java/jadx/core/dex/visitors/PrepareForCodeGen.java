package jadx.core.dex.visitors;

import java.util.Iterator;
import java.util.List;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.ArithOp;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.instructions.mods.TernaryInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.conditions.IfCondition;
import jadx.core.dex.regions.conditions.IfCondition.Mode;
import jadx.core.dex.visitors.regions.AbstractRegionVisitor;
import jadx.core.dex.visitors.regions.DepthRegionTraversal;
import jadx.core.dex.visitors.regions.variables.ProcessVariables;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.utils.exceptions.JadxException;

/**
 * Prepare instructions for code generation pass,
 * most of this modification breaks register dependencies,
 * so this pass must be just before CodeGen.
 */
@JadxVisitor(
		name = "PrepareForCodeGen",
		desc = "Prepare instructions for code generation pass",
		runAfter = { CodeShrinkVisitor.class, ClassModifier.class, ProcessVariables.class }
)
public class PrepareForCodeGen extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		List<BlockNode> blocks = mth.getBasicBlocks();
		if (blocks == null) {
			return;
		}
		for (BlockNode block : blocks) {
			if (block.contains(AFlag.DONT_GENERATE)) {
				continue;
			}
			removeInstructions(block);
			checkInline(block);
			removeParenthesis(block);
			modifyArith(block);
		}
		commentOutInsnsInConstructor(mth);
	}

	private static void removeInstructions(BlockNode block) {
		Iterator<InsnNode> it = block.getInstructions().iterator();
		while (it.hasNext()) {
			InsnNode insn = it.next();
			switch (insn.getType()) {
				case NOP:
				case MONITOR_ENTER:
				case MONITOR_EXIT:
				case MOVE_EXCEPTION:
					it.remove();
					break;

				case CONSTRUCTOR:
					ConstructorInsn co = (ConstructorInsn) insn;
					if (co.isSelf()) {
						it.remove();
					}
					break;

				case MOVE:
					// remove redundant moves: unused result and same args names (a = a;)
					RegisterArg result = insn.getResult();
					if (result.getSVar().getUseCount() == 0
							&& result.isNameEquals(insn.getArg(0))) {
						it.remove();
					}
					break;

				default:
					break;
			}
		}
	}

	private static void checkInline(BlockNode block) {
		List<InsnNode> list = block.getInstructions();
		for (int i = 0; i < list.size(); i++) {
			InsnNode insn = list.get(i);
			// replace 'move' with inner wrapped instruction
			if (insn.getType() == InsnType.MOVE
					&& insn.getArg(0).isInsnWrap()) {
				InsnNode wrapInsn = ((InsnWrapArg) insn.getArg(0)).getWrapInsn();
				wrapInsn.setResult(insn.getResult());
				wrapInsn.copyAttributesFrom(insn);
				list.set(i, wrapInsn);
			}
		}
	}

	private static void removeParenthesis(BlockNode block) {
		for (InsnNode insn : block.getInstructions()) {
			removeParenthesis(insn);
		}
	}

	/**
	 * Remove parenthesis for wrapped insn in arith '+' or '-'
	 * ('(a + b) +c' => 'a + b + c')
	 */
	private static void removeParenthesis(InsnNode insn) {
		if (insn.getType() == InsnType.ARITH) {
			ArithNode arith = (ArithNode) insn;
			ArithOp op = arith.getOp();
			if (op == ArithOp.ADD || op == ArithOp.MUL || op == ArithOp.AND || op == ArithOp.OR) {
				for (int i = 0; i < 2; i++) {
					InsnArg arg = arith.getArg(i);
					if (arg.isInsnWrap()) {
						InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
						if (wrapInsn.getType() == InsnType.ARITH && ((ArithNode) wrapInsn).getOp() == op) {
							wrapInsn.add(AFlag.DONT_WRAP);
						}
						removeParenthesis(wrapInsn);
					}
				}
			}
		} else {
			if (insn.getType() == InsnType.TERNARY) {
				removeParenthesis(((TernaryInsn) insn).getCondition());
			}
			for (InsnArg arg : insn.getArguments()) {
				if (arg.isInsnWrap()) {
					InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
					removeParenthesis(wrapInsn);
				}
			}
		}
	}

	private static void removeParenthesis(IfCondition cond) {
		Mode mode = cond.getMode();
		for (IfCondition c : cond.getArgs()) {
			if (c.getMode() == mode) {
				c.add(AFlag.DONT_WRAP);
			}
		}
	}

	/**
	 * Replace arithmetic operation with short form
	 * ('a = a + 2' => 'a += 2')
	 */
	private static void modifyArith(BlockNode block) {
		List<InsnNode> list = block.getInstructions();
		for (InsnNode insn : list) {
			if (insn.getType() == InsnType.ARITH
					&& !insn.contains(AFlag.ARITH_ONEARG)
					&& !insn.contains(AFlag.DECLARE_VAR)) {
				RegisterArg res = insn.getResult();
				InsnArg arg = insn.getArg(0);
				boolean replace = false;
				if (res.equals(arg)) {
					replace = true;
				} else if (arg.isRegister()) {
					RegisterArg regArg = (RegisterArg) arg;
					replace = res.sameCodeVar(regArg);
				}
				if (replace) {
					insn.setResult(null);
					insn.add(AFlag.ARITH_ONEARG);
				}
			}
		}
	}

	private void commentOutInsnsInConstructor(MethodNode mth) {
		if (mth.isConstructor()) {
			ConstructorInsn constrInsn = searchConstructorCall(mth);
			if (constrInsn != null && !constrInsn.contains(AFlag.DONT_GENERATE)) {
				DepthRegionTraversal.traverse(mth, new ConstructorRegionVisitor(constrInsn));
			}
		}
	}

	private ConstructorInsn searchConstructorCall(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				InsnType insnType = insn.getType();
				if (insnType == InsnType.CONSTRUCTOR) {
					ConstructorInsn constrInsn = (ConstructorInsn) insn;
					if (constrInsn.isSuper() || constrInsn.isThis()) {
						return constrInsn;
					}
				}
			}
		}
		return null;
	}

	private static final class ConstructorRegionVisitor extends AbstractRegionVisitor {
		private final ConstructorInsn constrInsn;
		private int regionDepth;
		private boolean found;
		private boolean brokenCode;
		private int commentedCount;

		public ConstructorRegionVisitor(ConstructorInsn constrInsn) {
			this.constrInsn = constrInsn;
		}

		@Override
		public boolean enterRegion(MethodNode mth, IRegion region) {
			if (found) {
				return false;
			}
			regionDepth++;
			return true;
		}

		@Override
		public void leaveRegion(MethodNode mth, IRegion region) {
			if (!found) {
				regionDepth--;
				region.add(AFlag.COMMENT_OUT);
				commentedCount++;
			}
		}

		@Override
		public void processBlock(MethodNode mth, IBlock container) {
			if (found) {
				return;
			}
			for (InsnNode insn : container.getInstructions()) {
				if (insn == constrInsn) {
					found = true;
					addMethodMsg(mth);
					break;
				}
				insn.add(AFlag.COMMENT_OUT);
				commentedCount++;
				if (!brokenCode) {
					RegisterArg resArg = insn.getResult();
					if (resArg != null) {
						for (RegisterArg arg : resArg.getSVar().getUseList()) {
							if (arg.getParentInsn() == constrInsn) {
								brokenCode = true;
								break;
							}
						}
					}
				}
			}
		}

		private void addMethodMsg(MethodNode mth) {
			if (commentedCount > 0) {
				String msg = "Illegal instructions before constructor call commented (this can break semantics)";
				if (brokenCode || regionDepth > 1) {
					mth.addWarn(msg);
				} else {
					mth.addComment("JADX WARN: " + msg);
				}
			}
		}
	}
}
