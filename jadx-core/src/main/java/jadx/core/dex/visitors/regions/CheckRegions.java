package jadx.core.dex.visitors.regions;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeWriter;
import jadx.api.impl.SimpleCodeWriter;
import jadx.core.codegen.InsnGen;
import jadx.core.codegen.MethodGen;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.loops.LoopRegion;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.exceptions.CodegenException;
import jadx.core.utils.exceptions.JadxException;

public class CheckRegions extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(CheckRegions.class);

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()
				|| mth.getRegion() == null
				|| mth.getBasicBlocks().isEmpty()
				|| mth.contains(AType.JADX_ERROR)) {
			return;
		}

		// check if all blocks included in regions
		Set<BlockNode> blocksInRegions = new HashSet<>();
		DepthRegionTraversal.traverse(mth, new AbstractRegionVisitor() {
			@Override
			public void processBlock(MethodNode mth, IBlock container) {
				if (!(container instanceof BlockNode)) {
					return;
				}
				BlockNode block = (BlockNode) container;
				if (blocksInRegions.add(block)) {
					return;
				}
				if (LOG.isDebugEnabled()
						&& !block.contains(AFlag.RETURN)
						&& !block.contains(AFlag.REMOVE)
						&& !block.contains(AFlag.SYNTHETIC)
						&& !block.getInstructions().isEmpty()) {
					LOG.debug("Duplicated block: {} - {}", mth, block);
				}
			}
		});
		if (mth.getBasicBlocks().size() != blocksInRegions.size()) {
			for (BlockNode block : mth.getBasicBlocks()) {
				if (!blocksInRegions.contains(block)
						&& !block.getInstructions().isEmpty()
						&& !block.contains(AFlag.ADDED_TO_REGION)
						&& !block.contains(AFlag.DONT_GENERATE)
						&& !block.contains(AFlag.REMOVE)) {
					String blockCode = getBlockInsnStr(mth, block).replace("*/", "*\\/");
					mth.addWarn("Code restructure failed: missing block: " + block + ", code lost:" + blockCode);
				}
			}
		}

		DepthRegionTraversal.traverse(mth, new AbstractRegionVisitor() {
			@Override
			public boolean enterRegion(MethodNode mth, IRegion region) {
				if (region instanceof LoopRegion) {
					// check loop conditions
					BlockNode loopHeader = ((LoopRegion) region).getHeader();
					if (loopHeader != null && loopHeader.getInstructions().size() != 1) {
						mth.addWarn("Incorrect condition in loop: " + loopHeader);
					}
				}
				return true;
			}
		});
	}

	private static String getBlockInsnStr(MethodNode mth, IBlock block) {
		ICodeWriter code = new SimpleCodeWriter();
		code.incIndent();
		code.newLine();
		MethodGen mg = MethodGen.getFallbackMethodGen(mth);
		InsnGen ig = new InsnGen(mg, true);
		for (InsnNode insn : block.getInstructions()) {
			try {
				ig.makeInsn(insn, code);
			} catch (CodegenException e) {
				// ignore
			}
		}
		code.newLine();
		return code.getCodeStr();
	}
}
