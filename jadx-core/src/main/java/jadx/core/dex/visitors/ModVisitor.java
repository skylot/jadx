package jadx.core.dex.visitors;

import jadx.core.codegen.TypeGen;
import jadx.core.deobf.NameMapper;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.ConstClassNode;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.FillArrayNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.SwitchNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.NamedArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.utils.InstructionRemover;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visitor for modify method instructions
 * (remove, replace, process exception handlers)
 */
public class ModVisitor extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(ModVisitor.class);

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}

		InstructionRemover remover = new InstructionRemover(mth);
		replaceStep(mth, remover);
		removeStep(mth, remover);

		checkArgsNames(mth);
	}

	private static void replaceStep(MethodNode mth, InstructionRemover remover) {
		ClassNode parentClass = mth.getParentClass();
		for (BlockNode block : mth.getBasicBlocks()) {
			remover.setBlock(block);
			int size = block.getInstructions().size();
			for (int i = 0; i < size; i++) {
				InsnNode insn = block.getInstructions().get(i);
				switch (insn.getType()) {
					case INVOKE:
						processInvoke(mth, block, i, remover);
						break;

					case CONST:
					case CONST_STR:
					case CONST_CLASS: {
						FieldNode f;
						if (insn.getType() == InsnType.CONST_STR) {
							String s = ((ConstStringNode) insn).getString();
							f = parentClass.getConstField(s);
						} else if (insn.getType() == InsnType.CONST_CLASS) {
							ArgType t = ((ConstClassNode) insn).getClsType();
							f = parentClass.getConstField(t);
						} else {
							f = parentClass.getConstFieldByLiteralArg((LiteralArg) insn.getArg(0));
						}
						if (f != null) {
							InsnNode inode = new IndexInsnNode(InsnType.SGET, f.getFieldInfo(), 0);
							inode.setResult(insn.getResult());
							replaceInsn(block, i, inode);
						}
						break;
					}

					case SWITCH:
						SwitchNode sn = (SwitchNode) insn;
						for (int k = 0; k < sn.getCasesCount(); k++) {
							FieldNode f = parentClass.getConstField(sn.getKeys()[k]);
							if (f != null) {
								sn.getKeys()[k] = f;
							}
						}
						break;

					case RETURN:
						if (insn.getArgsCount() > 0 && insn.getArg(0).isLiteral()) {
							LiteralArg arg = (LiteralArg) insn.getArg(0);
							FieldNode f = parentClass.getConstFieldByLiteralArg(arg);
							if (f != null) {
								arg.wrapInstruction(new IndexInsnNode(InsnType.SGET, f.getFieldInfo(), 0));
							}
						}
						break;

					case MOVE_EXCEPTION:
						processMoveException(mth, block, insn, remover);
						break;

					default:
						break;
				}
			}
			remover.perform();
		}
	}

	private static void processInvoke(MethodNode mth, BlockNode block, int insnNumber, InstructionRemover remover) {
		ClassNode parentClass = mth.getParentClass();
		InsnNode insn = block.getInstructions().get(insnNumber);
		InvokeNode inv = (InvokeNode) insn;
		MethodInfo callMth = inv.getCallMth();
		if (callMth.isConstructor()) {
			InsnNode instArgAssignInsn = ((RegisterArg) inv.getArg(0)).getAssignInsn();
			ConstructorInsn co = new ConstructorInsn(mth, inv);
			boolean remove = false;
			if (co.isSuper() && (co.getArgsCount() == 0 || parentClass.isEnum())) {
				remove = true;
			} else if (co.isThis() && co.getArgsCount() == 0) {
				MethodNode defCo = parentClass.searchMethodByName(callMth.getShortId());
				if (defCo == null || defCo.isNoCode()) {
					// default constructor not implemented
					remove = true;
				}
			}
			// remove super() call in instance initializer
			if (parentClass.isAnonymous() && mth.isDefaultConstructor() && co.isSuper()) {
				remove = true;
			}
			if (remove) {
				remover.add(insn);
			} else {
				replaceInsn(block, insnNumber, co);
				if (co.isNewInstance()) {
					InsnNode newInstInsn = removeAssignChain(instArgAssignInsn, remover, InsnType.NEW_INSTANCE);
					if (newInstInsn != null) {
						RegisterArg instArg = newInstInsn.getResult();
						RegisterArg resultArg = co.getResult();
						if (!resultArg.equals(instArg)) {
							// replace all usages of 'instArg' with result of this constructor instruction
							for (RegisterArg useArg : new ArrayList<RegisterArg>(instArg.getSVar().getUseList())) {
								RegisterArg dup = resultArg.duplicate();
								InsnNode parentInsn = useArg.getParentInsn();
								parentInsn.replaceArg(useArg, dup);
								dup.setParentInsn(parentInsn);
								resultArg.getSVar().use(dup);
							}
						}
					}
				}
				ConstructorInsn replace = processConstructor(mth, co);
				if (replace != null) {
					replaceInsn(block, insnNumber, replace);
				}
			}
		} else if (inv.getArgsCount() > 0) {
			for (int j = 0; j < inv.getArgsCount(); j++) {
				InsnArg arg = inv.getArg(j);
				if (arg.isLiteral()) {
					FieldNode f = parentClass.getConstFieldByLiteralArg((LiteralArg) arg);
					if (f != null) {
						arg.wrapInstruction(new IndexInsnNode(InsnType.SGET, f.getFieldInfo(), 0));
					}
				}
			}
		}
	}

	/**
	 * Replace call of synthetic constructor
	 */
	private static ConstructorInsn processConstructor(MethodNode mth, ConstructorInsn co) {
		MethodNode callMth = mth.dex().resolveMethod(co.getCallMth());
		if (callMth == null
				|| !callMth.getAccessFlags().isSynthetic()
				|| !allArgsNull(co)) {
			return null;
		}
		ClassNode classNode = mth.dex().resolveClass(callMth.getParentClass().getClassInfo());
		if (classNode == null) {
			return null;
		}
		boolean passThis = co.getArgsCount() >= 1 && co.getArg(0).isThis();
		String ctrId = "<init>(" + (passThis ? TypeGen.signature(co.getArg(0).getType()) : "") + ")V";
		MethodNode defCtr = classNode.searchMethodByName(ctrId);
		if (defCtr == null) {
			return null;
		}
		ConstructorInsn newInsn = new ConstructorInsn(defCtr.getMethodInfo(), co.getCallType(), co.getInstanceArg());
		newInsn.setResult(co.getResult());
		return newInsn;
	}

	private static boolean allArgsNull(InsnNode insn) {
		for (InsnArg insnArg : insn.getArguments()) {
			if (insnArg.isLiteral()) {
				LiteralArg lit = (LiteralArg) insnArg;
				if (lit.getLiteral() != 0) {
					return false;
				}
			} else if (!insnArg.isThis()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Remove instructions on 'move' chain until instruction with type 'insnType'
	 */
	private static InsnNode removeAssignChain(InsnNode insn, InstructionRemover remover, InsnType insnType) {
		if (insn == null) {
			return null;
		}
		remover.add(insn);
		InsnType type = insn.getType();
		if (type == insnType) {
			return insn;
		}
		if (type == InsnType.MOVE) {
			RegisterArg arg = (RegisterArg) insn.getArg(0);
			return removeAssignChain(arg.getAssignInsn(), remover, insnType);
		}
		return null;
	}

	/**
	 * Remove unnecessary instructions
	 */
	private static void removeStep(MethodNode mth, InstructionRemover remover) {
		for (BlockNode block : mth.getBasicBlocks()) {
			remover.setBlock(block);
			int size = block.getInstructions().size();
			for (int i = 0; i < size; i++) {
				InsnNode insn = block.getInstructions().get(i);
				switch (insn.getType()) {
					case NOP:
					case GOTO:
					case NEW_INSTANCE:
						remover.add(insn);
						break;

					case NEW_ARRAY:
						// create array in 'fill-array' instruction
						int next = i + 1;
						if (next < size) {
							InsnNode ni = block.getInstructions().get(next);
							if (ni.getType() == InsnType.FILL_ARRAY) {
								ni.getResult().merge(insn.getResult());
								((FillArrayNode) ni).mergeElementType(insn.getResult().getType().getArrayElement());
								remover.add(insn);
							}
						}
						break;

					case RETURN:
						if (insn.getArgsCount() == 0) {
							if (mth.getBasicBlocks().size() == 1 && i == size - 1) {
								remover.add(insn);
							} else if (mth.getMethodInfo().isClassInit()) {
								remover.add(insn);
							}
						}
						break;

					default:
						break;
				}
			}
			remover.perform();
		}
	}

	private static void processMoveException(MethodNode mth, BlockNode block, InsnNode insn,
			InstructionRemover remover) {
		ExcHandlerAttr excHandlerAttr = block.get(AType.EXC_HANDLER);
		if (excHandlerAttr == null) {
			return;
		}
		ExceptionHandler excHandler = excHandlerAttr.getHandler();

		// result arg used both in this insn and exception handler,
		RegisterArg resArg = insn.getResult();
		ArgType type = excHandler.isCatchAll() ? ArgType.THROWABLE : excHandler.getCatchType().getType();
		String name = excHandler.isCatchAll() ? "th" : "e";
		if (resArg.getName() == null) {
			resArg.setName(name);
		}
		SSAVar sVar = insn.getResult().getSVar();
		if (sVar.getUseCount() == 0) {
			excHandler.setArg(new NamedArg(name, type));
			remover.add(insn);
		} else if (sVar.isUsedInPhi()) {
			// exception var moved to external variable => replace with 'move' insn
			InsnNode moveInsn = new InsnNode(InsnType.MOVE, 1);
			moveInsn.setResult(insn.getResult());
			NamedArg namedArg = new NamedArg(name, type);
			moveInsn.addArg(namedArg);
			excHandler.setArg(namedArg);
			replaceInsn(block, 0, moveInsn);
		}
	}

	/**
	 * Replace insn by index i in block,
	 * for proper copy attributes, assume attributes are not overlap
	 */
	private static void replaceInsn(BlockNode block, int i, InsnNode insn) {
		InsnNode prevInsn = block.getInstructions().get(i);
		insn.copyAttributesFrom(prevInsn);
		block.getInstructions().set(i, insn);
	}

	private static void checkArgsNames(MethodNode mth) {
		for (RegisterArg arg : mth.getArguments(false)) {
			String name = arg.getName();
			if (name != null && NameMapper.isReserved(name)) {
				name = name + "_";
				arg.getSVar().setName(name);
			}
		}
	}
}
