package jadx.core.codegen;

import jadx.core.Consts;
import jadx.core.deobf.NameMapper;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.NamedArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.utils.Utils;

import java.util.HashSet;
import java.util.Set;

public class NameGen {

	private final Set<String> varNames = new HashSet<String>();
	private final boolean fallback;

	public NameGen(boolean fallback) {
		this.fallback = fallback;
	}

	public String assignArg(RegisterArg arg) {
		String name = makeArgName(arg);
		if (fallback) {
			return name;
		}
		name = getUniqueVarName(name);
		SSAVar sVar = arg.getSVar();
		if (sVar != null) {
			sVar.setName(name);
		}
		return name;
	}

	public String assignNamedArg(NamedArg arg) {
		String name = arg.getName();
		if (fallback) {
			return name;
		}
		name = getUniqueVarName(name);
		arg.setName(name);
		return name;
	}

	public String useArg(RegisterArg arg) {
		String name = makeArgName(arg);
		varNames.add(name);
		return name;
	}

	private String getUniqueVarName(String name) {
		String r = name;
		int i = 2;
		while (varNames.contains(r)) {
			r = name + i;
			i++;
		}
		varNames.add(r);
		return r;
	}

	private String makeArgName(RegisterArg arg) {
		String name = arg.getName();
		if (fallback) {
			String base = "r" + arg.getRegNum();
			if (name != null) {
				return base + "_" + name;
			}
			return base;
		}
		String varName;
		if (name != null) {
			if (name.equals("this")) {
				return name;
			}
			varName = name;
		} else {
			varName = makeNameForType(arg.getType());
		}
		if (NameMapper.isReserved(varName)) {
			return varName + "R";
		}
		return varName;
	}

	private static String makeNameForType(ArgType type) {
		if (type.isPrimitive()) {
			return makeNameForPrimitive(type);
		} else if (type.isArray()) {
			return makeNameForType(type.getArrayRootElement()) + "Arr";
		} else {
			return makeNameForObject(type);
		}
	}

	private static String makeNameForPrimitive(ArgType type) {
		return type.getPrimitiveType().getShortName().toLowerCase();
	}

	private static String makeNameForObject(ArgType type) {
		if (type.isObject()) {
			String obj = type.getObject();
			if (obj.startsWith("java.lang.")) {
				if (obj.equals(Consts.CLASS_STRING)) {
					return "str";
				}
				if (obj.equals(Consts.CLASS_OBJECT)) {
					return "obj";
				}
				if (obj.equals(Consts.CLASS_CLASS)) {
					return "cls";
				}
				if (obj.equals(Consts.CLASS_THROWABLE)) {
					return "th";
				}
			}
			ClassInfo clsInfo = ClassInfo.fromType(type);
			String shortName = clsInfo.getShortName();
			if (shortName.toUpperCase().equals(shortName)) {
				// all characters are upper case
				return shortName.toLowerCase();
			}
			if (!shortName.isEmpty()) {
				String v1 = Character.toLowerCase(shortName.charAt(0)) + shortName.substring(1);
				if (!v1.equals(shortName)) {
					return v1;
				}
			}
		}
		return Utils.escape(type.toString());
	}
}
