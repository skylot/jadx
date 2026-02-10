package jadx.core.utils;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import jadx.api.ICodeWriter;
import jadx.api.JavaMethod;
import jadx.api.impl.SimpleCodeWriter;
import jadx.api.plugins.input.data.IMethodRef;
import jadx.core.codegen.MethodGen;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.SwitchRegion;
import jadx.core.dex.regions.SynchronizedRegion;
import jadx.core.dex.regions.TryCatchRegion;
import jadx.core.dex.regions.conditions.IfRegion;
import jadx.core.dex.regions.loops.LoopRegion;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.visitors.SaveCode;
import jadx.core.utils.files.FileUtils;

import static jadx.core.codegen.MethodGen.FallbackOption.BLOCK_DUMP;

public class DotGraphUtils {
	private static final String NL = "\\l";
	private static final String NLQR = Matcher.quoteReplacement(NL);
	private static final boolean PRINT_DOMINATORS = false;
	private static final boolean PRINT_DOMINATORS_INFO = false;
	private static final int MAX_REGION_NAME_LENGTH = 2000;

	private final ICodeWriter dot = new SimpleCodeWriter();
	private final ICodeWriter conn = new SimpleCodeWriter();

	private final boolean useRegions;
	private final boolean rawInsn;

	// if present, this region and it's children will still be drawn when not in regions mode.
	private Optional<IRegion> highlightRegion;

	// flag set when the highlighted region has been processed once, to avoid processing it's children
	// more than once
	private boolean processedHighlightRegion = false;

	public DotGraphUtils(boolean useRegions, boolean rawInsn) {
		this(useRegions, rawInsn, Optional.empty());
	}

	public DotGraphUtils(boolean useRegions, boolean rawInsn, Optional<IRegion> highlightRegion) {
		this.useRegions = useRegions;
		this.rawInsn = rawInsn;
		this.highlightRegion = highlightRegion;
	}

	// The default out directory for the method
	public static File getOutDir(MethodNode mth) {
		return mth.root().getArgs().getOutDir();
	}

	// The filename the method cfg would be stored in under the default out directory
	public File getFullFile(MethodNode mth) {
		return getFullFile(mth, getOutDir(mth));
	}

	// The filename the method cfg would be stored in under the given out directory
	public File getFullFile(MethodNode mth, File outDir) {
		String fileName = StringUtils.escape(mth.getMethodInfo().getShortId())
				+ (useRegions ? ".regions" : "")
				+ (rawInsn ? ".raw" : "")
				+ ".dot";
		File file = outDir.toPath()
				.resolve(mth.getParentClass().getClassInfo().getAliasFullPath() + "_graphs")
				.resolve(fileName)
				.toFile();
		file = FileUtils.cutFileName(file);
		return file;
	}

	public void dumpToFile(MethodNode mth) {
		File dir = getOutDir(mth);
		dumpToFile(mth, dir);
	}

	public void dumpToFile(MethodNode mth, File dir) {
		String graph = dumpToString(mth);

		if (graph == null) {
			return;
		}

		File file = getFullFile(mth, dir);
		SaveCode.save(graph, file);
	}

	public String dumpToString(MethodNode mth) {
		dot.startLine("digraph \"CFG for");
		dot.add(escape(mth.getMethodInfo().getFullId()));
		dot.add("\" {");

		BlockNode enterBlock = mth.getEnterBlock();
		if (useRegions) {
			if (mth.getRegion() == null) {
				return null;
			}
			processMethodRegion(mth);
		} else {
			List<BlockNode> blocks = mth.getBasicBlocks();
			if (blocks == null) {
				InsnNode[] insnArr = mth.getInstructions();
				if (insnArr == null) {
					return null;
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
				if (processedHighlightRegion && highlightRegion.isPresent()
						&& RegionUtils.isRegionContainsBlock(highlightRegion.get(), block)) {
					// Don't process blocks in the highlight region if it's already been processed, since processing the
					// region will already process all it's containing blocks.
					continue;
				}

				processBlock(mth, block);

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

		return dot.finish().getCodeStr();
	}

	private void processMethodRegion(MethodNode mth) {
		Set<IBlock> regionsBlocks = new HashSet<>(mth.getBasicBlocks().size());
		RegionUtils.getAllRegionBlocks(mth.getRegion(), regionsBlocks);
		for (ExceptionHandler handler : mth.getExceptionHandlers()) {
			IContainer handlerRegion = handler.getHandlerRegion();
			if (handlerRegion != null) {
				RegionUtils.getAllRegionBlocks(handlerRegion, regionsBlocks);
			}
		}

		processRegion(mth, mth.getRegion(), regionsBlocks);
		for (ExceptionHandler h : mth.getExceptionHandlers()) {
			if (h.getHandlerRegion() != null) {
				processRegion(mth, h.getHandlerRegion(), regionsBlocks);
			}
		}

		for (BlockNode block : mth.getBasicBlocks()) {
			if (!regionsBlocks.contains(block)) {
				processBlock(mth, block, true, false);
			}
		}
	}

	private void processRegion(MethodNode mth, IContainer region, Set<IBlock> regionsBlocks) {
		if (region instanceof IRegion) {
			IRegion r = (IRegion) region;
			dot.startLine("subgraph " + makeName(region) + " {");
			dot.startLine("color = " + getColorForRegion(r));
			dot.startLine("label = \"").add(truncateRegionName(r));
			dot.add("\";");
			dot.startLine("node [shape=record,color=blue];");

			for (IContainer c : r.getSubBlocks()) {
				processRegion(mth, c, regionsBlocks);
			}

			dot.startLine('}');
		} else if (region instanceof BlockNode) {
			checkAndFixFloatingBlocks(mth, (BlockNode) region, regionsBlocks);
			processBlock(mth, (BlockNode) region);
		} else if (region instanceof IBlock) {
			processIBlock(mth, (IBlock) region);
		}
	}

	private String getColorForRegion(IRegion region) {
		if (region instanceof IfRegion) {
			return "lightgoldenrod3";
		} else if (region instanceof LoopRegion) {
			return "lightpink2";
		} else if (region instanceof SwitchRegion) {
			return "lightsteelblue3";
		} else if (region instanceof SynchronizedRegion) {
			return "mediumpurple3";
		} else if (region instanceof TryCatchRegion) {
			return "olivedrab4";
		} else if (region.contains(AType.EXC_HANDLER)) {
			return "orangered4";
		}
		return "gray";
	}

	private String truncateRegionName(IRegion r) {
		String regionName = r.toString();
		String attrs = attributesString(r);
		if (!attrs.isEmpty()) {
			regionName += " | " + attrs;
		}
		if (regionName.length() > MAX_REGION_NAME_LENGTH) {
			regionName = regionName.substring(0, MAX_REGION_NAME_LENGTH);
			regionName += "...";
		}
		return regionName;
	}

	/**
	 * A block is floating if it exists in no regions at all. These are placed in a region that makes
	 * sense for generation of this graph only, because otherwise the generated graph is unreadable.
	 */
	private void checkAndFixFloatingBlocks(MethodNode mth, BlockNode block, Set<IBlock> regionBlocks) {
		if (regionBlocks == null || regionBlocks.isEmpty()) {
			return;
		}

		// Heuristic: place the floating block in the same region as either it's predecessor or successor,
		// depending on which it has less of. This results in a more readable graph as a block with a single
		// predecessor will be placed near it.
		for (BlockNode floating : block.getSuccessors()) {
			if (!regionBlocks.contains(floating) && floating.getPredecessors().size() <= floating.getSuccessors().size()) {
				// Set true on the pseudoInRegion to draw the block with a dotted outline and apply a marker to it
				// to notify that it isn't actually in this region.
				processBlock(mth, floating, true, true);
				regionBlocks.add(floating);
			}
		}

		for (BlockNode floating : block.getPredecessors()) {
			if (!regionBlocks.contains(floating) && floating.getPredecessors().size() > floating.getSuccessors().size()) {
				processBlock(mth, floating, true, true);
				regionBlocks.add(floating);
			}
		}
	}

	private void processBlock(MethodNode mth, BlockNode block) {
		processBlock(mth, block, false, false);
	}

	private void processBlock(MethodNode mth, BlockNode block, boolean error, boolean pseudoInRegion) {
		if (!processedHighlightRegion && highlightRegion.isPresent()
				&& RegionUtils.isRegionContainsBlock(highlightRegion.get(), block)) {
			processedHighlightRegion = true;
			processRegion(mth, highlightRegion.get(), null);
			return;
		}

		boolean isMthStart = block.contains(AFlag.MTH_ENTER_BLOCK);
		boolean isMthEnd = block.contains(AFlag.MTH_EXIT_BLOCK);

		if (isMthEnd) {
			dot.startLine("subgraph { rank = sink; ");
		}

		dot.startLine(makeName(block));
		dot.add(" [shape=record,");
		if (error) {
			dot.add("color=red,");
		}
		if (pseudoInRegion) {
			dot.add("style = \"filled,dashed\"");
		} else {
			dot.add("style = filled,");
		}
		if (isMthStart || isMthEnd) {
			dot.add("fillcolor = \"#def3fd\",");
		} else {
			dot.add("fillcolor = \"#f8fafb\",");
		}
		dot.add("label=\"{");
		dot.add(String.valueOf(block.getCId())).add("\\:\\ ");
		dot.add(InsnUtils.formatOffset(block.getStartOffset()));
		if (pseudoInRegion) {
			dot.add("\\nNOT IN ANY REGION");
		}

		String attrs = attributesString(block);
		if (!attrs.isEmpty()) {
			dot.add('|').add(attrs);
		}

		if (PRINT_DOMINATORS_INFO) {
			dot.add('|');
			dot.startLine("doms: ").add(escape(block.getDoms()));
			dot.startLine("\\lidom: ").add(escape(block.getIDom()));
			dot.startLine("\\lpost-doms: ").add(escape(block.getPostDoms()));
			dot.startLine("\\lpost-idom: ").add(escape(block.getIPostDom()));
			dot.startLine("\\ldom-f: ").add(escape(block.getDomFrontier()));
			dot.startLine("\\ldoms-on: ").add(escape(Utils.listToString(block.getDominatesOn())));
			dot.startLine("\\l");
		}
		String insns = insertInsns(mth, block);
		if (!insns.isEmpty()) {
			dot.add('|').add(insns);
		}
		dot.add("}\"];");

		if (isMthEnd) {
			dot.add("};");
		}

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

	private void processIBlock(MethodNode mth, IBlock block) {
		processIBlock(mth, block, false);
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
			// For some reason, instructions here get put through an additional step of unescaping
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
		return escape(string, NLQR);
	}

	private String escape(String string, String newline) {
		return string
				.replace("\\", "") // TODO replace \"
				.replace("/", "\\/")
				.replace(">", "\\>").replace("<", "\\<")
				.replace("{", "\\{").replace("}", "\\}")
				.replace("\"", "\\\"")
				.replace("-", "\\-")
				.replace("|", "\\|")
				.replaceAll("\\R", newline);
	}

	// Consistently format names for graphs

	public static String classFormatName(ClassNode cls, boolean longName) {
		return classFormatName(cls.getClassInfo(), longName);
	}

	public static String classFormatName(ClassInfo cls, boolean longName) {
		return longName ? cls.getAliasFullName() : cls.getAliasShortName();
	}

	public static String methodFormatName(JavaMethod javaMethod, boolean longName) {
		return methodFormatName(javaMethod.getMethodNode(), longName);
	}

	public static String methodFormatName(MethodNode methodNode, boolean longName) {
		if (longName) {
			ClassNode parentClass = methodNode.getParentClass();
			List<ArgType> argTypes = methodNode.getArgTypes();
			ArgType retType = methodNode.getReturnType();
			return classFormatName(parentClass, true) + "." + methodFormatName(methodNode, false)
					+ '(' + Utils.listToString(argTypes, ", ", e -> argTypeFormatName(e, parentClass, true)) + "):"
					+ argTypeFormatName(retType, parentClass, true);
		}
		return methodNode.getAlias();
	}

	public static String unresolvedMethodFormatName(IMethodRef methodRef, boolean longName) {
		String name = methodRef.getName();
		if (longName) {
			String className = methodRef.getParentClassType();
			className = Utils.cleanObjectName(className);

			String returnName = methodRef.getReturnType();
			returnName = Utils.smaliNameToJavaName(returnName);

			List<String> argTypes = methodRef.getArgTypes();
			argTypes = argTypes.stream().map(c -> Utils.smaliNameToJavaName(c)).collect(Collectors.toList());

			return String.format("%s.%s(%s):%s", className, name, Utils.listToString(argTypes), returnName);
		}
		return name;
	}

	public static String interfaceFormatName(ArgType iface, ClassNode cls, boolean longName) {
		ClassInfo ifaceInfo = ClassInfo.fromType(cls.root(), iface);
		return longName ? ifaceInfo.getAliasFullName() : ifaceInfo.getAliasShortName();
	}

	public static String argTypeFormatName(ArgType arg, ClassNode cls, boolean longName) {
		if (arg.isObject() && !arg.isGenericType()) {
			ClassNode superCls = cls.root().resolveClass(arg);
			if (superCls != null) {
				return DotGraphUtils.classFormatName(superCls, longName);
			}
		}
		return arg.toString();
	}
}
