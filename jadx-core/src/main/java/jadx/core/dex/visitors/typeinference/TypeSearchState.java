package jadx.core.dex.visitors.typeinference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class TypeSearchState {

	private final Map<SSAVar, TypeSearchVarInfo> varInfoMap;

	public TypeSearchState(MethodNode mth) {
		List<SSAVar> vars = mth.getSVars();
		this.varInfoMap = new LinkedHashMap<>(vars.size());
		for (SSAVar var : vars) {
			varInfoMap.put(var, new TypeSearchVarInfo(var));
		}
	}

	@NotNull
	public TypeSearchVarInfo getVarInfo(SSAVar var) {
		TypeSearchVarInfo varInfo = this.varInfoMap.get(var);
		if (varInfo == null) {
			throw new JadxRuntimeException("TypeSearchVarInfo not found in map for var: " + var);
		}
		return varInfo;
	}

	public ArgType getArgType(InsnArg arg) {
		if (arg.isRegister()) {
			RegisterArg reg = (RegisterArg) arg;
			return getVarInfo(reg.getSVar()).getCurrentType();
		}
		return arg.getType();
	}

	public List<TypeSearchVarInfo> getAllVars() {
		return new ArrayList<>(varInfoMap.values());
	}

	public List<TypeSearchVarInfo> getUnresolvedVars() {
		return varInfoMap.values().stream()
				.filter(varInfo -> !varInfo.isTypeResolved())
				.collect(Collectors.toList());
	}

	public List<TypeSearchVarInfo> getResolvedVars() {
		return varInfoMap.values().stream()
				.filter(TypeSearchVarInfo::isTypeResolved)
				.collect(Collectors.toList());
	}
}
