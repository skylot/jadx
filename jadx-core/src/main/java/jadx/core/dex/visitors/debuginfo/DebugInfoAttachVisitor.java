package jadx.core.dex.visitors.debuginfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jadx.api.plugins.input.data.IDebugInfo;
import jadx.api.plugins.input.data.ILocalVar;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.nodes.LocalVarsDebugInfoAttr;
import jadx.core.dex.attributes.nodes.RegDebugInfoAttr;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.parser.SignatureParser;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.dex.visitors.blocks.BlockSplitter;
import jadx.core.dex.visitors.ssa.SSATransform;
import jadx.core.utils.ListUtils;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "Debug Info Parser",
		desc = "Attach debug information (variable names and types, instruction lines)",
		runBefore = {
				BlockSplitter.class,
				SSATransform.class
		}
)
public class DebugInfoAttachVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		try {
			IDebugInfo debugInfo = mth.getDebugInfo();
			if (debugInfo != null) {
				processDebugInfo(mth, debugInfo);
			}
		} catch (Exception e) {
			mth.addWarnComment("Failed to parse debug info", e);
		}
	}

	private void processDebugInfo(MethodNode mth, IDebugInfo debugInfo) {
		InsnNode[] insnArr = mth.getInstructions();
		attachSourceLines(mth, debugInfo.getSourceLineMapping(), insnArr);
		attachDebugInfo(mth, debugInfo.getLocalVars(), insnArr);
		setMethodSourceLine(mth, insnArr);
	}

	private void attachSourceLines(MethodNode mth, Map<Integer, Integer> lineMapping, InsnNode[] insnArr) {
		if (lineMapping.isEmpty()) {
			return;
		}
		Map<Integer, Integer> linesStat = new HashMap<>(); // count repeating lines
		for (Map.Entry<Integer, Integer> entry : lineMapping.entrySet()) {
			try {
				Integer offset = entry.getKey();
				InsnNode insn = insnArr[offset];
				if (insn != null) {
					int line = entry.getValue();
					insn.setSourceLine(line);
					if (insn.getType() != InsnType.NOP) {
						linesStat.merge(line, 1, (v, one) -> v + 1);
					}
				}
			} catch (Exception e) {
				mth.addWarnComment("Error attach source line", e);
			}
		}
		// 3 here is allowed maximum for lines repeat,
		// can occur in indexed 'for' loops (3 instructions with same line)
		List<Map.Entry<Integer, Integer>> repeatingLines = ListUtils.filter(linesStat.entrySet(), p -> p.getValue() > 3);
		if (repeatingLines.isEmpty()) {
			mth.add(AFlag.USE_LINES_HINTS);
		} else {
			mth.addDebugComment("Don't trust debug lines info. Repeating lines: " + repeatingLines);
		}
	}

	private void attachDebugInfo(MethodNode mth, List<ILocalVar> localVars, InsnNode[] insnArr) {
		if (localVars.isEmpty()) {
			return;
		}
		for (ILocalVar var : localVars) {
			int regNum = var.getRegNum();
			int start = var.getStartOffset();
			int end = var.getEndOffset();

			ArgType type = getVarType(mth, var);
			RegDebugInfoAttr debugInfoAttr = new RegDebugInfoAttr(type, var.getName());
			if (start <= 0) {
				// attach to method arguments
				RegisterArg thisArg = mth.getThisArg();
				if (thisArg != null) {
					attachDebugInfo(thisArg, debugInfoAttr, regNum);
				}
				for (RegisterArg arg : mth.getArgRegs()) {
					attachDebugInfo(arg, debugInfoAttr, regNum);
				}
				start = 0;
			}
			for (int i = start; i <= end; i++) {
				InsnNode insn = insnArr[i];
				if (insn == null) {
					continue;
				}
				int count = 0;
				for (InsnArg arg : insn.getArguments()) {
					count += attachDebugInfo(arg, debugInfoAttr, regNum);
				}
				if (count != 0) {
					// don't apply same info for result if applied to args
					continue;
				}
				attachDebugInfo(insn.getResult(), debugInfoAttr, regNum);
			}
		}

		mth.addAttr(new LocalVarsDebugInfoAttr(localVars));
	}

	private int attachDebugInfo(InsnArg arg, RegDebugInfoAttr debugInfoAttr, int regNum) {
		if (arg instanceof RegisterArg) {
			RegisterArg reg = (RegisterArg) arg;
			if (regNum == reg.getRegNum()) {
				reg.addAttr(debugInfoAttr);
				return 1;
			}
		}
		return 0;
	}

	public static ArgType getVarType(MethodNode mth, ILocalVar var) {
		ArgType type = ArgType.parse(var.getType());
		String sign = var.getSignature();
		if (sign == null) {
			return type;
		}
		try {
			ArgType gType = new SignatureParser(sign).consumeType();
			ArgType expandedType = mth.root().getTypeUtils().expandTypeVariables(mth, gType);
			if (checkSignature(mth, type, expandedType)) {
				return expandedType;
			}
		} catch (Exception e) {
			mth.addWarnComment("Can't parse signature for local variable: " + sign, e);
		}
		return type;
	}

	private static boolean checkSignature(MethodNode mth, ArgType type, ArgType gType) {
		boolean apply;
		ArgType el = gType.getArrayRootElement();
		if (el.isGeneric()) {
			if (!type.getArrayRootElement().getObject().equals(el.getObject())) {
				mth.addWarnComment("Generic types in debug info not equals: " + type + " != " + gType);
			}
			apply = true;
		} else {
			apply = el.isGenericType();
		}
		return apply;
	}

	/**
	 * Set method source line from first instruction
	 */
	private void setMethodSourceLine(MethodNode mth, InsnNode[] insnArr) {
		for (InsnNode insn : insnArr) {
			if (insn != null) {
				int line = insn.getSourceLine();
				if (line != 0) {
					mth.setSourceLine(line - 1);
					return;
				}
			}
		}
	}
}
