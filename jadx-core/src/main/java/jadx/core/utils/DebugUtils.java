package jadx.core.utils;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.codegen.CodeWriter;
import jadx.core.codegen.InsnGen;
import jadx.core.codegen.MethodGen;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.DotGraphVisitor;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.dex.visitors.regions.DepthRegionTraversal;
import jadx.core.dex.visitors.regions.TracedRegionVisitor;
import jadx.core.utils.exceptions.CodegenException;
import jadx.core.utils.exceptions.JadxException;

@Deprecated
@TestOnly
public class DebugUtils {
	private static final Logger LOG = LoggerFactory.getLogger(DebugUtils.class);

	private DebugUtils() {
	}

	public static void dump(MethodNode mth) {
		dump(mth, "dump");
	}

	public static void dumpRaw(MethodNode mth, String desc) {
		File out = new File("test-graph-" + desc + "-tmp");
		DotGraphVisitor.dumpRaw().save(out, mth);
	}

	public static IDexTreeVisitor dumpRawVisitor(String desc) {
		return new AbstractVisitor() {
			@Override
			public void visit(MethodNode mth) throws JadxException {
				dumpRaw(mth, desc);
			}
		};
	}

	public static void dump(MethodNode mth, String desc) {
		File out = new File("test-graph-" + desc + "-tmp");
		DotGraphVisitor.dump().save(out, mth);
		DotGraphVisitor.dumpRaw().save(out, mth);
		DotGraphVisitor.dumpRegions().save(out, mth);
	}

	public static void printRegionsWithBlock(MethodNode mth, BlockNode block) {
		Set<IRegion> regions = new LinkedHashSet<>();
		DepthRegionTraversal.traverse(mth, new TracedRegionVisitor() {
			@Override
			public void processBlockTraced(MethodNode mth, IBlock container, IRegion currentRegion) {
				if (block.equals(container)) {
					regions.add(currentRegion);
				}
			}
		});
		LOG.debug(" Found block: {} in regions: {}", block, regions);
	}

	public static IDexTreeVisitor printRegionsVisitor() {
		return new AbstractVisitor() {
			@Override
			public void visit(MethodNode mth) throws JadxException {
				printRegions(mth, true);
			}
		};
	}

	public static void printRegions(MethodNode mth) {
		printRegions(mth, false);
	}

	public static void printRegion(MethodNode mth, IRegion region, boolean printInsn) {
		printRegion(mth, region, "", printInsn);
	}

	public static void printRegions(MethodNode mth, boolean printInsns) {
		LOG.debug("|{}", mth);
		printRegion(mth, mth.getRegion(), "|  ", printInsns);
	}

	private static void printRegion(MethodNode mth, IRegion region, String indent, boolean printInsns) {
		LOG.debug("{}{} {}", indent, region, region.getAttributesString());
		indent += "|  ";
		for (IContainer container : region.getSubBlocks()) {
			if (container instanceof IRegion) {
				printRegion(mth, (IRegion) container, indent, printInsns);
			} else {
				LOG.debug("{}{} {}", indent, container, container.getAttributesString());
				if (printInsns && container instanceof IBlock) {
					IBlock block = (IBlock) container;
					printInsns(mth, indent, block);
				}
			}
		}
	}

	private static void printInsns(MethodNode mth, String indent, IBlock block) {
		for (InsnNode insn : block.getInstructions()) {
			try {
				MethodGen mg = MethodGen.getFallbackMethodGen(mth);
				InsnGen ig = new InsnGen(mg, true);
				CodeWriter code = new CodeWriter();
				ig.makeInsn(insn, code);
				String insnStr = code.toString().substring(CodeWriter.NL.length());
				String attrStr = insn.isAttrStorageEmpty() ? "" : '\t' + insn.getAttributesString();
				LOG.debug("{}|> {}{}", indent, insnStr, attrStr);
			} catch (CodegenException e) {
				LOG.debug("{}|>!! {}", indent, insn);
			}
		}
	}

	public static void printMap(String desc, Map<?, ?> map) {
		LOG.debug("Map of {}, size: {}", desc, map.size());
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			LOG.debug("  {} : {}", entry.getKey(), entry.getValue());
		}
	}
}
