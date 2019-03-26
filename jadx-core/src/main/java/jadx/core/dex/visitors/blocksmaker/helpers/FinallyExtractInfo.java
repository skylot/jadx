package jadx.core.dex.visitors.blocksmaker.helpers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.trycatch.ExceptionHandler;

public class FinallyExtractInfo {
	private final ExceptionHandler finallyHandler;
	private final List<BlockNode> allHandlerBlocks;
	private final List<InsnsSlice> duplicateSlices = new ArrayList<>();
	private final Set<BlockNode> checkedBlocks = new HashSet<>();
	private final InsnsSlice finallyInsnsSlice = new InsnsSlice();
	private final BlockNode startBlock;

	public FinallyExtractInfo(ExceptionHandler finallyHandler, BlockNode startBlock, List<BlockNode> allHandlerBlocks) {
		this.finallyHandler = finallyHandler;
		this.startBlock = startBlock;
		this.allHandlerBlocks = allHandlerBlocks;
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
}
