package jadx.core.dex.visitors;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.FieldReplaceAttr;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.ConstClassNode;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.FillArrayNode;
import jadx.core.dex.instructions.FilledNewArrayNode;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.IfOp;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.NewArrayNode;
import jadx.core.dex.instructions.SwitchNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.NamedArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.instructions.mods.TernaryInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.conditions.IfCondition;
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.InsnRemover;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.utils.BlockUtils.replaceInsn;

/**
 * Visitor for modify method instructions
 * (remove, replace, process exception handlers)
 */
@JadxVisitor(
		name = "ModVisitor",
		desc = "Modify method instructions",
		runBefore = CodeShrinkVisitor.class
)
public class ModVisitor extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(ModVisitor.class);

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}

		InsnRemover remover = new InsnRemover(mth);
		replaceStep(mth, remover);
		removeStep(mth, remover);
	}

	private static void replaceStep(MethodNode mth, InsnRemover remover) {
		ClassNode parentClass = mth.getParentClass();
		for (BlockNode block : mth.getBasicBlocks()) {
			remover.setBlock(block);
			int size = block.getInstructions().size();
			for (int i = 0; i < size; i++) {
				InsnNode insn = block.getInstructions().get(i);
				switch (insn.getType()) {
					case CONSTRUCTOR:
						processAnonymousConstructor(mth, ((ConstructorInsn) insn));
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
						// replace with filled array if 'fill-array' is next instruction
						int next = i + 1;
						if (next < size) {
							InsnNode ni = block.getInstructions().get(next);
							if (ni.getType() == InsnType.FILL_ARRAY) {
								InsnNode filledArr = makeFilledArrayInsn(mth, (NewArrayNode) insn, (FillArrayNode) ni);
								if (filledArr != null) {
									replaceInsn(block, i, filledArr);
									remover.addAndUnbind(ni);
								}
							}
						}
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

					case CHECK_CAST:
						InsnArg castArg = insn.getArg(0);
						ArgType castType = (ArgType) ((IndexInsnNode) insn).getIndex();
						if (!ArgType.isCastNeeded(mth.dex(), castArg.getType(), castType)
								|| isCastDuplicate((IndexInsnNode) insn)) {
							InsnNode insnNode = new InsnNode(InsnType.MOVE, 1);
							insnNode.setResult(insn.getResult());
							insnNode.addArg(castArg);
							replaceInsn(block, i, insnNode);
						}
						break;

					case CAST:
						// replace boolean to (byte/char/short/long/double/float) cast with ternary
						if (insn.getArg(0).getType() == ArgType.BOOLEAN) {
							ArgType type = insn.getResult().getType();
							if (type.isPrimitive()) {
								IfNode ifNode = new IfNode(IfOp.EQ, -1, insn.getArg(0), LiteralArg.TRUE);
								IfCondition condition = IfCondition.fromIfNode(ifNode);
								InsnArg zero = new LiteralArg(0, type);
								InsnArg one = new LiteralArg(
										type == ArgType.DOUBLE ? Double.doubleToLongBits(1)
												: type == ArgType.FLOAT ? Float.floatToIntBits(1) : 1,
										type);
								TernaryInsn ternary = new TernaryInsn(condition, insn.getResult(), one, zero);
								replaceInsn(block, i, ternary);
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

	private static boolean isCastDuplicate(IndexInsnNode castInsn) {
		InsnArg arg = castInsn.getArg(0);
		if (arg.isRegister()) {
			SSAVar sVar = ((RegisterArg) arg).getSVar();
			if (sVar != null && sVar.getUseCount() == 1 && !sVar.isUsedInPhi()) {
				InsnNode assignInsn = sVar.getAssign().getParentInsn();
				if (assignInsn != null && assignInsn.getType() == InsnType.CHECK_CAST) {
					ArgType assignCastType = (ArgType) ((IndexInsnNode) assignInsn).getIndex();
					return assignCastType.equals(castInsn.getIndex());
				}
			}
		}
		return false;
	}

	/**
	 * Remove unnecessary instructions
	 */
	private static void removeStep(MethodNode mth, InsnRemover remover) {
		for (BlockNode block : mth.getBasicBlocks()) {
			remover.setBlock(block);
			for (InsnNode insn : block.getInstructions()) {
				switch (insn.getType()) {
					case NOP:
					case GOTO:
					case NEW_INSTANCE:
						remover.addAndUnbind(insn);
						break;

					default:
						break;
				}
			}
			remover.perform();
		}
	}

	private static void processAnonymousConstructor(MethodNode mth, ConstructorInsn co) {
		MethodInfo callMth = co.getCallMth();
		MethodNode callMthNode = mth.dex().resolveMethod(callMth);
		if (callMthNode == null) {
			return;
		}

		ClassNode classNode = callMthNode.getParentClass();
		if (!classNode.isAnonymous()) {
			return;
		}
		if (!mth.getParentClass().getInnerClasses().contains(classNode)) {
			return;
		}
		Map<InsnArg, FieldNode> argsMap = getArgsToFieldsMapping(callMthNode, co);
		if (argsMap.isEmpty() && !callMthNode.getArguments(true).isEmpty()) {
			return;
		}

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
					sVar.getCodeVar().setFinal(true);
					sVar.add(AFlag.DONT_INLINE);
				}
				reg.add(AFlag.SKIP_ARG);
			}
		}
	}

	private static Map<InsnArg, FieldNode> getArgsToFieldsMapping(MethodNode callMthNode, ConstructorInsn co) {
		Map<InsnArg, FieldNode> map = new LinkedHashMap<>();
		MethodInfo callMth = callMthNode.getMethodInfo();
		ClassNode cls = callMthNode.getParentClass();
		ClassNode parentClass = cls.getParentClass();
		List<RegisterArg> argList = callMthNode.getArguments(false);
		int startArg = 0;
		if (callMth.getArgsCount() != 0 && callMth.getArgumentsTypes().get(0).equals(parentClass.getClassInfo().getType())) {
			startArg = 1;
		}
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
				fieldNode = cls.searchField(field);
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

	private static InsnNode makeFilledArrayInsn(MethodNode mth, NewArrayNode newArrayNode, FillArrayNode insn) {
		ArgType insnArrayType = newArrayNode.getArrayType();
		ArgType insnElementType = insnArrayType.getArrayElement();
		ArgType elType = insn.getElementType();
		if (!elType.isTypeKnown()
				&& insnElementType.isPrimitive()
				&& elType.contains(insnElementType.getPrimitiveType())) {
			elType = insnElementType;
		}
		if (!elType.equals(insnElementType) && !insnArrayType.equals(ArgType.OBJECT)) {
			ErrorsCounter.methodWarn(mth,
					"Incorrect type for fill-array insn " + InsnUtils.formatOffset(insn.getOffset())
							+ ", element type: " + elType + ", insn element type: " + insnElementType);
		}
		if (!elType.isTypeKnown()) {
			LOG.warn("Unknown array element type: {} in mth: {}", elType, mth);
			elType = insnElementType.isTypeKnown() ? insnElementType : elType.selectFirst();
			if (elType == null) {
				throw new JadxRuntimeException("Null array element type");
			}
		}

		List<LiteralArg> list = insn.getLiteralArgs(elType);
		InsnNode filledArr = new FilledNewArrayNode(elType, list.size());
		filledArr.setResult(newArrayNode.getResult());
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

	private static void processMoveException(BlockNode block, InsnNode insn, InsnRemover remover) {
		ExcHandlerAttr excHandlerAttr = block.get(AType.EXC_HANDLER);
		if (excHandlerAttr == null) {
			return;
		}
		ExceptionHandler excHandler = excHandlerAttr.getHandler();

		// result arg used both in this insn and exception handler,
		RegisterArg resArg = insn.getResult();
		ArgType type = excHandler.getArgType();
		String name = excHandler.isCatchAll() ? "th" : "e";
		if (resArg.getName() == null) {
			resArg.setName(name);
		}
		SSAVar sVar = insn.getResult().getSVar();
		if (sVar.getUseCount() == 0) {
			excHandler.setArg(new NamedArg(name, type));
			remover.addAndUnbind(insn);
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
}
