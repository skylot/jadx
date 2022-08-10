package jadx.core.dex.visitors.finaly;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.utils.Utils;

public class FinallyExtractInfo {
	private final MethodNode mth;
	private final ExceptionHandler finallyHandler;
	private final List<BlockNode> allHandlerBlocks;
	private final List<InsnsSlice> duplicateSlices = new ArrayList<>();
	private final Set<BlockNode> checkedBlocks = new HashSet<>();
	private final InsnsSlice finallyInsnsSlice = new InsnsSlice();
	private final BlockNode startBlock;

	private InsnsSlice curDupSlice;
	private List<InsnNode> curDupInsns;
	private int curDupInsnsOffset;

	public FinallyExtractInfo(MethodNode mth, ExceptionHandler finallyHandler, BlockNode startBlock, List<BlockNode> allHandlerBlocks) {
		this.mth = mth;
		this.finallyHandler = finallyHandler;
		this.startBlock = startBlock;
		this.allHandlerBlocks = allHandlerBlocks;
	}

	public MethodNode getMth() {
		return mth;
	}

	public ExceptionHandler getFinallyHandler() {
		return finallyHandler;
	}

	public List<BlockNode> getAllHandlerBlocks() {
		return allHandlerBlocks;
	}

	public InsnsSlice getFinallyInsnsSlice() {
		return finallyInsnsSlice;
	}

	public List<InsnsSlice> getDuplicateSlices() {
		return duplicateSlices;
	}

	public Set<BlockNode> getCheckedBlocks() {
		return checkedBlocks;
	}

	public BlockNode getStartBlock() {
		return startBlock;
	}

	public InsnsSlice getCurDupSlice() {
		return curDupSlice;
	}

	public void setCurDupSlice(InsnsSlice curDupSlice) {
		this.curDupSlice = curDupSlice;
	}

	public List<InsnNode> getCurDupInsns() {
		return curDupInsns;
	}

	public int getCurDupInsnsOffset() {
		return curDupInsnsOffset;
	}

	public void setCurDupInsns(List<InsnNode> insns, int offset) {
		this.curDupInsns = insns;
		this.curDupInsnsOffset = offset;
	}

	@Override
	public String toString() {
		return "FinallyExtractInfo{"
				+ "\n finally:\n  " + finallyInsnsSlice
				+ "\n dups:\n  " + Utils.listToString(duplicateSlices, "\n  ")
				+ "\n}";
	}
}
