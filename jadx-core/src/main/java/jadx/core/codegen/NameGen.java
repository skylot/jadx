package jadx.core.codegen;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jadx.core.deobf.NameMapper;
import jadx.core.dex.attributes.nodes.LoopLabelAttr;
import jadx.core.dex.instructions.args.CodeVar;
import jadx.core.dex.instructions.args.NamedArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;

public class NameGen {
	private final MethodNode mth;
	private final boolean fallback;
	private final Set<String> varNames = new HashSet<>();

	public NameGen(MethodNode mth, ClassGen classGen) {
		this.mth = mth;
		this.fallback = classGen.isFallbackMode();
		NameGen outerNameGen = classGen.getOuterNameGen();
		if (outerNameGen != null) {
			inheritUsedNames(outerNameGen);
		}
		addNamesUsedInClass();
	}

	public void inheritUsedNames(NameGen otherNameGen) {
		varNames.addAll(otherNameGen.varNames);
	}

	private void addNamesUsedInClass() {
		ClassNode parentClass = mth.getParentClass();
		for (FieldNode field : parentClass.getFields()) {
			if (field.isStatic()) {
				varNames.add(field.getAlias());
			}
		}
		for (ClassNode innerClass : parentClass.getInnerClasses()) {
			varNames.add(innerClass.getClassInfo().getAliasShortName());
		}
		// add all root package names to avoid collisions with full class names
		varNames.addAll(mth.root().getCacheStorage().getRootPkgs());
	}

	public String assignArg(CodeVar var) {
		if (fallback) {
			return getFallbackName(var);
		}
		if (var.isThis()) {
			return RegisterArg.THIS_ARG_NAME;
		}
		String name = getUniqueVarName(makeArgName(var));
		var.setName(name);
		return name;
	}

	public String assignNamedArg(NamedArg arg) {
		String name = arg.getName();
		if (fallback) {
			return name;
		}
		String uniqName = getUniqueVarName(name);
		arg.setName(uniqName);
		return uniqName;
	}

	public String useArg(RegisterArg arg) {
		String name = arg.getName();
		if (name == null || fallback) {
			return getFallbackName(arg);
		}
		return name;
	}

	// TODO: avoid name collision with variables names
	public String getLoopLabel(LoopLabelAttr attr) {
		String name = "loop" + attr.getLoop().getId();
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

	private String makeArgName(CodeVar var) {
		String name = var.getName();
		if (NameMapper.isValidAndPrintable(name)) {
			return name;
		}
		return getFallbackName(var);
	}

	private String getFallbackName(CodeVar var) {
		List<SSAVar> ssaVars = var.getSsaVars();
		if (ssaVars.isEmpty()) {
			return "v";
		}
		return getFallbackName(ssaVars.get(0).getAssign());
	}

	private String getFallbackName(RegisterArg arg) {
		return "r" + arg.getRegNum();
	}
}
