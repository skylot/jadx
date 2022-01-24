package jadx.core.dex.visitors;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.IFieldRef;
import jadx.api.plugins.input.data.annotations.AnnotationVisibility;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.types.AnnotationsAttr;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.AttrNode;
import jadx.core.dex.attributes.nodes.DeclareVariablesAttr;
import jadx.core.dex.attributes.nodes.LineAttrNode;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.ArithOp;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.instructions.mods.TernaryInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnContainer;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.conditions.IfCondition;
import jadx.core.dex.regions.conditions.IfCondition.Mode;
import jadx.core.dex.visitors.regions.variables.ProcessVariables;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnList;
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
	public boolean visit(ClassNode cls) throws JadxException {
		if (cls.root().getArgs().isDebugInfo()) {
			setClassSourceLine(cls);
		}
		collectFieldsUsageInAnnotations(cls);
		return true;
	}

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		for (BlockNode block : mth.getBasicBlocks()) {
			if (block.contains(AFlag.DONT_GENERATE)) {
				continue;
			}
			removeInstructions(block);
			checkInline(block);
			removeParenthesis(block);
			modifyArith(block);
			checkConstUsage(block);
		}
		moveConstructorInConstructor(mth);
		collectFieldsUsageInAnnotations(mth, mth);
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

	/**
	 * Add explicit type for non int constants
	 */
	private static void checkConstUsage(BlockNode block) {
		for (InsnNode blockInsn : block.getInstructions()) {
			blockInsn.visitInsns(insn -> {
				if (forbidExplicitType(insn.getType())) {
					return;
				}
				for (InsnArg arg : insn.getArguments()) {
					if (arg.isLiteral() && arg.getType() != ArgType.INT) {
						arg.add(AFlag.EXPLICIT_PRIMITIVE_TYPE);
					}
				}
			});
		}
	}

	private static boolean forbidExplicitType(InsnType type) {
		switch (type) {
			case CONST:
			case CAST:
			case IF:
			case FILLED_NEW_ARRAY:
			case APUT:
			case ARITH:
				return true;
			default:
				return false;
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

	/**
	 * Check that 'super' or 'this' call in constructor is a first instruction.
	 * Otherwise move to top and add a warning if code breaks.
	 */
	private void moveConstructorInConstructor(MethodNode mth) {
		if (mth.isConstructor()) {
			ConstructorInsn constrInsn = searchConstructorCall(mth);
			if (constrInsn != null && !constrInsn.contains(AFlag.DONT_GENERATE)) {
				Region oldRootRegion = mth.getRegion();
				boolean firstInsn = BlockUtils.isFirstInsn(mth, constrInsn);
				DeclareVariablesAttr declVarsAttr = oldRootRegion.get(AType.DECLARE_VARIABLES);
				if (firstInsn && declVarsAttr == null) {
					// move not needed
					return;
				}

				// move constructor instruction to new root region
				String callType = constrInsn.getCallType().toString().toLowerCase();
				BlockNode blockByInsn = BlockUtils.getBlockByInsn(mth, constrInsn);
				if (blockByInsn == null) {
					mth.addWarn("Failed to move " + callType + " instruction to top");
					return;
				}
				InsnList.remove(blockByInsn, constrInsn);

				Region region = new Region(null);
				region.add(new InsnContainer(Collections.singletonList(constrInsn)));
				region.add(oldRootRegion);
				mth.setRegion(region);

				if (!firstInsn) {
					Set<RegisterArg> regArgs = new HashSet<>();
					constrInsn.getRegisterArgs(regArgs);
					regArgs.remove(mth.getThisArg());
					mth.getArgRegs().forEach(regArgs::remove);
					if (!regArgs.isEmpty()) {
						mth.addWarn("Illegal instructions before constructor call");
					} else {
						mth.addWarnComment("'" + callType + "' call moved to the top of the method (can break code semantics)");
					}
				}
			}
		}
	}

	@Nullable
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

	/**
	 * Use source line from top method
	 */
	private void setClassSourceLine(ClassNode cls) {
		for (ClassNode innerClass : cls.getInnerClasses()) {
			setClassSourceLine(innerClass);
		}
		int minLine = Stream.of(cls.getMethods(), cls.getInnerClasses(), cls.getFields())
				.flatMap(Collection::stream)
				.filter(mth -> !mth.contains(AFlag.DONT_GENERATE))
				.filter(mth -> mth.getSourceLine() != 0)
				.mapToInt(LineAttrNode::getSourceLine)
				.min()
				.orElse(0);
		if (minLine != 0) {
			cls.setSourceLine(minLine - 1);
		}
	}

	private void collectFieldsUsageInAnnotations(ClassNode cls) {
		MethodNode useMth = cls.getDefaultConstructor();
		if (useMth == null && !cls.getMethods().isEmpty()) {
			useMth = cls.getMethods().get(0);
		}
		if (useMth == null) {
			return;
		}
		collectFieldsUsageInAnnotations(useMth, cls);
		MethodNode finalUseMth = useMth;
		cls.getFields().forEach(f -> collectFieldsUsageInAnnotations(finalUseMth, f));
	}

	private void collectFieldsUsageInAnnotations(MethodNode mth, AttrNode attrNode) {
		AnnotationsAttr annotationsList = attrNode.get(JadxAttrType.ANNOTATION_LIST);
		if (annotationsList == null) {
			return;
		}
		for (IAnnotation annotation : annotationsList.getAll()) {
			if (annotation.getVisibility() == AnnotationVisibility.SYSTEM) {
				continue;
			}
			for (Map.Entry<String, EncodedValue> entry : annotation.getValues().entrySet()) {
				checkEncodedValue(mth, entry.getValue());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void checkEncodedValue(MethodNode mth, EncodedValue encodedValue) {
		switch (encodedValue.getType()) {
			case ENCODED_FIELD:
				Object fieldData = encodedValue.getValue();
				FieldInfo fieldInfo;
				if (fieldData instanceof IFieldRef) {
					fieldInfo = FieldInfo.fromRef(mth.root(), (IFieldRef) fieldData);
				} else {
					fieldInfo = (FieldInfo) fieldData;
				}
				FieldNode fieldNode = mth.root().resolveField(fieldInfo);
				if (fieldNode != null) {
					fieldNode.addUseIn(mth);
				}
				break;

			case ENCODED_ANNOTATION:
				IAnnotation annotation = (IAnnotation) encodedValue.getValue();
				annotation.getValues().forEach((k, v) -> checkEncodedValue(mth, v));
				break;

			case ENCODED_ARRAY:
				List<EncodedValue> valueList = (List<EncodedValue>) encodedValue.getValue();
				valueList.forEach(v -> checkEncodedValue(mth, v));
				break;
		}
	}
}
