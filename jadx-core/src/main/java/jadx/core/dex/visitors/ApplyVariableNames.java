package jadx.core.dex.visitors;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import jadx.core.Consts;
import jadx.core.deobf.NameMapper;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.CodeVar;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.regions.variables.ProcessVariables;
import jadx.core.utils.StringUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "ApplyVariableNames",
		desc = "Try to guess variable name from usage",
		runAfter = {
				ProcessVariables.class
		}
)
public class ApplyVariableNames extends AbstractVisitor {

	private static final Map<String, String> OBJ_ALIAS = Utils.newConstStringMap(
			Consts.CLASS_STRING, "str",
			Consts.CLASS_CLASS, "cls",
			Consts.CLASS_THROWABLE, "th",
			Consts.CLASS_OBJECT, "obj",
			"java.util.Iterator", "it",
			"java.util.HashMap", "map",
			"java.lang.Boolean", "bool",
			"java.lang.Short", "sh",
			"java.lang.Integer", "num",
			"java.lang.Character", "ch",
			"java.lang.Byte", "b",
			"java.lang.Float", "f",
			"java.lang.Long", "l",
			"java.lang.Double", "d",
			"java.lang.StringBuilder", "sb",
			"java.lang.Exception", "exc");

	private static final Set<String> GOOD_VAR_NAMES = Set.of(
			"size", "length", "list", "map", "next");
	private static final List<String> INVOKE_PREFIXES = List.of(
			"get", "set", "to", "parse", "read", "format");

	private RootNode root;

	@Override
	public void init(RootNode root) throws JadxException {
		this.root = root;
	}

	@Override
	public void visit(MethodNode mth) throws JadxException {
		for (SSAVar ssaVar : mth.getSVars()) {
			CodeVar codeVar = ssaVar.getCodeVar();
			String newName = guessName(codeVar);
			if (newName != null) {
				codeVar.setName(newName);
			}
		}
	}

	private @Nullable String guessName(CodeVar var) {
		if (var.isThis()) {
			return RegisterArg.THIS_ARG_NAME;
		}
		if (!var.isDeclared()) {
			// name is not used in code
			return null;
		}
		if (NameMapper.isValidAndPrintable(var.getName())) {
			// the current name is valid, keep it
			return null;
		}
		List<SSAVar> ssaVars = var.getSsaVars();
		if (Utils.notEmpty(ssaVars)) {
			boolean mthArg = ssaVars.stream().anyMatch(ssaVar -> ssaVar.getAssign().contains(AFlag.METHOD_ARGUMENT));
			if (mthArg) {
				// for method args use defined type and ignore usage
				return makeNameForType(var.getType());
			}
			for (SSAVar ssaVar : ssaVars) {
				String name = makeNameForSSAVar(ssaVar);
				if (name != null) {
					return name;
				}
			}
		}
		return makeNameForType(var.getType());
	}

	private @Nullable String makeNameForSSAVar(SSAVar ssaVar) {
		String ssaVarName = ssaVar.getName();
		if (ssaVarName != null) {
			return ssaVarName;
		}
		InsnNode assignInsn = ssaVar.getAssignInsn();
		if (assignInsn != null) {
			String name = makeNameFromInsn(ssaVar, assignInsn);
			if (NameMapper.isValidAndPrintable(name)) {
				return name;
			}
		}
		return null;
	}

	private String makeNameFromInsn(SSAVar ssaVar, InsnNode insn) {
		switch (insn.getType()) {
			case INVOKE:
				return makeNameFromInvoke(ssaVar, (InvokeNode) insn);

			case CONSTRUCTOR:
				ConstructorInsn co = (ConstructorInsn) insn;
				MethodNode callMth = root.getMethodUtils().resolveMethod(co);
				if (callMth != null && callMth.contains(AFlag.ANONYMOUS_CONSTRUCTOR)) {
					// don't use name of anonymous class
					return null;
				}
				return makeNameForClass(co.getClassType());

			case ARRAY_LENGTH:
				return "length";

			case ARITH:
			case TERNARY:
			case CAST:
				for (InsnArg arg : insn.getArguments()) {
					if (arg.isInsnWrap()) {
						InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
						String wName = makeNameFromInsn(ssaVar, wrapInsn);
						if (wName != null) {
							return wName;
						}
					}
				}
				break;

			default:
				break;
		}
		return null;
	}

	private String makeNameForType(ArgType type) {
		if (type.isPrimitive()) {
			return type.getPrimitiveType().getShortName().toLowerCase();
		}
		if (type.isArray()) {
			return makeNameForType(type.getArrayRootElement()) + "Arr";
		}
		return makeNameForObject(type);
	}

	private String makeNameForObject(ArgType type) {
		if (type.isGenericType()) {
			return StringUtils.escape(type.getObject().toLowerCase());
		}
		if (type.isObject()) {
			String alias = getAliasForObject(type.getObject());
			if (alias != null) {
				return alias;
			}
			return makeNameForCheckedClass(ClassInfo.fromType(root, type));
		}
		return StringUtils.escape(type.toString());
	}

	private String makeNameForCheckedClass(ClassInfo classInfo) {
		String shortName = classInfo.getAliasShortName();
		String vName = fromName(shortName);
		if (vName != null) {
			return vName;
		}
		String lower = StringUtils.escape(shortName.toLowerCase());
		if (shortName.equals(lower)) {
			return lower + "Var";
		}
		return lower;
	}

	private String makeNameForClass(ClassInfo classInfo) {
		String alias = getAliasForObject(classInfo.getFullName());
		if (alias != null) {
			return alias;
		}
		return makeNameForCheckedClass(classInfo);
	}

	private static String fromName(String name) {
		if (name == null || name.isEmpty()) {
			return null;
		}
		if (name.toUpperCase().equals(name)) {
			// all characters are upper case
			return name.toLowerCase();
		}
		String v1 = Character.toLowerCase(name.charAt(0)) + name.substring(1);
		if (!v1.equals(name)) {
			return v1;
		}
		if (name.length() < 3) {
			return name + "Var";
		}
		return null;
	}

	private static String getAliasForObject(String name) {
		return OBJ_ALIAS.get(name);
	}

	private String makeNameFromInvoke(SSAVar ssaVar, InvokeNode inv) {
		MethodInfo callMth = inv.getCallMth();
		String name = callMth.getAlias();
		ClassInfo declClass = callMth.getDeclClass();
		if ("getInstance".equals(name)) {
			// e.g. Cipher.getInstance
			return makeNameForClass(declClass);
		}
		String shortName = cutPrefix(name);
		if (shortName != null) {
			return fromName(shortName);
		}
		if ("iterator".equals(name)) {
			return "it";
		}
		if ("toString".equals(name)) {
			return makeNameForClass(declClass);
		}
		if ("forName".equals(name) && declClass.getType().equals(ArgType.CLASS)) {
			return OBJ_ALIAS.get(Consts.CLASS_CLASS);
		}
		// use method name as a variable name not the best idea in most cases
		if (!GOOD_VAR_NAMES.contains(name)) {
			String typeName = makeNameForType(ssaVar.getCodeVar().getType());
			if (!typeName.equalsIgnoreCase(name)) {
				return typeName + StringUtils.capitalizeFirstChar(name);
			}
		}
		return name;
	}

	private @Nullable String cutPrefix(String name) {
		for (String prefix : INVOKE_PREFIXES) {
			if (name.startsWith(prefix)) {
				return name.substring(prefix.length());
			}
		}
		return null;
	}

	@Override
	public String getName() {
		return "ApplyVariableNames";
	}
}
