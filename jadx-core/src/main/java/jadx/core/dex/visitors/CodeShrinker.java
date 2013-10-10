package jadx.core.dex.visitors;

import jadx.core.Consts;
import jadx.core.dex.attributes.AttributeFlag;
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
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeShrinker extends AbstractVisitor {

	private static final Logger LOG = LoggerFactory.getLogger(CodeShrinker.class);

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode() || mth.getAttributes().contains(AttributeFlag.DONT_SHRINK))
			return;

		shrink(mth);
		prettify(mth);
	}

	private static void shrink(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			List<InsnNode> insnList = block.getInstructions();
			InstructionRemover remover = new InstructionRemover(insnList);
			for (InsnNode insn : insnList) {
				// wrap instructions
				RegisterArg result = insn.getResult();
				if (result != null) {
					List<InsnArg> useList = result.getTypedVar().getUseList();
					if (useList.size() == 1) {
						// variable is used only in this instruction
						// TODO not correct sometimes :(
						remover.add(insn);
					} else if (useList.size() == 2) {
						InsnArg useInsnArg = selectOther(useList, result);
						InsnNode useInsn = useInsnArg.getParentInsn();
						if (useInsn == null) {
							LOG.debug("parent insn null: {}, mth: {}", insn, mth);
						} else if (useInsn != insn) {
							boolean wrap = false;
							// TODO
							if (false && result.getTypedVar().getName() != null) {
								// don't wrap if result variable has name from debug info
								wrap = false;
							} else if (BlockUtils.blockContains(block, useInsn)) {
								// TODO don't reorder methods invocations
								// wrap insn from current block
								wrap = true;
							} else {
								// TODO implement rules for shrink insn from different blocks
								BlockNode useBlock = BlockUtils.getBlockByInsn(mth, useInsn);
								if (useBlock != null
										&& (useBlock.getPredecessors().contains(block)
										|| insn.getType() == InsnType.MOVE_EXCEPTION)) {
									wrap = true;
								}
							}
							if (wrap) {
//								if (useInsn.getType() == InsnType.MOVE) {
//									// TODO
//									// remover.add(useInsn);
//								} else {
								useInsnArg.wrapInstruction(insn);
								remover.add(insn);
//								}
							}
						}
					}
				}
			}
			remover.perform();
		}
	}

	private static void prettify(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			List<InsnNode> list = block.getInstructions();
			for (int i = 0; i < list.size(); i++) {
				InsnNode modInsn = pretifyInsn(mth, list.get(i));
				if (modInsn != null) {
					list.set(i, modInsn);
				}
			}
		}
	}

	private static InsnNode pretifyInsn(MethodNode mth, InsnNode insn) {
		for (InsnArg arg : insn.getArguments()) {
			if (arg.isInsnWrap()) {
				InsnNode ni = pretifyInsn(mth, ((InsnWrapArg) arg).getWrapInsn());
				if (ni != null)
					arg.wrapInstruction(ni);
			}
		}
		switch (insn.getType()) {
			case ARITH:
				ArithNode arith = (ArithNode) insn;
				if (arith.getArgsCount() == 2) {
					InsnArg litArg = null;

					if (arith.getArg(1).isInsnWrap()) {
						InsnNode wr = ((InsnWrapArg) arith.getArg(1)).getWrapInsn();
						if (wr.getType() == InsnType.CONST)
							litArg = wr.getArg(0);
					} else if (arith.getArg(1).isLiteral()) {
						litArg = arith.getArg(1);
					}

					if (litArg != null) {
						long lit = ((LiteralArg) litArg).getLiteral();
						boolean invert = false;

						if (arith.getOp() == ArithOp.ADD && lit < 0)
							invert = true;

						// fix 'c + (-1)' => 'c - (1)'
						if (invert) {
							return new ArithNode(ArithOp.SUB,
									arith.getResult(), insn.getArg(0),
									InsnArg.lit(-lit, litArg.getType()));
						}
					}
				}
				break;

			case IF:
				// simplify 'cmp' instruction in if condition
				IfNode ifb = (IfNode) insn;
				InsnArg f = ifb.getArg(0);
				if (f.isInsnWrap()) {
					InsnNode wi = ((InsnWrapArg) f).getWrapInsn();
					if (wi.getType() == InsnType.CMP_L || wi.getType() == InsnType.CMP_G) {
						if (ifb.isZeroCmp()
								|| ((LiteralArg) ifb.getArg(1)).getLiteral() == 0) {
							ifb.changeCondition(wi.getArg(0), wi.getArg(1), ifb.getOp());
						} else {
							LOG.warn("TODO: cmp" + ifb);
						}
					}
				}
				break;

			case INVOKE:
				MethodInfo callMth = ((InvokeNode) insn).getCallMth();
				if (callMth.getDeclClass().getFullName().equals(Consts.CLASS_STRING_BUILDER)
						&& callMth.getShortId().equals("toString()")
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
				break;

			case IPUT:
			case SPUT:
				// convert field arith operation to arith instruction
				// (IPUT = ARITH (IGET, lit) -> ARITH (fieldArg <op>= lit))
				InsnArg arg = insn.getArg(0);
				if (arg.isInsnWrap()) {
					InsnNode wrap = ((InsnWrapArg) arg).getWrapInsn();
					InsnType wrapType = wrap.getType();
					if ((wrapType == InsnType.ARITH || wrapType == InsnType.STR_CONCAT) && wrap.getArg(0).isInsnWrap()) {
						InsnNode get = ((InsnWrapArg) wrap.getArg(0)).getWrapInsn();
						InsnType getType = get.getType();
						if (getType == InsnType.IGET || getType == InsnType.SGET) {
							FieldInfo field = (FieldInfo) ((IndexInsnNode) insn).getIndex();
							FieldInfo innerField = (FieldInfo) ((IndexInsnNode) get).getIndex();
							if (field.equals(innerField)) {
								try {
									RegisterArg reg = null;
									if (getType == InsnType.IGET) {
										reg = ((RegisterArg) get.getArg(0));
									}
									RegisterArg fArg = new FieldArg(field, reg != null ? reg.getRegNum() : -1);
									if (reg != null) {
										fArg.replaceTypedVar(get.getArg(0));
									}
									if (wrapType == InsnType.ARITH) {
										ArithNode ar = (ArithNode) wrap;
										return new ArithNode(ar.getOp(), fArg, fArg, ar.getArg(1));
									} else {
										int argsCount = wrap.getArgsCount();
										InsnNode concat = new InsnNode(InsnType.STR_CONCAT, argsCount - 1);
										for (int i = 1; i < argsCount; i++) {
											concat.addArg(wrap.getArg(i));
										}
										return new ArithNode(ArithOp.ADD, fArg, fArg, InsnArg.wrap(concat));
									}
								} catch (Throwable e) {
									LOG.debug("Can't convert field arith insn: {}, mth: {}", insn, mth, e);
								}
							}
						}
					}
				}
				break;

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

	private static List<InsnNode> flattenInsnChain(InsnNode insn) {
		List<InsnNode> chain = new ArrayList<InsnNode>();
		InsnArg i = insn.getArg(0);
		while (i.isInsnWrap()) {
			InsnNode wrapInsn = ((InsnWrapArg) i).getWrapInsn();
			chain.add(wrapInsn);
			if (wrapInsn.getArgsCount() == 0)
				break;

			i = wrapInsn.getArg(0);
		}
		Collections.reverse(chain);
		return chain;
	}

	public static InsnArg inlineArgument(MethodNode mth, RegisterArg arg) {
		InsnNode assignInsn = arg.getAssignInsn();
		if (assignInsn == null) {
			return null;
		}
		// recursively wrap all instructions
		List<RegisterArg> list = new ArrayList<RegisterArg>();
		List<RegisterArg> args = mth.getArguments(false);
		int i = 0;
		do {
			list.clear();
			assignInsn.getRegisterArgs(list);
			for (RegisterArg rarg : list) {
				InsnNode ai = rarg.getAssignInsn();
				if (ai != assignInsn && ai != null && ai != rarg.getParentInsn()) {
					rarg.wrapInstruction(ai);
				}
			}
			// remove method args
			if (list.size() != 0 && args.size() != 0) {
				list.removeAll(args);
			}
			i++;
			if (i > 1000) {
				throw new JadxRuntimeException("Can't inline arguments for: " + arg + " insn: " + assignInsn);
			}
		} while (!list.isEmpty());

		return arg.wrapInstruction(assignInsn);
	}

	private static InsnArg selectOther(List<InsnArg> list, RegisterArg insn) {
		InsnArg first = list.get(0);
		if (first == insn)
			return list.get(1);
		else
			return first;
	}
}
