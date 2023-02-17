package jadx.core.dex.visitors;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jadx.api.ICodeWriter;
import jadx.api.impl.SimpleCodeWriter;
import jadx.core.codegen.MethodGen;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.StringUtils;
import jadx.core.utils.Utils;

import static jadx.core.codegen.MethodGen.FallbackOption.BLOCK_DUMP;

public class DotGraphVisitor extends AbstractVisitor {

	private static final String NL = "\\l";
	private static final boolean PRINT_DOMINATORS = false;
	private static final boolean PRINT_DOMINATORS_INFO = false;

	private final boolean useRegions;
	private final boolean rawInsn;

	public static DotGraphVisitor dump() {
		return new DotGraphVisitor(false, false);
	}

	public static DotGraphVisitor dumpRaw() {
		return new DotGraphVisitor(false, true);
	}

	public static DotGraphVisitor dumpRegions() {
		return new DotGraphVisitor(true, false);
	}

	public static DotGraphVisitor dumpRawRegions() {
		return new DotGraphVisitor(true, true);
	}

	private DotGraphVisitor(boolean useRegions, boolean rawInsn) {
		this.useRegions = useRegions;
		this.rawInsn = rawInsn;
	}

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		File outRootDir = mth.root().getArgs().getOutDir();
		new DumpDotGraph(outRootDir).process(mth);
	}

	public void save(File dir, MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		new DumpDotGraph(dir).process(mth);
	}

	private class DumpDotGraph {
		private final ICodeWriter dot = new SimpleCodeWriter();
		private final ICodeWriter conn = new SimpleCodeWriter();
		private final File dir;

		public DumpDotGraph(File dir) {
			this.dir = dir;
		}

		public void process(MethodNode mth) {
			dot.startLine("digraph \"CFG for");
			dot.add(escape(mth.getParentClass() + "." + mth.getMethodInfo().getShortId()));
			dot.add("\" {");

			BlockNode enterBlock = mth.getEnterBlock();
			if (useRegions) {
				if (mth.getRegion() == null) {
					return;
				}
				processMethodRegion(mth);
			} else {
				List<BlockNode> blocks = mth.getBasicBlocks();
				if (blocks == null) {
					InsnNode[] insnArr = mth.getInstructions();
					if (insnArr == null) {
						return;
					}
					BlockNode block = new BlockNode(0, 0, 0);
					List<InsnNode> insnList = block.getInstructions();
					for (InsnNode insn : insnArr) {
						if (insn != null) {
							insnList.add(insn);
						}
					}
					enterBlock = block;
					blocks = Collections.singletonList(block);
				}
				for (BlockNode block : blocks) {
					processBlock(mth, block, false);
				}
			}

			dot.startLine("MethodNode[shape=record,label=\"{");
			dot.add(escape(mth.getAccessFlags().makeString(true)));
			dot.add(escape(mth.getReturnType() + " "
					+ mth.getParentClass() + '.' + mth.getName()
					+ '(' + Utils.listToString(mth.getAllArgRegs()) + ") "));

			String attrs = attributesString(mth);
			if (!attrs.isEmpty()) {
				dot.add(" | ").add(attrs);
			}
			dot.add("}\"];");

			dot.startLine("MethodNode -> ").add(makeName(enterBlock)).add(';');

			dot.add(conn.toString());

			dot.startLine('}');
			dot.startLine();

			String fileName = StringUtils.escape(mth.getMethodInfo().getShortId())
					+ (useRegions ? ".regions" : "")
					+ (rawInsn ? ".raw" : "")
					+ ".dot";
			File file = dir.toPath()
					.resolve(mth.getParentClass().getClassInfo().getAliasFullPath() + "_graphs")
					.resolve(fileName)
					.toFile();
			SaveCode.save(dot.finish(), file);
		}

		private void processMethodRegion(MethodNode mth) {
			processRegion(mth, mth.getRegion());
			for (ExceptionHandler h : mth.getExceptionHandlers()) {
				if (h.getHandlerRegion() != null) {
					processRegion(mth, h.getHandlerRegion());
				}
			}
			Set<IBlock> regionsBlocks = new HashSet<>(mth.getBasicBlocks().size());
			RegionUtils.getAllRegionBlocks(mth.getRegion(), regionsBlocks);
			for (ExceptionHandler handler : mth.getExceptionHandlers()) {
				IContainer handlerRegion = handler.getHandlerRegion();
				if (handlerRegion != null) {
					RegionUtils.getAllRegionBlocks(handlerRegion, regionsBlocks);
				}
			}
			for (BlockNode block : mth.getBasicBlocks()) {
				if (!regionsBlocks.contains(block)) {
					processBlock(mth, block, true);
				}
			}
		}

		private void processRegion(MethodNode mth, IContainer region) {
			if (region instanceof IRegion) {
				IRegion r = (IRegion) region;
				dot.startLine("subgraph " + makeName(region) + " {");
				dot.startLine("label = \"").add(r.toString());
				String attrs = attributesString(r);
				if (!attrs.isEmpty()) {
					dot.add(" | ").add(attrs);
				}
				dot.add("\";");
				dot.startLine("node [shape=record,color=blue];");

				for (IContainer c : r.getSubBlocks()) {
					processRegion(mth, c);
				}

				dot.startLine('}');
			} else if (region instanceof BlockNode) {
				processBlock(mth, (BlockNode) region, false);
			} else if (region instanceof IBlock) {
				processIBlock(mth, (IBlock) region, false);
			}
		}

		private void processBlock(MethodNode mth, BlockNode block, boolean error) {
			String attrs = attributesString(block);
			dot.startLine(makeName(block));
			dot.add(" [shape=record,");
			if (error) {
				dot.add("color=red,");
			}
			dot.add("label=\"{");
			dot.add(String.valueOf(block.getCId())).add("\\:\\ ");
			dot.add(InsnUtils.formatOffset(block.getStartOffset()));
			if (!attrs.isEmpty()) {
				dot.add('|').add(attrs);
			}
			if (PRINT_DOMINATORS_INFO) {
				dot.add('|');
				dot.startLine("doms: ").add(escape(block.getDoms()));
				dot.startLine("\\lidom: ").add(escape(block.getIDom()));
				dot.startLine("\\ldom-f: ").add(escape(block.getDomFrontier()));
				dot.startLine("\\ldoms-on: ").add(escape(Utils.listToString(block.getDominatesOn())));
				dot.startLine("\\l");
			}
			String insns = insertInsns(mth, block);
			if (!insns.isEmpty()) {
				dot.add('|').add(insns);
			}
			dot.add("}\"];");

			BlockNode falsePath = null;
			InsnNode lastInsn = BlockUtils.getLastInsn(block);
			if (lastInsn != null && lastInsn.getType() == InsnType.IF) {
				falsePath = ((IfNode) lastInsn).getElseBlock();
			}
			for (BlockNode next : block.getSuccessors()) {
				String style = next == falsePath ? "[style=dashed]" : "";
				addEdge(block, next, style);
			}

			if (PRINT_DOMINATORS) {
				for (BlockNode c : block.getDominatesOn()) {
					conn.startLine(block.getCId() + " -> " + c.getCId() + "[color=green];");
				}
				for (BlockNode dom : BlockUtils.bitSetToBlocks(mth, block.getDomFrontier())) {
					conn.startLine("f_" + block.getCId() + " -> f_" + dom.getCId() + "[color=blue];");
				}
			}
		}

		private void processIBlock(MethodNode mth, IBlock block, boolean error) {
			String attrs = attributesString(block);
			dot.startLine(makeName(block));
			dot.add(" [shape=record,");
			if (error) {
				dot.add("color=red,");
			}
			dot.add("label=\"{");
			if (!attrs.isEmpty()) {
				dot.add(attrs);
			}
			String insns = insertInsns(mth, block);
			if (!insns.isEmpty()) {
				dot.add('|').add(insns);
			}
			dot.add("}\"];");
		}

		private void addEdge(BlockNode from, BlockNode to, String style) {
			conn.startLine(makeName(from)).add(" -> ").add(makeName(to));
			conn.add(style);
			conn.add(';');
		}

		private String attributesString(IAttributeNode block) {
			StringBuilder attrs = new StringBuilder();
			for (String attr : block.getAttributesStringsList()) {
				attrs.append(escape(attr)).append(NL);
			}
			return attrs.toString();
		}

		private String makeName(IContainer c) {
			String name;
			if (c instanceof BlockNode) {
				name = "Node_" + ((BlockNode) c).getCId();
			} else if (c instanceof IBlock) {
				name = "Node_" + c.getClass().getSimpleName() + '_' + c.hashCode();
			} else {
				name = "cluster_" + c.getClass().getSimpleName() + '_' + c.hashCode();
			}
			return name;
		}

		private String insertInsns(MethodNode mth, IBlock block) {
			if (rawInsn) {
				StringBuilder sb = new StringBuilder();
				for (InsnNode insn : block.getInstructions()) {
					sb.append(escape(insn)).append(NL);
				}
				return sb.toString();
			} else {
				ICodeWriter code = new SimpleCodeWriter();
				List<InsnNode> instructions = block.getInstructions();
				MethodGen.addFallbackInsns(code, mth, instructions.toArray(new InsnNode[0]), BLOCK_DUMP);
				String str = escape(code.newLine().toString());
				if (str.startsWith(NL)) {
					str = str.substring(NL.length());
				}
				return str;
			}
		}

		private String escape(Object obj) {
			if (obj == null) {
				return "null";
			}
			return escape(obj.toString());
		}

		private String escape(String string) {
			return string
					.replace("\\", "") // TODO replace \"
					.replace("/", "\\/")
					.replace(">", "\\>").replace("<", "\\<")
					.replace("{", "\\{").replace("}", "\\}")
					.replace("\"", "\\\"")
					.replace("-", "\\-")
					.replace("|", "\\|")
					.replace(ICodeWriter.NL, NL)
					.replace("\n", NL);
		}
	}
}
