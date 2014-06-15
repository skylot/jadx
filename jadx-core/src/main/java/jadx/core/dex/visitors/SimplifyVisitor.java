package jadx.core.dex.visitors;

import jadx.core.Consts;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.ArithOp;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.FieldArg;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimplifyVisitor extends AbstractVisitor {

	private static final Logger LOG = LoggerFactory.getLogger(SimplifyVisitor.class);

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		for (BlockNode block : mth.getBasicBlocks()) {
			List<InsnNode> list = block.getInstructions();
			for (int i = 0; i < list.size(); i++) {
				InsnNode modInsn = simplifyInsn(mth, list.get(i));
				if (modInsn != null) {
					list.set(i, modInsn);
				}
			}
		}
	}

	private static InsnNode simplifyInsn(MethodNode mth, InsnNode insn) {
		for (InsnArg arg : insn.getArguments()) {
			if (arg.isInsnWrap()) {
				InsnNode ni = simplifyInsn(mth, ((InsnWrapArg) arg).getWrapInsn());
				if (ni != null) {
					arg.wrapInstruction(ni);
				}
			}
		}
		switch (insn.getType()) {
			case ARITH:
				return simplifyArith(insn);

			case IF:
				simplifyIf((IfNode) insn);
				break;

			case INVOKE:
				return convertInvoke(mth, insn);

			case IPUT:
			case SPUT:
				return convertFieldArith(mth, insn);

			case CHECK_CAST:
				InsnArg castArg = insn.getArg(0);
				ArgType castType = (ArgType) ((IndexInsnNode) insn).getIndex();
				if (!ArgType.isCastNeeded(castArg.getType(), castType)) {
					InsnNode insnNode = new InsnNode(InsnType.MOVE, 1);
					insnNode.setResult(insn.getResult());
					insnNode.addArg(castArg);
					return insnNode;
				}
				break;

			default:
				break;
		}
		return null;
	}

	/**
	 * Simplify 'cmp' instruction in if condition
	 */
	private static void simplifyIf(IfNode insn) {
		InsnArg f = insn.getArg(0);
		if (f.isInsnWrap()) {
			InsnNode wi = ((InsnWrapArg) f).getWrapInsn();
			if (wi.getType() == InsnType.CMP_L || wi.getType() == InsnType.CMP_G) {
				if (insn.getArg(1).isLiteral()
						&& ((LiteralArg) insn.getArg(1)).getLiteral() == 0) {
					insn.changeCondition(insn.getOp(), wi.getArg(0), wi.getArg(1));
				} else {
					LOG.warn("TODO: cmp" + insn);
				}
			}
		}
	}

	private static InsnNode convertInvoke(MethodNode mth, InsnNode insn) {
		MethodInfo callMth = ((InvokeNode) insn).getCallMth();
		if (callMth.getDeclClass().getFullName().equals(Consts.CLASS_STRING_BUILDER)
				&& callMth.getShortId().equals(Consts.MTH_TOSTRING_SIGNATURE)
				&& insn.getArg(0).isInsnWrap()) {
			try {
				List<InsnNode> chain = flattenInsnChain(insn);
				if (chain.size() > 1 && chain.get(0).getType() == InsnType.CONSTRUCTOR) {
					ConstructorInsn constr = (ConstructorInsn) chain.get(0);
					if (constr.getClassType().getFullName().equals(Consts.CLASS_STRING_BUILDER)
							&& constr.getArgsCount() == 0) {
						int len = chain.size();
						InsnNode concatInsn = new InsnNode(InsnType.STR_CONCAT, len - 1);
						for (int i = 1; i < len; i++) {
							concatInsn.addArg(chain.get(i).getArg(1));
						}
						concatInsn.setResult(insn.getResult());
						return concatInsn;
					}
				}
			} catch (Throwable e) {
				LOG.debug("Can't convert string concatenation: {} insn: {}", mth, insn, e);
			}
		}
		return null;
	}

	private static InsnNode simplifyArith(InsnNode insn) {
		ArithNode arith = (ArithNode) insn;
		if (arith.getArgsCount() != 2) {
			return null;
		}
		InsnArg litArg = null;
		InsnArg secondArg = arith.getArg(1);
		if (secondArg.isInsnWrap()) {
			InsnNode wr = ((InsnWrapArg) secondArg).getWrapInsn();
			if (wr.getType() == InsnType.CONST) {
				litArg = wr.getArg(0);
			}
		} else if (secondArg.isLiteral()) {
			litArg = secondArg;
		}
		if (litArg != null) {
			long lit = ((LiteralArg) litArg).getLiteral();
			// fix 'c + (-1)' => 'c - (1)'
			if (arith.getOp() == ArithOp.ADD && lit < 0) {
				return new ArithNode(ArithOp.SUB,
						arith.getResult(), insn.getArg(0),
						InsnArg.lit(-lit, litArg.getType()));
			}
		}
		return null;
	}

	/**
	 * Convert field arith operation to arith instruction
	 * (IPUT = ARITH (IGET, lit) -> ARITH (fieldArg <op>= lit))
	 */
	private static InsnNode convertFieldArith(MethodNode mth, InsnNode insn) {
		InsnArg arg = insn.getArg(0);
		if (!arg.isInsnWrap()) {
			return null;
		}
		InsnNode wrap = ((InsnWrapArg) arg).getWrapInsn();
		InsnType wrapType = wrap.getType();
		if ((wrapType != InsnType.ARITH && wrapType != InsnType.STR_CONCAT)
				|| !wrap.getArg(0).isInsnWrap()) {
			return null;
		}
		InsnNode get = ((InsnWrapArg) wrap.getArg(0)).getWrapInsn();
		InsnType getType = get.getType();
		if (getType != InsnType.IGET && getType != InsnType.SGET) {
			return null;
		}
		FieldInfo field = (FieldInfo) ((IndexInsnNode) insn).getIndex();
		FieldInfo innerField = (FieldInfo) ((IndexInsnNode) get).getIndex();
		if (!field.equals(innerField)) {
			return null;
		}
		try {
			InsnArg reg = null;
			if (getType == InsnType.IGET) {
				reg = get.getArg(0);
			}
			FieldArg fArg = new FieldArg(field, reg);
			if (reg != null) {
				fArg.setType(get.getArg(0).getType());
			}
			if (wrapType == InsnType.ARITH) {
				ArithNode ar = (ArithNode) wrap;
				return new ArithNode(ar.getOp(), fArg, ar.getArg(1));
			} else {
				int argsCount = wrap.getArgsCount();
				InsnNode concat = new InsnNode(InsnType.STR_CONCAT, argsCount - 1);
				for (int i = 1; i < argsCount; i++) {
					concat.addArg(wrap.getArg(i));
				}
				return new ArithNode(ArithOp.ADD, fArg, InsnArg.wrapArg(concat));
			}
		} catch (Throwable e) {
			LOG.debug("Can't convert field arith insn: {}, mth: {}", insn, mth, e);
		}
		return null;
	}

	private static List<InsnNode> flattenInsnChain(InsnNode insn) {
		List<InsnNode> chain = new ArrayList<InsnNode>();
		InsnArg i = insn.getArg(0);
		while (i.isInsnWrap()) {
			InsnNode wrapInsn = ((InsnWrapArg) i).getWrapInsn();
			chain.add(wrapInsn);
			if (wrapInsn.getArgsCount() == 0) {
				break;
			}

			i = wrapInsn.getArg(0);
		}
		Collections.reverse(chain);
		return chain;
	}
}
