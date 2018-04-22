package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.codegen.TypeGen;
import jadx.core.deobf.NameMapper;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.FieldReplaceAttr;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.ConstClassNode;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.FillArrayNode;
import jadx.core.dex.instructions.FilledNewArrayNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.NewArrayNode;
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
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.InstructionRemover;
import jadx.core.utils.exceptions.JadxRuntimeException;

/**
 * Visitor for modify method instructions
 * (remove, replace, process exception handlers)
 */
@JadxVisitor(
		name = "ModVisitor",
		desc = "Modify method instructions",
		runBefore = CodeShrinker.class
)
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

					case NEW_ARRAY:
						// create array in 'fill-array' instruction
						int next = i + 1;
						if (next < size) {
							InsnNode ni = block.getInstructions().get(next);
							if (ni.getType() == InsnType.FILL_ARRAY) {
								ni.getResult().merge(mth.dex(), insn.getResult());
								ArgType arrType = ((NewArrayNode) insn).getArrayType();
								((FillArrayNode) ni).mergeElementType(mth.dex(), arrType.getArrayElement());
								remover.add(insn);
							}
						}
						break;

					case FILL_ARRAY:
						InsnNode filledArr = makeFilledArrayInsn(mth, (FillArrayNode) insn);
						replaceInsn(block, i, filledArr);
						break;

					case MOVE_EXCEPTION:
						processMoveException(block, insn, remover);
						break;

					case ARITH:
						ArithNode arithNode = (ArithNode) insn;
						if (arithNode.getArgsCount() == 2) {
							InsnArg litArg = arithNode.getArg(1);
							if (litArg.isLiteral()) {
								FieldNode f = parentClass.getConstFieldByLiteralArg((LiteralArg) litArg);
								if (f != null) {
									InsnNode fGet = new IndexInsnNode(InsnType.SGET, f.getFieldInfo(), 0);
									insn.replaceArg(litArg, InsnArg.wrapArg(fGet));
								}
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

	/**
	 * Remove unnecessary instructions
	 */
	private static void removeStep(MethodNode mth, InstructionRemover remover) {
		for (BlockNode block : mth.getBasicBlocks()) {
			remover.setBlock(block);
			for (InsnNode insn : block.getInstructions()) {
				switch (insn.getType()) {
					case NOP:
					case GOTO:
					case NEW_INSTANCE:
						remover.add(insn);
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
		if (!callMth.isConstructor()) {
			return;
		}
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
			return;
		}
		if (co.isNewInstance()) {
			InsnNode newInstInsn = removeAssignChain(instArgAssignInsn, remover, InsnType.NEW_INSTANCE);
			if (newInstInsn != null) {
				RegisterArg instArg = newInstInsn.getResult();
				RegisterArg resultArg = co.getResult();
				if (!resultArg.equals(instArg)) {
					// replace all usages of 'instArg' with result of this constructor instruction
					for (RegisterArg useArg : new ArrayList<>(instArg.getSVar().getUseList())) {
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
			co = replace;
		}
		replaceInsn(block, insnNumber, co);

		processAnonymousConstructor(mth, co);
	}

	private static void processAnonymousConstructor(MethodNode mth, ConstructorInsn co) {
		MethodInfo callMth = co.getCallMth();
		MethodNode callMthNode = mth.dex().resolveMethod(callMth);
		if (callMthNode == null) {
			return;
		}
		ClassNode classNode = callMthNode.getParentClass();
		ClassInfo classInfo = classNode.getClassInfo();
		ClassNode parentClass = mth.getParentClass();
		if (!classInfo.isInner()
				|| !Character.isDigit(classInfo.getShortName().charAt(0))
				|| !parentClass.getInnerClasses().contains(classNode)) {
			return;
		}
		if (!classNode.getAccessFlags().isStatic()
				&& (callMth.getArgsCount() == 0
				|| !callMth.getArgumentsTypes().get(0).equals(parentClass.getClassInfo().getType()))) {
			return;
		}
		// TODO: calculate this constructor and other constructor usage
		Map<InsnArg, FieldNode> argsMap = getArgsToFieldsMapping(callMthNode, co);
		if (argsMap.isEmpty()) {
			return;
		}

		// all checks passed
		classNode.add(AFlag.ANONYMOUS_CLASS);
		callMthNode.add(AFlag.DONT_GENERATE);
		for (Map.Entry<InsnArg, FieldNode> entry : argsMap.entrySet()) {
			FieldNode field = entry.getValue();
			if (field == null) {
				continue;
			}
			InsnArg arg = entry.getKey();
			field.addAttr(new FieldReplaceAttr(arg));
			field.add(AFlag.DONT_GENERATE);
			if (arg.isRegister()) {
				RegisterArg reg = (RegisterArg) arg;
				SSAVar sVar = reg.getSVar();
				if (sVar != null) {
					sVar.add(AFlag.FINAL);
					sVar.add(AFlag.DONT_INLINE);
				}
				reg.add(AFlag.SKIP_ARG);
			}
		}
	}

	private static Map<InsnArg, FieldNode> getArgsToFieldsMapping(MethodNode callMthNode, ConstructorInsn co) {
		Map<InsnArg, FieldNode> map = new LinkedHashMap<>();
		ClassNode parentClass = callMthNode.getParentClass();
		List<RegisterArg> argList = callMthNode.getArguments(false);
		int startArg = parentClass.getAccessFlags().isStatic() ? 0 : 1;
		int argsCount = argList.size();
		for (int i = startArg; i < argsCount; i++) {
			RegisterArg arg = argList.get(i);
			InsnNode useInsn = getParentInsnSkipMove(arg);
			if (useInsn == null) {
				return Collections.emptyMap();
			}
			FieldNode fieldNode = null;
			if (useInsn.getType() == InsnType.IPUT) {
				FieldInfo field = (FieldInfo) ((IndexInsnNode) useInsn).getIndex();
				fieldNode = parentClass.searchField(field);
				if (fieldNode == null || !fieldNode.getAccessFlags().isSynthetic()) {
					return Collections.emptyMap();
				}
			} else if (useInsn.getType() == InsnType.CONSTRUCTOR) {
				ConstructorInsn superConstr = (ConstructorInsn) useInsn;
				if (!superConstr.isSuper()) {
					return Collections.emptyMap();
				}
			} else {
				return Collections.emptyMap();
			}
			map.put(co.getArg(i), fieldNode);
		}
		return map;
	}

	private static InsnNode getParentInsnSkipMove(RegisterArg arg) {
		SSAVar sVar = arg.getSVar();
		if (sVar.getUseCount() != 1) {
			return null;
		}
		RegisterArg useArg = sVar.getUseList().get(0);
		InsnNode parentInsn = useArg.getParentInsn();
		if (parentInsn == null) {
			return null;
		}
		if (parentInsn.getType() == InsnType.MOVE) {
			return getParentInsnSkipMove(parentInsn.getResult());
		}
		return parentInsn;
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

	private static InsnNode makeFilledArrayInsn(MethodNode mth, FillArrayNode insn) {
		ArgType insnArrayType = insn.getResult().getType();
		ArgType insnElementType = insnArrayType.getArrayElement();
		ArgType elType = insn.getElementType();
		if (!elType.isTypeKnown()
				&& insnElementType.isPrimitive()
				&& elType.contains(insnElementType.getPrimitiveType())) {
			elType = insnElementType;
		}
		if (!elType.equals(insnElementType) && !insnArrayType.equals(ArgType.OBJECT)) {
			ErrorsCounter.methodError(mth,
					"Incorrect type for fill-array insn " + InsnUtils.formatOffset(insn.getOffset())
							+ ", element type: " + elType + ", insn element type: " + insnElementType
			);
		}
		if (!elType.isTypeKnown()) {
			LOG.warn("Unknown array element type: {} in mth: {}", elType, mth);
			elType = insnElementType.isTypeKnown() ? insnElementType : elType.selectFirst();
			if (elType == null) {
				throw new JadxRuntimeException("Null array element type");
			}
		}
		insn.mergeElementType(mth.dex(), elType);
		elType = insn.getElementType();

		List<LiteralArg> list = insn.getLiteralArgs();
		InsnNode filledArr = new FilledNewArrayNode(elType, list.size());
		filledArr.setResult(insn.getResult());
		for (LiteralArg arg : list) {
			FieldNode f = mth.getParentClass().getConstFieldByLiteralArg(arg);
			if (f != null) {
				InsnNode fGet = new IndexInsnNode(InsnType.SGET, f.getFieldInfo(), 0);
				filledArr.addArg(InsnArg.wrapArg(fGet));
			} else {
				filledArr.addArg(arg);
			}
		}
		return filledArr;
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

	private static void processMoveException(BlockNode block, InsnNode insn, InstructionRemover remover) {
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
		insn.setSourceLine(prevInsn.getSourceLine());
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
