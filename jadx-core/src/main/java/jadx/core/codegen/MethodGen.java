package jadx.core.codegen;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.CommentsLevel;
import jadx.api.ICodeWriter;
import jadx.api.JadxArgs;
import jadx.api.metadata.annotations.InsnCodeOffset;
import jadx.api.metadata.annotations.VarNode;
import jadx.api.plugins.input.data.AccessFlags;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.types.AnnotationMethodParamsAttr;
import jadx.core.Consts;
import jadx.core.Jadx;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.JadxError;
import jadx.core.dex.attributes.nodes.JumpInfo;
import jadx.core.dex.attributes.nodes.MethodOverrideAttr;
import jadx.core.dex.attributes.nodes.MethodReplaceAttr;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.CodeVar;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.visitors.DepthTraversal;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.utils.CodeGenUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.CodegenException;
import jadx.core.utils.exceptions.JadxOverflowException;

import static jadx.core.codegen.MethodGen.FallbackOption.BLOCK_DUMP;
import static jadx.core.codegen.MethodGen.FallbackOption.COMMENTED_DUMP;
import static jadx.core.codegen.MethodGen.FallbackOption.FALLBACK_MODE;

public class MethodGen {
	private static final Logger LOG = LoggerFactory.getLogger(MethodGen.class);

	private final MethodNode mth;
	private final ClassGen classGen;
	private final AnnotationGen annotationGen;
	private final NameGen nameGen;

	public MethodGen(ClassGen classGen, MethodNode mth) {
		this.mth = mth;
		this.classGen = classGen;
		this.annotationGen = classGen.getAnnotationGen();
		this.nameGen = new NameGen(mth, classGen);
	}

	public ClassGen getClassGen() {
		return classGen;
	}

	public NameGen getNameGen() {
		return nameGen;
	}

	public MethodNode getMethodNode() {
		return mth;
	}

	public boolean addDefinition(ICodeWriter code) {
		if (mth.getMethodInfo().isClassInit()) {
			code.attachDefinition(mth);
			code.startLine("static");
			return true;
		}
		if (mth.contains(AFlag.ANONYMOUS_CONSTRUCTOR)) {
			// don't add method name and arguments
			code.startLine();
			code.attachDefinition(mth);
			return false;
		}
		if (Consts.DEBUG_USAGE) {
			ClassGen.addMthUsageInfo(code, mth);
		}
		addOverrideAnnotation(code, mth);
		annotationGen.addForMethod(code, mth);

		AccessInfo clsAccFlags = mth.getParentClass().getAccessFlags();
		AccessInfo ai = mth.getAccessFlags();
		// don't add 'abstract' and 'public' to methods in interface
		if (clsAccFlags.isInterface()) {
			ai = ai.remove(AccessFlags.ABSTRACT);
			ai = ai.remove(AccessFlags.PUBLIC);
		}
		// don't add 'public' for annotations
		if (clsAccFlags.isAnnotation()) {
			ai = ai.remove(AccessFlags.PUBLIC);
		}
		if (mth.getMethodInfo().isConstructor() && mth.getParentClass().isEnum()) {
			ai = ai.remove(AccessInfo.VISIBILITY_FLAGS);
		}

		if (mth.getMethodInfo().hasAlias() && !ai.isConstructor()) {
			CodeGenUtils.addRenamedComment(code, mth, mth.getName());
		}
		if (mth.contains(AFlag.INCONSISTENT_CODE) && mth.checkCommentsLevel(CommentsLevel.ERROR)) {
			code.startLine("/*");
			code.incIndent();
			code.startLine("Code decompiled incorrectly, please refer to instructions dump.");
			if (!mth.root().getArgs().isShowInconsistentCode()) {
				if (code.isMetadataSupported()) {
					code.startLine("To view partially-correct code enable 'Show inconsistent code' option in preferences");
				} else {
					code.startLine("To view partially-correct add '--show-bad-code' argument");
				}
			}
			code.decIndent();
			code.startLine("*/");
		}

		code.startLineWithNum(mth.getSourceLine());
		code.add(ai.makeString(mth.checkCommentsLevel(CommentsLevel.INFO)));
		if (clsAccFlags.isInterface() && !mth.isNoCode() && !mth.getAccessFlags().isStatic()) {
			// add 'default' for method with code in interface
			code.add("default ");
		}

		if (classGen.addGenericTypeParameters(code, mth.getTypeParameters(), false)) {
			code.add(' ');
		}
		if (ai.isConstructor()) {
			code.attachDefinition(mth);
			code.add(classGen.getClassNode().getShortName()); // constructor
		} else {
			classGen.useType(code, mth.getReturnType());
			code.add(' ');
			MethodNode defMth = getMethodForDefinition();
			code.attachDefinition(defMth);
			code.add(defMth.getAlias());
		}
		code.add('(');

		List<RegisterArg> args = mth.getArgRegs();
		if (mth.getMethodInfo().isConstructor()
				&& mth.getParentClass().contains(AType.ENUM_CLASS)) {
			if (args.size() == 2) {
				args = Collections.emptyList();
			} else if (args.size() > 2) {
				args = args.subList(2, args.size());
			} else {
				mth.addWarnComment("Incorrect number of args for enum constructor: " + args.size() + " (expected >= 2)");
			}
		} else if (mth.contains(AFlag.SKIP_FIRST_ARG)) {
			args = args.subList(1, args.size());
		}
		addMethodArguments(code, args);
		code.add(')');

		annotationGen.addThrows(mth, code);

		// add default value for annotation class
		if (mth.getParentClass().getAccessFlags().isAnnotation()) {
			EncodedValue def = annotationGen.getAnnotationDefaultValue(mth);
			if (def != null) {
				code.add(" default ");
				annotationGen.encodeValue(mth.root(), code, def);
			}
		}
		return true;
	}

	private MethodNode getMethodForDefinition() {
		MethodReplaceAttr replaceAttr = mth.get(AType.METHOD_REPLACE);
		if (replaceAttr != null) {
			return replaceAttr.getReplaceMth();
		}
		return mth;
	}

	private void addOverrideAnnotation(ICodeWriter code, MethodNode mth) {
		MethodOverrideAttr overrideAttr = mth.get(AType.METHOD_OVERRIDE);
		if (overrideAttr == null) {
			return;
		}
		if (!overrideAttr.getBaseMethods().contains(mth)) {
			code.startLine("@Override");
			if (mth.checkCommentsLevel(CommentsLevel.INFO)) {
				code.add(" // ");
				code.add(Utils.listToString(overrideAttr.getOverrideList(), ", ",
						md -> md.getMethodInfo().getDeclClass().getAliasFullName()));
			}
		}
		if (Consts.DEBUG) {
			code.startLine("// related by override: ");
			code.add(Utils.listToString(overrideAttr.getRelatedMthNodes(), ", ", m -> m.getParentClass().getFullName()));
		}
	}

	private void addMethodArguments(ICodeWriter code, List<RegisterArg> args) {
		AnnotationMethodParamsAttr paramsAnnotation = mth.get(JadxAttrType.ANNOTATION_MTH_PARAMETERS);
		int i = 0;
		Iterator<RegisterArg> it = args.iterator();
		while (it.hasNext()) {
			RegisterArg mthArg = it.next();
			SSAVar ssaVar = mthArg.getSVar();
			CodeVar var;
			if (ssaVar == null) {
				// abstract or interface methods
				var = CodeVar.fromMthArg(mthArg, classGen.isFallbackMode());
			} else {
				var = ssaVar.getCodeVar();
			}

			// add argument annotation
			if (paramsAnnotation != null) {
				annotationGen.addForParameter(code, paramsAnnotation, i);
			}
			if (var.isFinal()) {
				code.add("final ");
			}
			ArgType argType;
			ArgType varType = var.getType();
			if (varType == null || varType == ArgType.UNKNOWN) {
				// occur on decompilation errors
				argType = mthArg.getInitType();
			} else {
				argType = varType;
			}
			if (!it.hasNext() && mth.getAccessFlags().isVarArgs()) {
				// change last array argument to varargs
				if (argType.isArray()) {
					ArgType elType = argType.getArrayElement();
					classGen.useType(code, elType);
					code.add("...");
				} else {
					mth.addWarnComment("Last argument in varargs method is not array: " + var);
					classGen.useType(code, argType);
				}
			} else {
				classGen.useType(code, argType);
			}
			code.add(' ');
			String varName = nameGen.assignArg(var);
			if (code.isMetadataSupported() && ssaVar != null /* for fallback mode */) {
				code.attachDefinition(VarNode.get(mth, var));
			}
			code.add(varName);

			i++;
			if (it.hasNext()) {
				code.add(", ");
			}
		}
	}

	public void addInstructions(ICodeWriter code) throws CodegenException {
		JadxArgs args = mth.root().getArgs();
		switch (args.getDecompilationMode()) {
			case AUTO:
				if (classGen.isFallbackMode()) {
					// TODO: try simple mode first
					dumpInstructions(code);
				} else {
					addRegionInsns(code);
				}
				break;

			case RESTRUCTURE:
				addRegionInsns(code);
				break;

			case SIMPLE:
				addSimpleMethodCode(code);
				break;

			case FALLBACK:
				addFallbackMethodCode(code, FALLBACK_MODE);
				break;
		}
	}

	public void addRegionInsns(ICodeWriter code) throws CodegenException {
		try {
			RegionGen regionGen = new RegionGen(this);
			regionGen.makeRegion(code, mth.getRegion());
		} catch (StackOverflowError | BootstrapMethodError e) {
			mth.addError("Method code generation error", new JadxOverflowException("StackOverflow"));
			CodeGenUtils.addErrors(code, mth);
			dumpInstructions(code);
		} catch (Exception e) {
			if (mth.getParentClass().getTopParentClass().contains(AFlag.RESTART_CODEGEN)) {
				throw e;
			}
			mth.addError("Method code generation error", e);
			CodeGenUtils.addErrors(code, mth);
			dumpInstructions(code);
		}
	}

	private void addSimpleMethodCode(ICodeWriter code) {
		if (mth.getBasicBlocks() == null) {
			code.startLine("// Blocks not ready for simple mode, using fallback");
			addFallbackMethodCode(code, FALLBACK_MODE);
			return;
		}
		JadxArgs args = mth.root().getArgs();
		ICodeWriter tmpCode = args.getCodeWriterProvider().apply(args);
		try {
			tmpCode.setIndent(code.getIndent());
			generateSimpleCode(tmpCode);
			code.add(tmpCode);
		} catch (Exception e) {
			mth.addError("Simple mode code generation failed", e);
			CodeGenUtils.addError(code, "Simple mode code generation failed", e);
			dumpInstructions(code);
		}
	}

	private void generateSimpleCode(ICodeWriter code) throws CodegenException {
		SimpleModeHelper helper = new SimpleModeHelper(mth);
		List<BlockNode> blocks = helper.prepareBlocks();
		InsnGen insnGen = new InsnGen(this, true);
		for (BlockNode block : blocks) {
			if (block.contains(AFlag.DONT_GENERATE)) {
				continue;
			}
			if (helper.isNeedStartLabel(block)) {
				code.decIndent();
				code.startLine(getLabelName(block)).add(':');
				code.incIndent();
			}
			for (InsnNode insn : block.getInstructions()) {
				if (!insn.contains(AFlag.DONT_GENERATE)) {
					if (insn.getResult() != null) {
						CodeVar codeVar = insn.getResult().getSVar().getCodeVar();
						if (!codeVar.isDeclared()) {
							insn.add(AFlag.DECLARE_VAR);
							codeVar.setDeclared(true);
						}
					}
					InsnCodeOffset.attach(code, insn);
					insnGen.makeInsn(insn, code);
					addCatchComment(code, insn, false);
					CodeGenUtils.addCodeComments(code, mth, insn);
				}
			}
			if (helper.isNeedEndGoto(block)) {
				code.startLine("goto ").add(getLabelName(block.getSuccessors().get(0)));
			}
		}
	}

	public void dumpInstructions(ICodeWriter code) {
		if (mth.checkCommentsLevel(CommentsLevel.ERROR)) {
			code.startLine("/*");
			addFallbackMethodCode(code, COMMENTED_DUMP);
			code.startLine("*/");
		}
		code.startLine("throw new UnsupportedOperationException(\"Method not decompiled: ")
				.add(mth.getParentClass().getClassInfo().getAliasFullName())
				.add('.')
				.add(mth.getAlias())
				.add('(')
				.add(Utils.listToString(mth.getMethodInfo().getArgumentsTypes()))
				.add("):")
				.add(mth.getMethodInfo().getReturnType().toString())
				.add("\");");
	}

	public void addFallbackMethodCode(ICodeWriter code, FallbackOption fallbackOption) {
		if (fallbackOption != FALLBACK_MODE) {
			List<JadxError> errors = mth.getAll(AType.JADX_ERROR); // preserve error before unload
			try {
				// load original instructions
				mth.unload();
				mth.load();
				for (IDexTreeVisitor visitor : Jadx.getFallbackPassesList()) {
					DepthTraversal.visit(visitor, mth);
				}
				errors.forEach(err -> mth.addAttr(AType.JADX_ERROR, err));
			} catch (Exception e) {
				LOG.error("Error reload instructions in fallback mode:", e);
				code.startLine("// Can't load method instructions: " + e.getMessage());
				return;
			} finally {
				errors.forEach(err -> mth.addAttr(AType.JADX_ERROR, err));
			}
		}
		InsnNode[] insnArr = mth.getInstructions();
		if (insnArr == null) {
			code.startLine("// Can't load method instructions.");
			return;
		}
		if (fallbackOption == COMMENTED_DUMP && mth.getCommentsLevel() != CommentsLevel.DEBUG) {
			long insnCountEstimate = Stream.of(insnArr)
					.filter(Objects::nonNull)
					.filter(insn -> insn.getType() != InsnType.NOP)
					.count();
			if (insnCountEstimate > 100) {
				code.incIndent();
				code.startLine("Method dump skipped, instructions count: " + insnArr.length);
				if (code.isMetadataSupported()) {
					code.startLine("To view this dump change 'Code comments level' option to 'DEBUG'");
				} else {
					code.startLine("To view this dump add '--comments-level debug' option");
				}
				code.decIndent();
				return;
			}
		}
		code.incIndent();
		if (mth.getThisArg() != null) {
			code.startLine(nameGen.useArg(mth.getThisArg())).add(" = this;");
		}
		addFallbackInsns(code, mth, insnArr, fallbackOption);
		code.decIndent();
	}

	public enum FallbackOption {
		FALLBACK_MODE,
		BLOCK_DUMP,
		COMMENTED_DUMP
	}

	public static void addFallbackInsns(ICodeWriter code, MethodNode mth, InsnNode[] insnArr, FallbackOption option) {
		int startIndent = code.getIndent();
		MethodGen methodGen = getFallbackMethodGen(mth);
		InsnGen insnGen = new InsnGen(methodGen, true);
		InsnNode prevInsn = null;
		for (InsnNode insn : insnArr) {
			if (insn == null) {
				continue;
			}
			methodGen.dumpInsn(code, insnGen, option, startIndent, prevInsn, insn);
			prevInsn = insn;
		}
	}

	private boolean dumpInsn(ICodeWriter code, InsnGen insnGen, FallbackOption option, int startIndent,
			@Nullable InsnNode prevInsn, InsnNode insn) {
		if (insn.contains(AType.JADX_ERROR)) {
			for (JadxError error : insn.getAll(AType.JADX_ERROR)) {
				code.startLine("// ").add(error.getError());
			}
			return true;
		}
		if (option != BLOCK_DUMP && needLabel(insn, prevInsn)) {
			code.decIndent();
			code.startLine(getLabelName(insn.getOffset()) + ':');
			code.incIndent();
		}
		if (insn.getType() == InsnType.NOP) {
			return true;
		}
		try {
			boolean escapeComment = isCommentEscapeNeeded(insn, option);
			if (escapeComment) {
				code.decIndent();
				code.startLine("*/");
				code.startLine("//  ");
			} else {
				code.startLineWithNum(insn.getSourceLine());
			}
			InsnCodeOffset.attach(code, insn);
			RegisterArg resArg = insn.getResult();
			if (resArg != null) {
				ArgType varType = resArg.getInitType();
				if (varType.isTypeKnown()) {
					code.add(varType.toString()).add(' ');
				}
			}
			insnGen.makeInsn(insn, code, InsnGen.Flags.INLINE);
			if (escapeComment) {
				code.startLine("/*");
				code.incIndent();
			}
			addCatchComment(code, insn, true);
			CodeGenUtils.addCodeComments(code, mth, insn);
		} catch (Exception e) {
			LOG.debug("Error generate fallback instruction: ", e.getCause());
			code.setIndent(startIndent);
			code.startLine("// error: " + insn);
		}
		return false;
	}

	private void addCatchComment(ICodeWriter code, InsnNode insn, boolean raw) {
		CatchAttr catchAttr = insn.get(AType.EXC_CATCH);
		if (catchAttr == null) {
			return;
		}
		code.add("     // Catch:");
		for (ExceptionHandler handler : catchAttr.getHandlers()) {
			code.add(' ');
			classGen.useClass(code, handler.getArgType());
			code.add(" -> ");
			if (raw) {
				code.add(getLabelName(handler.getHandlerOffset()));
			} else {
				code.add(getLabelName(handler.getHandlerBlock()));
			}
		}
	}

	private static boolean isCommentEscapeNeeded(InsnNode insn, FallbackOption option) {
		if (option == COMMENTED_DUMP) {
			if (insn.getType() == InsnType.CONST_STR) {
				String str = ((ConstStringNode) insn).getString();
				return str.contains("*/");
			}
		}
		return false;
	}

	private static boolean needLabel(InsnNode insn, InsnNode prevInsn) {
		if (insn.contains(AType.EXC_HANDLER)) {
			return true;
		}
		if (insn.contains(AType.JUMP)) {
			// don't add label for ifs else branch
			if (prevInsn != null && prevInsn.getType() == InsnType.IF) {
				List<JumpInfo> jumps = insn.getAll(AType.JUMP);
				if (jumps.size() == 1) {
					JumpInfo jump = jumps.get(0);
					if (jump.getSrc() == prevInsn.getOffset() && jump.getDest() == insn.getOffset()) {
						int target = ((IfNode) prevInsn).getTarget();
						return insn.getOffset() == target;
					}
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Return fallback variant of method codegen
	 */
	public static MethodGen getFallbackMethodGen(MethodNode mth) {
		ClassGen clsGen = new ClassGen(mth.getParentClass(), null, false, true, true);
		return new MethodGen(clsGen, mth);
	}

	public static String getLabelName(BlockNode block) {
		return String.format("L%d", block.getId());
	}

	public static String getLabelName(IfNode insn) {
		BlockNode thenBlock = insn.getThenBlock();
		if (thenBlock != null) {
			return getLabelName(thenBlock);
		}
		return getLabelName(insn.getTarget());
	}

	public static String getLabelName(int offset) {
		if (offset < 0) {
			return String.format("LB_%x", -offset);
		}
		return String.format("L%x", offset);
	}
}
