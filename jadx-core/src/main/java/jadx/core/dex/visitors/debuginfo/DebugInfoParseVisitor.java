package jadx.core.dex.visitors.debuginfo;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.Consts;
import jadx.core.dex.attributes.nodes.LocalVarsDebugInfoAttr;
import jadx.core.dex.attributes.nodes.RegDebugInfoAttr;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.dex.visitors.blocksmaker.BlockSplitter;
import jadx.core.dex.visitors.ssa.SSATransform;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "Debug Info Parser",
		desc = "Parse debug information (variable names and types, instruction lines)",
		runBefore = {
				BlockSplitter.class,
				SSATransform.class
		}
)
public class DebugInfoParseVisitor extends AbstractVisitor {

	private static final Logger LOG = LoggerFactory.getLogger(DebugInfoParseVisitor.class);

	@Override
	public void visit(MethodNode mth) throws JadxException {
		try {
			int debugOffset = mth.getDebugInfoOffset();
			if (debugOffset > 0 && mth.dex().checkOffset(debugOffset)) {
				processDebugInfo(mth, debugOffset);
			}
		} catch (Exception e) {
			LOG.error("Error to parse debug info: {}", ErrorsCounter.formatMsg(mth, e.getMessage()), e);
		}
	}

	private void processDebugInfo(MethodNode mth, int debugOffset) throws DecodeException {
		InsnNode[] insnArr = mth.getInstructions();
		DebugInfoParser debugInfoParser = new DebugInfoParser(mth, debugOffset, insnArr);
		List<LocalVar> localVars = debugInfoParser.process();
		attachDebugInfo(mth, localVars, insnArr);
		setMethodSourceLine(mth, insnArr);
	}

	private void attachDebugInfo(MethodNode mth, List<LocalVar> localVars, InsnNode[] insnArr) {
		if (localVars.isEmpty()) {
			return;
		}
		if (Consts.DEBUG) {
			LOG.debug("Parsed debug info for {}: ", mth);
			localVars.forEach(v -> LOG.debug("  {}", v));
		}
		localVars.forEach(var -> {
			int start = var.getStartAddr();
			int end = var.getEndAddr();
			RegDebugInfoAttr debugInfoAttr = new RegDebugInfoAttr(var);
			if (start < 0) {
				// attach to method arguments
				for (RegisterArg arg : mth.getArguments(true)) {
					attachDebugInfo(arg, var, debugInfoAttr);
				}
				start = 0;
			}
			for (int i = start; i <= end; i++) {
				InsnNode insn = insnArr[i];
				if (insn != null) {
					attachDebugInfo(insn.getResult(), var, debugInfoAttr);
					for (InsnArg arg : insn.getArguments()) {
						attachDebugInfo(arg, var, debugInfoAttr);
					}
				}
			}
		});

		mth.addAttr(new LocalVarsDebugInfoAttr(localVars));
	}

	private void attachDebugInfo(InsnArg arg, LocalVar var, RegDebugInfoAttr debugInfoAttr) {
		if (arg instanceof RegisterArg) {
			RegisterArg reg = (RegisterArg) arg;
			if (var.getRegNum() == reg.getRegNum()) {
				reg.addAttr(debugInfoAttr);
			}
		}
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
				}
				return;
			}
		}
	}
}
