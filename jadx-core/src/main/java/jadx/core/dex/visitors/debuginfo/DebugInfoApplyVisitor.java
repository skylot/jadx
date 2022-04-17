package jadx.core.dex.visitors.debuginfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.input.data.AccessFlags;
import jadx.api.plugins.input.data.ILocalVar;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.types.MethodParametersAttr;
import jadx.core.Consts;
import jadx.core.deobf.NameMapper;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.LocalVarsDebugInfoAttr;
import jadx.core.dex.attributes.nodes.RegDebugInfoAttr;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.CodeVar;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.Named;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.dex.visitors.ssa.SSATransform;
import jadx.core.dex.visitors.typeinference.TypeInferenceVisitor;
import jadx.core.dex.visitors.typeinference.TypeUpdateResult;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "Debug Info Apply",
		desc = "Apply debug info to registers (type and names)",
		runAfter = {
				SSATransform.class,
				TypeInferenceVisitor.class
		}
)
public class DebugInfoApplyVisitor extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(DebugInfoApplyVisitor.class);

	@Override
	public void visit(MethodNode mth) throws JadxException {
		try {
			if (mth.contains(AType.LOCAL_VARS_DEBUG_INFO)) {
				applyDebugInfo(mth);
				mth.remove(AType.LOCAL_VARS_DEBUG_INFO);
			}
			processMethodParametersAttribute(mth);
		} catch (Exception e) {
			mth.addWarnComment("Failed to apply debug info", e);
		}
	}

	private static void applyDebugInfo(MethodNode mth) {
		if (Consts.DEBUG_TYPE_INFERENCE) {
			LOG.info("Apply debug info for method: {}", mth);
		}
		mth.getSVars().forEach(ssaVar -> searchAndApplyVarDebugInfo(mth, ssaVar));

		fixLinesForReturn(mth);
		fixNamesForPhiInsns(mth);
	}

	private static void searchAndApplyVarDebugInfo(MethodNode mth, SSAVar ssaVar) {
		if (applyDebugInfo(mth, ssaVar, ssaVar.getAssign())) {
			return;
		}
		for (RegisterArg useArg : ssaVar.getUseList()) {
			if (applyDebugInfo(mth, ssaVar, useArg)) {
				return;
			}
		}
		searchDebugInfoByOffset(mth, ssaVar);
	}

	private static void searchDebugInfoByOffset(MethodNode mth, SSAVar ssaVar) {
		LocalVarsDebugInfoAttr debugInfoAttr = mth.get(AType.LOCAL_VARS_DEBUG_INFO);
		if (debugInfoAttr == null) {
			return;
		}
		OptionalInt max = ssaVar.getUseList().stream().mapToInt(DebugInfoApplyVisitor::getInsnOffsetByArg).max();
		if (!max.isPresent()) {
			return;
		}
		int startOffset = getInsnOffsetByArg(ssaVar.getAssign());
		int endOffset = max.getAsInt();
		int regNum = ssaVar.getRegNum();
		for (ILocalVar localVar : debugInfoAttr.getLocalVars()) {
			if (localVar.getRegNum() == regNum) {
				int startAddr = localVar.getStartOffset();
				int endAddr = localVar.getEndOffset();
				if (isInside(startOffset, startAddr, endAddr) || isInside(endOffset, startAddr, endAddr)) {
					if (Consts.DEBUG_TYPE_INFERENCE) {
						LOG.debug("Apply debug info by offset for: {} to {}", ssaVar, localVar);
					}
					ArgType type = DebugInfoAttachVisitor.getVarType(mth, localVar);
					applyDebugInfo(mth, ssaVar, type, localVar.getName());
					break;
				}
			}
		}
	}

	private static boolean isInside(int var, int start, int end) {
		return start <= var && var <= end;
	}

	private static int getInsnOffsetByArg(InsnArg arg) {
		if (arg != null) {
			InsnNode insn = arg.getParentInsn();
			if (insn != null) {
				return insn.getOffset();
			}
		}
		return -1;
	}

	public static boolean applyDebugInfo(MethodNode mth, SSAVar ssaVar, RegisterArg arg) {
		RegDebugInfoAttr debugInfoAttr = arg.get(AType.REG_DEBUG_INFO);
		if (debugInfoAttr == null) {
			return false;
		}
		return applyDebugInfo(mth, ssaVar, debugInfoAttr.getRegType(), debugInfoAttr.getName());
	}

	public static boolean applyDebugInfo(MethodNode mth, SSAVar ssaVar, ArgType type, String varName) {
		TypeUpdateResult result = mth.root().getTypeUpdate().applyWithWiderIgnoreUnknown(mth, ssaVar, type);
		if (result == TypeUpdateResult.REJECT) {
			if (Consts.DEBUG_TYPE_INFERENCE) {
				LOG.debug("Reject debug info of type: {} and name: '{}' for {}, mth: {}", type, varName, ssaVar, mth);
			}
			return false;
		}
		if (NameMapper.isValidAndPrintable(varName)) {
			ssaVar.setName(varName);
		}
		return true;
	}

	/**
	 * Fix debug info for splitter 'return' instructions
	 */
	private static void fixLinesForReturn(MethodNode mth) {
		if (mth.isVoidReturn()) {
			return;
		}
		InsnNode origReturn = null;
		List<InsnNode> newReturns = new ArrayList<>(mth.getPreExitBlocks().size());
		for (BlockNode exit : mth.getPreExitBlocks()) {
			InsnNode ret = BlockUtils.getLastInsn(exit);
			if (ret != null) {
				if (ret.contains(AFlag.ORIG_RETURN)) {
					origReturn = ret;
				} else {
					newReturns.add(ret);
				}
			}
		}
		if (origReturn != null) {
			for (InsnNode ret : newReturns) {
				InsnArg oldArg = origReturn.getArg(0);
				InsnArg newArg = ret.getArg(0);
				if (oldArg.isRegister() && newArg.isRegister()) {
					RegisterArg oldArgReg = (RegisterArg) oldArg;
					RegisterArg newArgReg = (RegisterArg) newArg;
					applyDebugInfo(mth, newArgReg.getSVar(), oldArgReg.getType(), oldArgReg.getName());
				}
				ret.setSourceLine(origReturn.getSourceLine());
			}
		}
	}

	private static void fixNamesForPhiInsns(MethodNode mth) {
		mth.getSVars().forEach(ssaVar -> {
			for (PhiInsn phiInsn : ssaVar.getUsedInPhi()) {
				Set<String> names = new HashSet<>(1 + phiInsn.getArgsCount());
				addArgName(phiInsn.getResult(), names);
				phiInsn.getArguments().forEach(arg -> addArgName(arg, names));
				if (names.size() == 1) {
					setNameForInsn(phiInsn, names.iterator().next());
				} else if (names.size() > 1) {
					mth.addDebugComment("Different variable names in phi insn: " + names + ", use first");
					setNameForInsn(phiInsn, names.iterator().next());
				}
			}
		});
	}

	private static void addArgName(InsnArg arg, Set<String> names) {
		if (arg instanceof Named) {
			String name = ((Named) arg).getName();
			if (name != null) {
				names.add(name);
			}
		}
	}

	private static void setNameForInsn(PhiInsn phiInsn, String name) {
		phiInsn.getResult().setName(name);
		phiInsn.getArguments().forEach(arg -> {
			if (arg instanceof Named) {
				((Named) arg).setName(name);
			}
		});
	}

	private void processMethodParametersAttribute(MethodNode mth) {
		MethodParametersAttr parametersAttr = mth.get(JadxAttrType.METHOD_PARAMETERS);
		if (parametersAttr == null) {
			return;
		}
		try {
			List<MethodParametersAttr.Info> params = parametersAttr.getList();
			if (params.size() != mth.getMethodInfo().getArgsCount()) {
				return;
			}
			int i = 0;
			for (RegisterArg mthArg : mth.getArgRegs()) {
				MethodParametersAttr.Info paramInfo = params.get(i++);
				String name = paramInfo.getName();
				if (NameMapper.isValidAndPrintable(name)) {
					CodeVar codeVar = mthArg.getSVar().getCodeVar();
					codeVar.setName(name);
					if (AccessFlags.hasFlag(paramInfo.getAccFlags(), AccessFlags.FINAL)) {
						codeVar.setFinal(true);
					}
				}
			}
		} catch (Exception e) {
			mth.addWarnComment("Failed to process method parameters attribute: " + parametersAttr.getList(), e);
		}
	}
}
