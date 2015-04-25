package jadx.core.dex.visitors;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.parser.DebugInfoParser;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.exceptions.JadxException;

public class DebugInfoVisitor extends AbstractVisitor {
	@Override
	public void visit(MethodNode mth) throws JadxException {
		int debugOffset = mth.getDebugInfoOffset();
		if (debugOffset > 0) {
			InsnNode[] insnArr = mth.getInstructions();
			DebugInfoParser debugInfoParser = new DebugInfoParser(mth, debugOffset, insnArr);
			debugInfoParser.process();

			// set method source line from first instruction
			if (insnArr.length != 0) {
				for (InsnNode insn : insnArr) {
					if (insn != null) {
						int line = insn.getSourceLine();
						if (line != 0) {
							mth.setSourceLine(line - 1);
						}
						break;
					}
				}
			}
			if (!mth.getReturnType().equals(ArgType.VOID)) {
				// fix debug info for splitter 'return' instructions
				for (BlockNode exit : mth.getExitBlocks()) {
					InsnNode ret = BlockUtils.getLastInsn(exit);
					if (ret == null) {
						continue;
					}
					InsnNode oldRet = insnArr[ret.getOffset()];
					if (oldRet == ret) {
						continue;
					}
					RegisterArg oldArg = (RegisterArg) oldRet.getArg(0);
					RegisterArg newArg = (RegisterArg) ret.getArg(0);
					newArg.mergeDebugInfo(oldArg.getType(), oldArg.getName());
					ret.setSourceLine(oldRet.getSourceLine());
				}
			}
		}
		mth.unloadInsnArr();
	}
}
