package jadx.plugins.mappings.utils;

import java.util.ArrayList;
import java.util.List;

import jadx.api.metadata.annotations.VarNode;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.MethodNode;

public class DalvikToJavaBytecodeUtils {

	// ****************************
	// Local variable index
	// ****************************

	// Method args

	public static Integer getMethodArgLvIndex(VarNode methodArg) {
		MethodNode mth = methodArg.getMth();
		Integer lvIndex = getMethodArgLvIndexViaSsaVars(methodArg.getReg(), mth);
		if (lvIndex != null) {
			return lvIndex;
		}
		List<VarNode> args = mth.collectArgsWithoutLoading();
		for (VarNode arg : args) {
			lvIndex = arg.getReg() - args.get(0).getReg() + (mth.getAccessFlags().isStatic() ? 0 : 1);
			if (arg.equals(methodArg)) {
				break;
			}
		}
		return lvIndex;
	}

	public static Integer getMethodArgLvIndex(SSAVar methodArgSsaVar, MethodNode mth) {
		return getMethodArgLvIndexViaSsaVars(methodArgSsaVar.getRegNum(), mth);
	}

	private static Integer getMethodArgLvIndexViaSsaVars(int regNum, MethodNode mth) {
		List<SSAVar> ssaVars = mth.getSVars();
		if (!ssaVars.isEmpty()) {
			return regNum - ssaVars.get(0).getRegNum();
		}
		return null;
	}

	// Method vars

	public static Integer getMethodVarLvIndex(VarNode methodVar) {
		MethodNode mth = methodVar.getMth();
		Integer lvIndex = getMethodVarLvIndexViaSsaVars(methodVar.getReg(), mth);
		if (lvIndex != null) {
			return lvIndex;
		}
		Integer lastArgLvIndex = mth.getAccessFlags().isStatic() ? -1 : 0;
		List<VarNode> args = mth.collectArgsWithoutLoading();
		if (!args.isEmpty()) {
			lastArgLvIndex = getMethodArgLvIndex(args.get(args.size() - 1));
		}
		return lastArgLvIndex + methodVar.getReg() + (mth.getAccessFlags().isStatic() ? 0 : 1);
	}

	public static Integer getMethodVarLvIndex(SSAVar methodVarSsaVar, MethodNode mth) {
		return getMethodVarLvIndexViaSsaVars(methodVarSsaVar.getRegNum(), mth);
	}

	private static Integer getMethodVarLvIndexViaSsaVars(int regNum, MethodNode mth) {
		List<SSAVar> ssaVars = mth.getSVars();
		if (ssaVars.isEmpty()) {
			return null;
		}
		Integer lastArgLvIndex = mth.getAccessFlags().isStatic() ? -1 : 0;
		List<RegisterArg> args = mth.getArgRegs();
		if (!args.isEmpty()) {
			lastArgLvIndex = getMethodArgLvIndexViaSsaVars(args.get(args.size() - 1).getSVar().getRegNum(), mth);
		}
		return lastArgLvIndex + regNum + (mth.getAccessFlags().isStatic() ? 0 : 1);
	}

	// ****************************
	// Local variable table index
	// ****************************

	// Method args

	public static Integer getMethodArgLvtIndex(VarNode methodArg) {
		MethodNode mth = methodArg.getMth();
		int lvtIndex = mth.getAccessFlags().isStatic() ? 0 : 1;
		List<VarNode> args = mth.collectArgsWithoutLoading();
		for (VarNode arg : args) {
			if (arg.equals(methodArg)) {
				return lvtIndex;
			}
			lvtIndex++;
		}
		return null;
	}

	public static Integer getMethodArgLvtIndex(SSAVar methodArgSsaVar, MethodNode mth) {
		List<SSAVar> ssaVars = mth.getSVars();
		if (ssaVars.isEmpty()) {
			return null;
		}
		List<RegisterArg> args = mth.getArgRegs();
		int lvtIndex = mth.getAccessFlags().isStatic() ? 0 : 1;
		for (RegisterArg arg : args) {
			if (arg.getSVar().equals(methodArgSsaVar)) {
				return lvtIndex;
			}
			lvtIndex++;
		}
		return null;
	}

	// Method vars

	// TODO: public static Integer getMethodVarLvtIndex(VarNode methodVar) {}

	public static Integer getMethodVarLvtIndex(SSAVar methodVarSsaVar, MethodNode mth) {
		List<SSAVar> ssaVars = new ArrayList<>(mth.getSVars());
		if (ssaVars.isEmpty()) {
			return null;
		}
		Integer lvtIndex = getMethodArgLvtIndex(methodVarSsaVar, mth);
		if (lvtIndex != null) {
			return lvtIndex;
		}

		lvtIndex = mth.getAccessFlags().isStatic() ? 0 : 1;
		lvtIndex += mth.getArgTypes().size();

		lvtIndex = getMethodArgLvtIndex(methodVarSsaVar, mth) + 1;
		ssaVars.subList(0, ssaVars.indexOf(methodVarSsaVar) + 1).clear();

		int lastRegNum = -1;
		for (SSAVar ssaVar : ssaVars) {
			if (ssaVar.getRegNum() == lastRegNum) {
				// Not present in bytecode
				// System.out.println("Duplicate RegNum: " + ssaVar.getRegNum());
				continue;
			}
			lvtIndex++;
			if (ssaVar.equals(methodVarSsaVar)) {
				return lvtIndex;
			}
			lastRegNum = ssaVar.getRegNum();
		}
		return null;
	}

}
