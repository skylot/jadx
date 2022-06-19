package jadx.core.utils.mappings;

import java.util.List;

import jadx.api.metadata.annotations.VarNode;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.MethodNode;

public class DalvikToJavaBytecodeUtils {

	public static Integer getMethodArgLvIndex(VarNode methodArg) {
		MethodNode mth = methodArg.getMth();
		List<SSAVar> ssaVars = mth.getSVars();
		if (!ssaVars.isEmpty()) {
			return methodArg.getReg() - ssaVars.get(0).getRegNum();
		}
		List<VarNode> args = mth.collectArgsWithoutLoading();
		Integer lvIndex = null;
		for (VarNode arg : args) {
			lvIndex = arg.getReg() - args.get(0).getReg() + (mth.getAccessFlags().isStatic() ? 0 : 1);
			if (arg.equals(methodArg)) {
				break;
			}
		}
		return lvIndex;
	}

	public static Integer getMethodArgLvIndex(SSAVar methodArgSsaVar, MethodNode mth) {
		List<SSAVar> ssaVars = mth.getSVars();
		if (!ssaVars.isEmpty()) {
			return methodArgSsaVar.getRegNum() - ssaVars.get(0).getRegNum();
		}
		return null;
	}

}
