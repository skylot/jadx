package jadx.core.dex.visitors;

import java.util.HashSet;
import java.util.Set;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.CodeVar;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.ssa.SSATransform;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "InitCodeVariables",
		desc = "Initialize code variables",
		runAfter = SSATransform.class
)
public class InitCodeVariables extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		initCodeVars(mth);
	}

	private static void initCodeVars(MethodNode mth) {
		for (RegisterArg mthArg : mth.getArguments(true)) {
			initCodeVar(mthArg.getSVar());
		}
		for (SSAVar ssaVar : mth.getSVars()) {
			initCodeVar(ssaVar);
		}
	}

	public static void initCodeVar(SSAVar ssaVar) {
		if (ssaVar.isCodeVarSet()) {
			return;
		}
		CodeVar codeVar = new CodeVar();
		codeVar.setType(ssaVar.getTypeInfo().getType());
		RegisterArg assignArg = ssaVar.getAssign();
		if (assignArg.contains(AFlag.THIS)) {
			codeVar.setName(RegisterArg.THIS_ARG_NAME);
			codeVar.setThis(true);
		}
		if (assignArg.contains(AFlag.METHOD_ARGUMENT) || assignArg.contains(AFlag.CUSTOM_DECLARE)) {
			codeVar.setDeclared(true);
		}

		setCodeVar(ssaVar, codeVar);
	}

	private static void setCodeVar(SSAVar ssaVar, CodeVar codeVar) {
		ssaVar.setCodeVar(codeVar);
		PhiInsn usedInPhi = ssaVar.getUsedInPhi();
		if (usedInPhi != null) {
			Set<SSAVar> vars = new HashSet<>();
			collectConnectedVars(usedInPhi, vars);
			vars.forEach(var -> {
				if (var.isCodeVarSet()) {
					codeVar.mergeFlagsFrom(var.getCodeVar());
				}
				var.setCodeVar(codeVar);
			});
		}
	}

	private static void collectConnectedVars(PhiInsn phiInsn, Set<SSAVar> vars) {
		if (phiInsn == null) {
			return;
		}
		SSAVar resultVar = phiInsn.getResult().getSVar();
		if (vars.add(resultVar)) {
			collectConnectedVars(resultVar.getUsedInPhi(), vars);
		}
		phiInsn.getArguments().forEach(arg -> {
			SSAVar sVar = ((RegisterArg) arg).getSVar();
			if (vars.add(sVar)) {
				collectConnectedVars(sVar.getUsedInPhi(), vars);
			}
		});
	}
}
