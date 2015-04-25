package jadx.core.dex.visitors;

import jadx.core.Consts;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.ArithOp;
import jadx.core.dex.instructions.ConstStringNode;
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
import jadx.core.dex.instructions.mods.TernaryInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.conditions.IfCondition;

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
			case TERNARY:
				simplifyTernary((TernaryInsn) insn);
				break;

			case INVOKE:
				return convertInvoke(mth, insn);

			case IPUT:
			case SPUT:
				return convertFieldArith(mth, insn);

			case CHECK_CAST:
				return processCast(mth, insn);

			case MOVE:
				InsnArg firstArg = insn.getArg(0);
				if (firstArg.isLiteral()) {
					InsnNode constInsn = new InsnNode(InsnType.CONST, 1);
					constInsn.setResult(insn.getResult());
					constInsn.addArg(firstArg);
					constInsn.copyAttributesFrom(insn);
					return constInsn;
				}
				break;

			default:
				break;
		}
		return null;
	}

	private static InsnNode processCast(MethodNode mth, InsnNode insn) {
		InsnArg castArg = insn.getArg(0);
		ArgType argType = castArg.getType();

		// Don't removes CHECK_CAST for wrapped INVOKE if invoked method returns different type
		if (castArg.isInsnWrap()) {
			InsnNode wrapInsn = ((InsnWrapArg) castArg).getWrapInsn();
			if (wrapInsn.getType() == InsnType.INVOKE) {
				argType = ((InvokeNode) wrapInsn).getCallMth().getReturnType();
			}
		}
		ArgType castToType = (ArgType) ((IndexInsnNode) insn).getIndex();
		if (ArgType.isCastNeeded(mth.dex(), argType, castToType)) {
			return null;
		}
		InsnNode insnNode = new InsnNode(InsnType.MOVE, 1);
		insnNode.setOffset(insn.getOffset());
		insnNode.setResult(insn.getResult());
		insnNode.addArg(castArg);
		return insnNode;
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
					LOG.warn("TODO: cmp {}", insn);
				}
			}
		}
	}

	/**
	 * Simplify condition in ternary operation
	 */
	private static void simplifyTernary(TernaryInsn insn) {
		IfCondition condition = insn.getCondition();
		if (condition.isCompare()) {
			simplifyIf(condition.getCompare().getInsn());
		} else {
			insn.simplifyCondition();
		}
	}

	private static InsnNode convertInvoke(MethodNode mth, InsnNode insn) {
		MethodInfo callMth = ((InvokeNode) insn).getCallMth();

		// If this is a 'new StringBuilder(xxx).append(yyy).append(zzz).toString(),
		// convert it to STRING_CONCAT pseudo instruction.
		if (callMth.getDeclClass().getFullName().equals(Consts.CLASS_STRING_BUILDER)
				&& callMth.getShortId().equals(Consts.MTH_TOSTRING_SIGNATURE)
				&& insn.getArg(0).isInsnWrap()) {
			try {
				List<InsnNode> chain = flattenInsnChain(insn);
				int constrIndex = -1;  //RAF
				// Case where new StringBuilder() is called with NO args (the entire
				// string is created using .append() calls:
				if (chain.size() > 1 && chain.get(0).getType() == InsnType.CONSTRUCTOR) {
					constrIndex = 0;
				} else if (chain.size() > 2 && chain.get(1).getType() == InsnType.CONSTRUCTOR) {
					//RAF Case where the first string element is String arg to the
					// new StringBuilder("xxx") constructor
					constrIndex = 1;
				} else if (chain.size() > 3 && chain.get(2).getType() == InsnType.CONSTRUCTOR) {
					//RAF Case where the first string element is String.valueOf() arg
					// to the new StringBuilder(String.valueOf(zzz)) constructor
					constrIndex = 2;
				}

				if (constrIndex != -1) {  // If we found a CONSTRUCTOR, is it a StringBuilder?
					ConstructorInsn constr = (ConstructorInsn) chain.get(constrIndex);
					if (constr.getClassType().getFullName().equals(Consts.CLASS_STRING_BUILDER)) {
						int len = chain.size(), argInd = 1;
						InsnNode concatInsn = new InsnNode(InsnType.STR_CONCAT, len - 1);
						InsnNode argInsn;
						if (constrIndex > 0) {  // There was an arg to the StringBuilder constr
							InsnWrapArg iwa;
							if (constrIndex == 2
									&& (argInsn = chain.get(1)).getType() == InsnType.INVOKE
									&& ((InvokeNode) argInsn).getCallMth().getName().compareTo("valueOf") == 0) {
								// The argument of new StringBuilder() is a String.valueOf(chainElement0)
								iwa = (InsnWrapArg) argInsn.getArg(0);
								argInd = 3;  // Cause for loop below to skip to after the constructor
							} else {
								InsnNode firstNode = chain.get(0);
								if (firstNode instanceof ConstStringNode) {
									ConstStringNode csn = (ConstStringNode) firstNode;
									iwa = new InsnWrapArg(csn);
									argInd = 2;  // Cause for loop below to skip to after the constructor
								} else {
									return null;
								}
							}
							concatInsn.addArg(iwa);
						}

						for (; argInd < len; argInd++) {  // Add the .append(xxx) arg string to concat
							concatInsn.addArg(chain.get(argInd).getArg(1));
						}
						concatInsn.setResult(insn.getResult());
						return concatInsn;
					} // end of if constructor is for StringBuilder
				} // end of if we found a constructor early in the chain

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
		if (wrapType != InsnType.ARITH && wrapType != InsnType.STR_CONCAT
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
				InsnArg putReg = insn.getArg(1);
				if (!reg.equals(putReg)) {
					return null;
				}
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
		} catch (Exception e) {
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
