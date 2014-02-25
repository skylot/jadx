package jadx.core.codegen;

import jadx.core.Consts;
import jadx.core.dex.attributes.AttributeFlag;
import jadx.core.dex.attributes.AttributeType;
import jadx.core.dex.attributes.AttributesList;
import jadx.core.dex.attributes.JadxErrorAttr;
import jadx.core.dex.attributes.annotations.MethodParameters;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.NamedArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.Region;
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.visitors.DepthTraverser;
import jadx.core.dex.visitors.FallbackModeVisitor;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.CodegenException;
import jadx.core.utils.exceptions.DecodeException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.android.dx.rop.code.AccessFlags;

public class MethodGen {
	private static final Logger LOG = LoggerFactory.getLogger(MethodGen.class);

	private final MethodNode mth;
	private final ClassGen classGen;
	private final boolean fallback;
	private final AnnotationGen annotationGen;

	private final Set<String> varNames = new HashSet<String>();

	public MethodGen(ClassGen classGen, MethodNode mth) {
		this.mth = mth;
		this.classGen = classGen;
		this.fallback = classGen.isFallbackMode();
		this.annotationGen = classGen.getAnnotationGen();

		List<RegisterArg> args = mth.getArguments(true);
		for (RegisterArg arg : args) {
			varNames.add(makeArgName(arg));
		}
	}

	public ClassGen getClassGen() {
		return classGen;
	}

	public boolean addDefinition(CodeWriter code) {
		if (mth.getMethodInfo().isClassInit()) {
			code.startLine("static");
			code.attachAnnotation(mth);
			return true;
		}
		if (mth.getAttributes().contains(AttributeFlag.ANONYMOUS_CONSTRUCTOR)) {
			// don't add method name and arguments
			code.startLine();
			code.attachAnnotation(mth);
			return false;
		}
		annotationGen.addForMethod(code, mth);

		AccessInfo clsAccFlags = mth.getParentClass().getAccessFlags();
		AccessInfo ai = mth.getAccessFlags();
		// don't add 'abstract' to methods in interface
		if (clsAccFlags.isInterface()) {
			ai = ai.remove(AccessFlags.ACC_ABSTRACT);
		}
		// don't add 'public' for annotations
		if (clsAccFlags.isAnnotation()) {
			ai = ai.remove(AccessFlags.ACC_PUBLIC);
		}
		code.startLine(ai.makeString());

		if (classGen.makeGenericMap(code, mth.getGenericMap())) {
			code.add(' ');
		}
		if (mth.getAccessFlags().isConstructor()) {
			code.add(classGen.getClassNode().getShortName()); // constructor
		} else {
			code.add(TypeGen.translate(classGen, mth.getReturnType()));
			code.add(' ');
			code.add(mth.getName());
		}
		code.add('(');

		List<RegisterArg> args = mth.getArguments(false);
		if (mth.getMethodInfo().isConstructor()
				&& mth.getParentClass().getAttributes().contains(AttributeType.ENUM_CLASS)) {
			if (args.size() == 2) {
				args.clear();
			} else if (args.size() > 2) {
				args = args.subList(2, args.size());
			} else {
				LOG.warn(ErrorsCounter.formatErrorMsg(mth,
						"Incorrect number of args for enum constructor: " + args.size()
								+ " (expected >= 2)"
				));
			}
		}
		code.add(makeArguments(args));
		code.add(")");

		annotationGen.addThrows(mth, code);
		code.attachAnnotation(mth);
		return true;
	}

	public CodeWriter makeArguments(List<RegisterArg> args) {
		CodeWriter argsCode = new CodeWriter();

		MethodParameters paramsAnnotation =
				(MethodParameters) mth.getAttributes().get(AttributeType.ANNOTATION_MTH_PARAMETERS);

		int i = 0;
		for (Iterator<RegisterArg> it = args.iterator(); it.hasNext(); ) {
			RegisterArg arg = it.next();

			// add argument annotation
			if (paramsAnnotation != null) {
				annotationGen.addForParameter(argsCode, paramsAnnotation, i);
			}
			if (!it.hasNext() && mth.getAccessFlags().isVarArgs()) {
				// change last array argument to varargs
				ArgType type = arg.getType();
				if (type.isArray()) {
					ArgType elType = type.getArrayElement();
					argsCode.add(TypeGen.translate(classGen, elType));
					argsCode.add(" ...");
				} else {
					LOG.warn(ErrorsCounter.formatErrorMsg(mth, "Last argument in varargs method not array"));
					argsCode.add(TypeGen.translate(classGen, arg.getType()));
				}
			} else {
				argsCode.add(TypeGen.translate(classGen, arg.getType()));
			}
			argsCode.add(' ');
			argsCode.add(makeArgName(arg));

			i++;
			if (it.hasNext()) {
				argsCode.add(", ");
			}
		}
		return argsCode;
	}

	/**
	 * Make variable name for register,
	 * Name contains register number and
	 * variable type or name (if debug info available)
	 */
	public String makeArgName(RegisterArg arg) {
		String name = arg.getTypedVar().getName();
		String base = "r" + arg.getRegNum();
		if (fallback) {
			if (name != null) {
				return base + "_" + name;
			} else {
				return base;
			}
		} else {
			if (name != null) {
				if (name.equals("this")) {
					return name;
				} else if (Consts.DEBUG) {
					return name + "_" + base;
				} else {
					return name;
				}
			} else {
				ArgType type = arg.getType();
				if (type.isPrimitive()) {
					return base + type.getPrimitiveType().getShortName().toLowerCase();
				} else {
					return base + "_" + Utils.escape(TypeGen.translate(classGen, arg.getType()));
				}
			}
		}
	}

	/**
	 * Put variable declaration and return variable name (used for assignments)
	 *
	 * @param arg register variable
	 * @return variable name
	 */
	public String assignArg(RegisterArg arg) {
		String name = makeArgName(arg);
		if (varNames.add(name) || fallback) {
			return name;
		}
		name = getUniqVarName(name);
		arg.getTypedVar().setName(name);
		return name;
	}

	public String assignNamedArg(NamedArg arg) {
		String name = arg.getName();
		if (varNames.add(name) || fallback) {
			return name;
		}
		name = getUniqVarName(name);
		arg.setName(name);
		return name;
	}

	private String getUniqVarName(String name) {
		String r;
		int i = 2;
		do {
			r = name + "_" + i;
			i++;
		} while (varNames.contains(r));
		varNames.add(r);
		return r;
	}

	public void addInstructions(CodeWriter code) throws CodegenException {
		if (mth.getAttributes().contains(AttributeType.JADX_ERROR)) {
			code.startLine("throw new UnsupportedOperationException(\"Method not decompiled: ");
			code.add(mth.toString());
			code.add("\");");

			JadxErrorAttr err = (JadxErrorAttr) mth.getAttributes().get(AttributeType.JADX_ERROR);
			code.startLine("/* JADX: method processing error */");
			Throwable cause = err.getCause();
			if (cause != null) {
				code.newLine();
				code.add("/*");
				code.startLine("Error: ").add(Utils.getStackTrace(cause));
				code.add("*/");
			}
			makeMethodDump(code);
		} else if (mth.getAttributes().contains(AttributeFlag.INCONSISTENT_CODE)) {
			code.startLine("/*");
			addFallbackMethodCode(code);
			code.startLine("*/");
			code.newLine();
		} else {
			Region startRegion = mth.getRegion();
			if (startRegion != null) {
				(new RegionGen(this, mth)).makeRegion(code, startRegion);
			} else {
				addFallbackMethodCode(code);
			}
		}
	}

	private void makeMethodDump(CodeWriter code) {
		code.startLine("/*");
		getFallbackMethodGen(mth).addDefinition(code);
		code.add(" {");
		code.incIndent();

		addFallbackMethodCode(code);

		code.decIndent();
		code.startLine('}');
		code.startLine("*/");
	}

	public void addFallbackMethodCode(CodeWriter code) {
		if (mth.getInstructions() == null) {
			// loadFile original instructions
			try {
				mth.load();
				DepthTraverser.visit(new FallbackModeVisitor(), mth);
			} catch (DecodeException e) {
				// ignore
				code.startLine("Can't loadFile method instructions");
				return;
			}
		}
		if (mth.getThisArg() != null) {
			code.startLine(getFallbackMethodGen(mth).makeArgName(mth.getThisArg())).add(" = this;");
		}
		addFallbackInsns(code, mth, mth.getInstructions(), true);
	}

	public static void addFallbackInsns(CodeWriter code, MethodNode mth, List<InsnNode> insns, boolean addLabels) {
		InsnGen insnGen = new InsnGen(getFallbackMethodGen(mth), mth, true);
		for (InsnNode insn : insns) {
			AttributesList attrs = insn.getAttributes();
			if (addLabels) {
				if (attrs.contains(AttributeType.JUMP)
						|| attrs.contains(AttributeType.EXC_HANDLER)) {
					code.decIndent();
					code.startLine(getLabelName(insn.getOffset()) + ":");
					code.incIndent();
				}
			}
			try {
				if (insnGen.makeInsn(insn, code)) {
					CatchAttr catchAttr = (CatchAttr) attrs.get(AttributeType.CATCH_BLOCK);
					if (catchAttr != null) {
						code.add("\t //" + catchAttr);
					}
				}
			} catch (CodegenException e) {
				code.startLine("// error: " + insn);
			}
		}
	}

	/**
	 * Return fallback variant of method codegen
	 */
	private static MethodGen getFallbackMethodGen(MethodNode mth) {
		ClassGen clsGen = new ClassGen(mth.getParentClass(), null, true);
		return new MethodGen(clsGen, mth);
	}

	public static String getLabelName(int offset) {
		return "L_" + InsnUtils.formatOffset(offset);
	}

}
