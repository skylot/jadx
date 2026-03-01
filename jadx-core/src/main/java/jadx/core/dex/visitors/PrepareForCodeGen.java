package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.Collection;
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
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
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
import jadx.core.dex.regions.conditions.Compare;
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
	public String getName() {
		return "PrepareForCodeGen";
	}

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
			addNullCasts(mth, block);
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
	 * Otherwise, move to the top and add a warning.
	 *
	 * Improved: When there are instructions before the constructor call that compute
	 * values used by the constructor, try to move only the non-essential instructions
	 * (like null checks) after the constructor call, keeping the essential ones in place.
	 */
	private void moveConstructorInConstructor(MethodNode mth) {
		if (!mth.isConstructor()) {
			return;
		}
		ConstructorInsn ctrInsn = searchConstructorCall(mth);
		if (ctrInsn == null || ctrInsn.contains(AFlag.DONT_GENERATE)) {
			return;
		}
		boolean firstInsn = BlockUtils.isFirstInsn(mth, ctrInsn);
		DeclareVariablesAttr declVarsAttr = mth.getRegion().get(AType.DECLARE_VARIABLES);
		if (firstInsn && declVarsAttr == null) {
			// move not needed
			return;
		}
		String callType = ctrInsn.getCallType().toString().toLowerCase();
		BlockNode blockByInsn = BlockUtils.getBlockByInsn(mth, ctrInsn);
		if (blockByInsn == null) {
			mth.addWarn("Failed to move " + callType + " instruction to top");
			return;
		}

		if (!firstInsn) {
			Set<RegisterArg> regArgs = new HashSet<>();
			ctrInsn.getRegisterArgs(regArgs);
			regArgs.remove(mth.getThisArg());
			mth.getArgRegs().forEach(regArgs::remove);
			mth.addDebugComment("regArgs not empty: " + regArgs.size() + ", trying inline");
			if (!regArgs.isEmpty()) {
				// First, try to inline simple instructions (SGET, CONST, MOVE) into constructor args
				if (tryInlineSimpleInstructions(mth, ctrInsn, blockByInsn, regArgs)) {
					// Check if all register args are now resolved
					Set<RegisterArg> remainingRegs = new HashSet<>();
					ctrInsn.getRegisterArgs(remainingRegs);
					remainingRegs.remove(mth.getThisArg());
					mth.getArgRegs().forEach(remainingRegs::remove);
					if (remainingRegs.isEmpty() || BlockUtils.isFirstInsn(mth, ctrInsn)) {
						// Successfully inlined all dependencies
						mth.addWarnComment("'" + callType + "' call moved to the top of the method (can break code semantics)");
						InsnList.remove(blockByInsn, ctrInsn);
						mth.getRegion().getSubBlocks().add(0, new InsnContainer(ctrInsn));
						return;
					}
				}
				// Try to separate movable instructions from essential ones
				if (tryMoveNonEssentialInstructions(mth, ctrInsn, blockByInsn, regArgs)) {
					mth.addWarnComment("'" + callType + "' call moved to the top of the method (can break code semantics)");
				} else {
					// Check if this is a Kotlin synthetic constructor with DefaultConstructorMarker
					// These are internal constructors for default parameter handling and can be hidden
					if (isKotlinDefaultParamConstructor(mth)) {
						mth.add(AFlag.DONT_GENERATE);
						mth.addWarnComment("Removed Kotlin synthetic constructor with illegal instructions");
					} else {
						mth.addWarnComment("Illegal instructions before constructor call");
					}
				}
				return;
			}
			mth.addWarnComment("'" + callType + "' call moved to the top of the method (can break code semantics)");
		}

		// move confirmed
		InsnList.remove(blockByInsn, ctrInsn);
		mth.getRegion().getSubBlocks().add(0, new InsnContainer(ctrInsn));
	}

	/**
	 * Try to move non-essential instructions (like null checks) to after the constructor call,
	 * keeping only essential instructions (that produce values for constructor) before it.
	 *
	 * @return true if successfully rearranged, false if cannot fix
	 */
	private boolean tryMoveNonEssentialInstructions(MethodNode mth, ConstructorInsn ctrInsn,
			BlockNode blockByInsn, Set<RegisterArg> requiredRegs) {
		List<InsnNode> instructions = blockByInsn.getInstructions();
		int ctrIndex = instructions.indexOf(ctrInsn);
		if (ctrIndex <= 0) {
			return false;
		}

		// Collect all registers that are needed by the constructor (including transitively)
		Set<RegisterArg> essentialRegs = new HashSet<>(requiredRegs);
		boolean changed = true;
		while (changed) {
			changed = false;
			for (int i = 0; i < ctrIndex; i++) {
				InsnNode insn = instructions.get(i);
				RegisterArg result = insn.getResult();
				if (result != null && containsAny(essentialRegs, result)) {
					// This instruction produces an essential result, its inputs are also essential
					Set<RegisterArg> inputRegs = new HashSet<>();
					insn.getRegisterArgs(inputRegs);
					for (RegisterArg inputReg : inputRegs) {
						if (!containsAny(essentialRegs, inputReg)) {
							essentialRegs.add(inputReg);
							changed = true;
						}
					}
				}
			}
		}

		// Separate instructions into essential (must stay before) and movable (can go after)
		List<InsnNode> essentialInsns = new ArrayList<>();
		List<InsnNode> movableInsns = new ArrayList<>();

		for (int i = 0; i < ctrIndex; i++) {
			InsnNode insn = instructions.get(i);
			RegisterArg result = insn.getResult();
			if (result != null && containsAny(essentialRegs, result)) {
				// This instruction produces a value needed by the constructor
				essentialInsns.add(insn);
			} else {
				// This instruction doesn't produce a value for the constructor (e.g., null checks)
				movableInsns.add(insn);
			}
		}

		// If we couldn't identify any movable instructions, we can't fix this
		if (movableInsns.isEmpty()) {
			return false;
		}

		// Rearrange: essential instructions, then constructor call, then movable instructions
		// First, remove all instructions before the constructor from the block
		for (int i = ctrIndex - 1; i >= 0; i--) {
			instructions.remove(i);
		}

		// Now insert: essential instructions first
		for (int i = 0; i < essentialInsns.size(); i++) {
			instructions.add(i, essentialInsns.get(i));
		}

		// The constructor is now at position essentialInsns.size()
		// Insert movable instructions after the constructor
		int insertPos = essentialInsns.size() + 1; // +1 for constructor
		for (InsnNode movableInsn : movableInsns) {
			instructions.add(insertPos++, movableInsn);
		}

		// Move the constructor call to the front of the region
		InsnList.remove(blockByInsn, ctrInsn);
		mth.getRegion().getSubBlocks().add(0, new InsnContainer(ctrInsn));

		return true;
	}

	/**
	 * Check if the set contains a register with the same register number.
	 */
	private boolean containsAny(Set<RegisterArg> set, RegisterArg reg) {
		for (RegisterArg r : set) {
			if (r.getRegNum() == reg.getRegNum()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Try to inline simple instructions (SGET, CONST) directly into the constructor arguments.
	 * This handles cases like:
	 *   int i = DEF_STYLE_RES; // static field read
	 *   super(context, attrs, i, i);
	 * Which becomes:
	 *   super(context, attrs, DEF_STYLE_RES, DEF_STYLE_RES);
	 *
	 * @return true if any inlining was performed AND constructor can now be moved
	 */
	private boolean tryInlineSimpleInstructions(MethodNode mth, ConstructorInsn ctrInsn,
			BlockNode blockByInsn, Set<RegisterArg> requiredRegs) {
		// Map from register number to the instruction that defines it
		java.util.HashMap<Integer, InsnNode> regDefMap = new java.util.HashMap<>();

		// Collect inlineable instructions from current block (before constructor)
		List<InsnNode> instructions = blockByInsn.getInstructions();
		int ctrIndex = instructions.indexOf(ctrInsn);
		for (int i = 0; i < ctrIndex; i++) {
			InsnNode insn = instructions.get(i);
			RegisterArg result = insn.getResult();
			if (result != null && isInlineableInstruction(insn)) {
				regDefMap.put(result.getRegNum(), insn);
			}
		}

		// Also check predecessor blocks for inlineable instructions
		for (BlockNode pred : blockByInsn.getPredecessors()) {
			for (InsnNode insn : pred.getInstructions()) {
				RegisterArg result = insn.getResult();
				if (result != null && isInlineableInstruction(insn)) {
					regDefMap.put(result.getRegNum(), insn);
				}
			}
		}

		if (regDefMap.isEmpty()) {
			return false;
		}

		boolean inlined = false;
		// Try to inline into constructor arguments (including nested ones in wrapped instructions)
		inlined = inlineIntoArgs(mth, ctrInsn, regDefMap);

		if (inlined) {
			// Remove the inlined instructions from the block if they're no longer needed
			removeUnusedInstructions(instructions, instructions.indexOf(ctrInsn), regDefMap, ctrInsn);
		}

		return inlined;
	}

	/**
	/**
	 * Recursively inline simple instructions into the arguments of an instruction.
	 * Handles nested arguments in wrapped instructions (e.g., method calls within constructor args).
	 * Also handles ternary conditions.
	 */
	private boolean inlineIntoArgs(MethodNode mth, InsnNode insn, java.util.HashMap<Integer, InsnNode> regDefMap) {
		boolean inlined = false;
		for (int argIdx = 0; argIdx < insn.getArgsCount(); argIdx++) {
			InsnArg arg = insn.getArg(argIdx);
			if (arg.isRegister()) {
				RegisterArg regArg = (RegisterArg) arg;
				InsnNode defInsn = regDefMap.get(regArg.getRegNum());
				if (defInsn != null) {
					// Inline this instruction into the argument
					InsnNode inlineInsn = defInsn.copy();
					if (inlineInsn != null) {
						InsnArg wrappedArg = regArg.wrapInstruction(mth, inlineInsn);
						if (wrappedArg != null) {
							inlined = true;
						}
					}
				}
			} else if (arg.isInsnWrap()) {
				// Recursively process wrapped instructions
				InsnNode wrappedInsn = ((InsnWrapArg) arg).getWrapInsn();
				if (inlineIntoArgs(mth, wrappedInsn, regDefMap)) {
					inlined = true;
				}
				// Handle ternary instruction conditions inside wrapped args
				if (wrappedInsn.getType() == InsnType.TERNARY) {
					TernaryInsn ternary = (TernaryInsn) wrappedInsn;
					if (inlineIntoCondition(mth, ternary.getCondition(), regDefMap)) {
						inlined = true;
					}
				}
			}
		}
		// Handle ternary instruction conditions at top level
		if (insn.getType() == InsnType.TERNARY) {
			TernaryInsn ternary = (TernaryInsn) insn;
			if (inlineIntoCondition(mth, ternary.getCondition(), regDefMap)) {
				inlined = true;
			}
		}
		return inlined;
	}

	/**
	 * Inline register arguments in IfCondition (used by ternary expressions).
	 */
	private boolean inlineIntoCondition(MethodNode mth, IfCondition condition, java.util.HashMap<Integer, InsnNode> regDefMap) {
		boolean inlined = false;
		if (condition.getMode() == IfCondition.Mode.COMPARE) {
			Compare compare = condition.getCompare();
			if (compare != null) {
				// Try to inline into compare arguments
				InsnArg argA = compare.getA();
				InsnArg argB = compare.getB();
				if (argA != null && argA.isRegister()) {
					RegisterArg regArg = (RegisterArg) argA;
					InsnNode defInsn = regDefMap.get(regArg.getRegNum());
					if (defInsn != null) {
						InsnNode inlineInsn = defInsn.copy();
						if (inlineInsn != null) {
							InsnArg wrappedArg = regArg.wrapInstruction(mth, inlineInsn);
							if (wrappedArg != null) {
								inlined = true;
							}
						}
					}
				}
				if (argB != null && argB.isRegister()) {
					RegisterArg regArg = (RegisterArg) argB;
					InsnNode defInsn = regDefMap.get(regArg.getRegNum());
					if (defInsn != null) {
						InsnNode inlineInsn = defInsn.copy();
						if (inlineInsn != null) {
							InsnArg wrappedArg = regArg.wrapInstruction(mth, inlineInsn);
							if (wrappedArg != null) {
								inlined = true;
							}
						}
					}
				}
			}
		} else {
			// Recursively process nested conditions (AND, OR, NOT)
			for (IfCondition nested : condition.getArgs()) {
				if (inlineIntoCondition(mth, nested, regDefMap)) {
					inlined = true;
				}
			}
		}
		return inlined;
	}

	/**
	 * Check if an instruction can be safely inlined into a constructor argument.
	 * Only simple, side-effect-free instructions are allowed.
	 */
	private boolean isInlineableInstruction(InsnNode insn) {
		switch (insn.getType()) {
			case SGET:  // Static field read
			case IGET:  // Instance field read
			case CONST:
			case CONST_STR:
			case CONST_CLASS:
			case CHECK_CAST:  // Type cast
			case INVOKE:  // Method invocations (getters, etc.)
				return true;
			// Note: MOVE is not inlineable because wrapping it creates assignment expressions
			// that look like (var = source) which is confusing in constructor arguments
			default:
				return false;
		}
	}

	/**
	 * Remove instructions that were inlined and are no longer used elsewhere.
	 */
	private void removeUnusedInstructions(List<InsnNode> instructions, int ctrIndex,
			java.util.HashMap<Integer, InsnNode> inlinedInsns, ConstructorInsn ctrInsn) {
		// Check each inlined instruction to see if it's still needed elsewhere
		Iterator<java.util.Map.Entry<Integer, InsnNode>> it = inlinedInsns.entrySet().iterator();
		while (it.hasNext()) {
			java.util.Map.Entry<Integer, InsnNode> entry = it.next();
			InsnNode insn = entry.getValue();
			RegisterArg result = insn.getResult();
			if (result == null) {
				continue;
			}

			// Check if this result is still used elsewhere (other than in constructor args we inlined)
			boolean stillUsed = false;
			for (int i = 0; i < instructions.size(); i++) {
				if (i == instructions.indexOf(insn)) {
					continue; // Skip the instruction itself
				}
				InsnNode otherInsn = instructions.get(i);
				if (otherInsn == ctrInsn) {
					continue; // Skip constructor (we already inlined there)
				}
				Set<RegisterArg> usedRegs = new HashSet<>();
				otherInsn.getRegisterArgs(usedRegs);
				if (containsAny(usedRegs, result)) {
					stillUsed = true;
					break;
				}
			}

			if (!stillUsed) {
				// Safe to remove this instruction
				instructions.remove(insn);
			}
		}
	}

	/**
	 * Check if this method is a Kotlin synthetic constructor with DefaultConstructorMarker parameter.
	 * These are generated by Kotlin for default parameter handling and are internal implementation details.
	 * Format: Constructor(params..., int flags, DefaultConstructorMarker marker)
	 */
	private boolean isKotlinDefaultParamConstructor(MethodNode mth) {
		if (!mth.getAccessFlags().isSynthetic()) {
			return false;
		}
		List<ArgType> argTypes = mth.getMethodInfo().getArgumentsTypes();
		if (argTypes.size() < 2) {
			return false;
		}
		// Check if last parameter is DefaultConstructorMarker
		ArgType lastArg = argTypes.get(argTypes.size() - 1);
		if (lastArg.isObject()) {
			String className = lastArg.getObject();
			if (className != null && className.endsWith("DefaultConstructorMarker")) {
				return true;
			}
		}
		return false;
	}

	private @Nullable ConstructorInsn searchConstructorCall(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				if (insn.getType() == InsnType.CONSTRUCTOR) {
					ConstructorInsn ctrInsn = (ConstructorInsn) insn;
					if (ctrInsn.isSuper() || ctrInsn.isThis()) {
						return ctrInsn;
					}
					return null;
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

	private void addNullCasts(MethodNode mth, BlockNode block) {
		for (InsnNode insn : block.getInstructions()) {
			switch (insn.getType()) {
				case INVOKE:
					verifyNullCast(mth, ((InvokeNode) insn).getInstanceArg());
					break;

				case ARRAY_LENGTH:
					verifyNullCast(mth, insn.getArg(0));
					break;
			}
		}
	}

	private void verifyNullCast(MethodNode mth, InsnArg arg) {
		if (arg != null && arg.isZeroConst()) {
			ArgType castType = arg.getType();
			IndexInsnNode castInsn = new IndexInsnNode(InsnType.CAST, castType, 1);
			castInsn.addArg(InsnArg.lit(0, castType));
			arg.wrapInstruction(mth, castInsn);
		}
	}
}
