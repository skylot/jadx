package jadx.core.dex.visitors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.parser.DebugInfoParser;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxException;

public class DebugInfoVisitor extends AbstractVisitor {

	private static final Logger LOG = LoggerFactory.getLogger(DebugInfoVisitor.class);

	@Override
	public void visit(MethodNode mth) throws JadxException {
		try {
			int debugOffset = mth.getDebugInfoOffset();
			if (debugOffset > 0 && mth.dex().checkOffset(debugOffset)) {
				processDebugInfo(mth, debugOffset);
			}
		} catch (Exception e) {
			LOG.error("Error in debug info parser: {}", ErrorsCounter.formatErrorMsg(mth, e.getMessage()), e);
		} finally {
			mth.unloadInsnArr();
		}
	}

	private void processDebugInfo(MethodNode mth, int debugOffset) throws DecodeException {
		InsnNode[] insnArr = mth.getInstructions();
		DebugInfoParser debugInfoParser = new DebugInfoParser(mth, debugOffset, insnArr);
		debugInfoParser.process();

		if (insnArr.length != 0) {
			setMethodSourceLine(mth, insnArr);
		}
		if (!mth.getReturnType().equals(ArgType.VOID)) {
			setLineForReturn(mth, insnArr);
		}
	}

	/**
	 * Fix debug info for splitter 'return' instructions
	 */
	private void setLineForReturn(MethodNode mth, InsnNode[] insnArr) {
		for (BlockNode exit : mth.getExitBlocks()) {
			InsnNode ret = BlockUtils.getLastInsn(exit);
			if (ret != null) {
				InsnNode oldRet = insnArr[ret.getOffset()];
				if (oldRet != ret) {
					RegisterArg oldArg = (RegisterArg) oldRet.getArg(0);
					RegisterArg newArg = (RegisterArg) ret.getArg(0);
					newArg.mergeDebugInfo(oldArg.getType(), oldArg.getName());
					ret.setSourceLine(oldRet.getSourceLine());
				}
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
