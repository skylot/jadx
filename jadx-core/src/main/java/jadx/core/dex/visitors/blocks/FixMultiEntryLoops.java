package jadx.core.dex.visitors.blocks;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.SpecialEdgeAttr;
import jadx.core.dex.attributes.nodes.SpecialEdgeAttr.SpecialEdgeType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.ListUtils;

public class FixMultiEntryLoops {

	public static boolean process(MethodNode mth) {
		try {
			detectSpecialEdges(mth);
		} catch (Exception e) {
			mth.addWarnComment("Failed to detect multi-entry loops", e);
			return false;
		}
		List<SpecialEdgeAttr> specialEdges = mth.getAll(AType.SPECIAL_EDGE);
		List<SpecialEdgeAttr> multiEntryLoops = specialEdges.stream()
				.filter(e -> e.getType() == SpecialEdgeType.BACK_EDGE && !isSingleEntryLoop(e))
				.collect(Collectors.toList());
		if (multiEntryLoops.isEmpty()) {
			return false;
		}
		try {
			List<SpecialEdgeAttr> crossEdges = ListUtils.filter(specialEdges, e -> e.getType() == SpecialEdgeType.CROSS_EDGE);
			boolean changed = false;
			for (SpecialEdgeAttr backEdge : multiEntryLoops) {
				changed |= fixLoop(mth, backEdge, crossEdges);
			}
			return changed;
		} catch (Exception e) {
			mth.addWarnComment("Failed to fix multi-entry loops", e);
			return false;
		}
	}

	private static boolean fixLoop(MethodNode mth, SpecialEdgeAttr backEdge, List<SpecialEdgeAttr> crossEdges) {
		if (isHeaderSuccessorEntry(mth, backEdge, crossEdges)) {
			return true;
		}
		if (isEndBlockEntry(mth, backEdge, crossEdges)) {
			return true;
		}
		mth.addWarnComment("Unsupported multi-entry loop pattern (" + backEdge + "). Please report as a decompilation issue!!!");
		return false;
	}

	private static boolean isHeaderSuccessorEntry(MethodNode mth, SpecialEdgeAttr backEdge, List<SpecialEdgeAttr> crossEdges) {
		BlockNode header = backEdge.getEnd();
		BlockNode headerIDom = header.getIDom();
		SpecialEdgeAttr subEntry = ListUtils.filterOnlyOne(crossEdges, e -> e.getStart() == headerIDom);
		if (subEntry == null || !ListUtils.isSingleElement(header.getSuccessors(), subEntry.getEnd())) {
			return false;
		}
		BlockNode loopEnd = backEdge.getStart();
		BlockNode subEntryBlock = subEntry.getEnd();
		BlockNode copyHeader = BlockSplitter.insertBlockBetween(mth, loopEnd, header);
		BlockSplitter.copyBlockData(header, copyHeader);
		BlockSplitter.replaceConnection(copyHeader, header, subEntryBlock);
		mth.addDebugComment("Duplicate block (" + header + ") to fix multi-entry loop: " + backEdge);
		return true;
	}

	private static boolean isEndBlockEntry(MethodNode mth, SpecialEdgeAttr backEdge, List<SpecialEdgeAttr> crossEdges) {
		BlockNode loopEnd = backEdge.getStart();
		SpecialEdgeAttr subEntry = ListUtils.filterOnlyOne(crossEdges, e -> e.getEnd() == loopEnd);
		if (subEntry == null) {
			return false;
		}
		dupPath(mth, subEntry.getStart(), loopEnd, backEdge.getEnd());
		mth.addDebugComment("Duplicate block (" + loopEnd + ") to fix multi-entry loop: " + backEdge);
		return true;
	}

	/**
	 * Duplicate 'center' block on path from 'start' to 'end'
	 */
	private static void dupPath(MethodNode mth, BlockNode start, BlockNode center, BlockNode end) {
		BlockNode copyCenter = BlockSplitter.insertBlockBetween(mth, start, end);
		BlockSplitter.copyBlockData(center, copyCenter);
		BlockSplitter.removeConnection(start, center);
	}

	private static boolean isSingleEntryLoop(SpecialEdgeAttr e) {
		BlockNode header = e.getEnd();
		BlockNode loopEnd = e.getStart();
		return header == loopEnd
				|| loopEnd.getDoms().get(header.getId()); // header dominates loop end
	}

	private enum BlockColor {
		WHITE, GRAY, BLACK
	}

	private static void detectSpecialEdges(MethodNode mth) {
		BlockColor[] colors = new BlockColor[mth.getBasicBlocks().size()];
		Arrays.fill(colors, BlockColor.WHITE);
		colorDFS(mth, colors, mth.getEnterBlock());
	}

	// TODO: transform to non-recursive form
	private static void colorDFS(MethodNode mth, BlockColor[] colors, BlockNode block) {
		colors[block.getId()] = BlockColor.GRAY;
		for (BlockNode v : block.getSuccessors()) {
			switch (colors[v.getId()]) {
				case WHITE:
					colorDFS(mth, colors, v);
					break;
				case GRAY:
					mth.addAttr(AType.SPECIAL_EDGE, new SpecialEdgeAttr(SpecialEdgeType.BACK_EDGE, block, v));
					break;
				case BLACK:
					mth.addAttr(AType.SPECIAL_EDGE, new SpecialEdgeAttr(SpecialEdgeType.CROSS_EDGE, block, v));
					break;
			}
		}
		colors[block.getId()] = BlockColor.BLACK;
	}
}
