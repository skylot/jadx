package jadx.dex.visitors;

import jadx.codegen.CodeWriter;
import jadx.codegen.MethodGen;
import jadx.dex.attributes.IAttributeNode;
import jadx.dex.nodes.BlockNode;
import jadx.dex.nodes.IContainer;
import jadx.dex.nodes.IRegion;
import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;
import jadx.dex.trycatch.ExceptionHandler;
import jadx.utils.InsnUtils;
import jadx.utils.Utils;

import java.io.File;

public class DotGraphVisitor extends AbstractVisitor {

	private static final String NL = "\\l";
	private static final boolean PRINT_REGISTERS_STATES = false;

	private final File dir;
	private final boolean useRegions;
	private final boolean rawInsn;

	public DotGraphVisitor(File outDir, boolean useRegions, boolean rawInsn) {
		this.dir = outDir;
		this.useRegions = useRegions;
		this.rawInsn = rawInsn;
	}

	public DotGraphVisitor(File outDir, boolean useRegions) {
		this(outDir, useRegions, false);
	}

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode())
			return;

		CodeWriter dot = new CodeWriter();
		CodeWriter conn = new CodeWriter();

		dot.startLine("digraph \"CFG for"
				+ escape(mth.getParentClass().getFullName() + "." + mth.getMethodInfo().getShortId())
				+ "\" {");

		if (useRegions) {
			if (mth.getRegion() == null)
				return;

			processRegion(mth, mth.getRegion(), dot, conn);
			if (mth.getExceptionHandlers() != null) {
				for (ExceptionHandler h : mth.getExceptionHandlers())
					if (h.getHandlerRegion() != null)
						processRegion(mth, h.getHandlerRegion(), dot, conn);
			}
		} else {
			for (BlockNode block : mth.getBasicBlocks())
				processBlock(mth, block, dot, conn);
		}

		String attrs = attributesString(mth);

		dot.startLine("MethodNode[shape=record,label=\"{"
				+ escape(mth.getAccessFlags().makeString())
				+ escape(mth.getMethodInfo().getReturnType() + " "
						+ mth.getParentClass().getFullName() + "." + mth.getName()
						+ "(" + Utils.listToString(mth.getArguments(true)) + ") ")
				+ (attrs.length() == 0 ? "" : " | " + attrs)
				+ (mth.getSuperCall() != null ? "| Super call: " + escape(mth.getSuperCall().toString()) : "")
				+ "}\"];");

		dot.startLine("MethodNode -> " + makeName(mth.getEnterBlock()) + ";");

		dot.add(conn);

		dot.startLine("}");
		dot.startLine();

		String fileName = Utils.escape(mth.getMethodInfo().getShortId())
				+ (useRegions ? ".regions" : "")
				+ (rawInsn ? ".raw" : "")
				+ ".dot";
		dot.save(dir, mth.getParentClass().getClassInfo().getFullPath() + "_graphs", fileName);
	}

	private void processRegion(MethodNode mth, IContainer region, CodeWriter dot, CodeWriter conn) {
		if (region instanceof IRegion) {
			IRegion r = (IRegion) region;
			String attrs = attributesString(r);
			dot.startLine("subgraph " + makeName(region) + " {");
			dot.startLine("label = \"" + r.toString()
					+ (attrs.length() == 0 ? "" : " | " + attrs)
					+ "\";");
			dot.startLine("node [shape=record,color=blue];");

			for (IContainer c : r.getSubBlocks()) {
				processRegion(mth, c, dot, conn);
			}

			dot.startLine("}");
		} else if (region instanceof BlockNode) {
			processBlock(mth, (BlockNode) region, dot, conn);
		}
	}

	private void processBlock(MethodNode mth, BlockNode block, CodeWriter dot, CodeWriter conn) {
		String attrs = attributesString(block);
		if (PRINT_REGISTERS_STATES) {
			if (block.getStartState() != null) {
				if (attrs.length() != 0)
					attrs += "|";
				attrs += escape("RS: " + block.getStartState()) + NL;
				attrs += escape("RE: " + block.getEndState()) + NL;
			}
		}

		String insns = insertInsns(mth, block);

		dot.startLine(makeName(block) + " [shape=record,label=\"{"
				+ block.getId() + "\\:\\ "
				+ InsnUtils.formatOffset(block.getStartOffset())
				+ (attrs.length() == 0 ? "" : "|" + attrs)
				+ (insns.length() == 0 ? "" : "|" + insns)
				+ "}\"];");

		for (BlockNode next : block.getSuccessors())
			conn.startLine(makeName(block) + " -> " + makeName(next) + ";");

		for (BlockNode next : block.getDominatesOn())
			conn.startLine(makeName(block) + " -> " + makeName(next) + "[style=dotted];");

		// // add all dominators connections
		// for (BlockNode next : BlockUtils.bitsetToBlocks(mth, block.getDoms()))
		// conn.startLine(makeName(block) + " -> " + makeName(next) + "[style=dotted, color=green];");
	}

	private String attributesString(IAttributeNode block) {
		StringBuilder attrs = new StringBuilder();
		for (String attr : block.getAttributes().getAttributeStrings()) {
			attrs.append(escape(attr)).append(NL);
		}
		return attrs.toString();
	}

	private String makeName(IContainer c) {
		String name;
		if (c instanceof BlockNode) {
			name = "Node_" + ((BlockNode) c).getId();
		} else {
			name = "cluster_" + c.getClass().getSimpleName() + "_" + c.hashCode();
		}
		return name;
	}

	private String insertInsns(MethodNode mth, BlockNode block) {
		if (rawInsn) {
			StringBuilder str = new StringBuilder();
			for (InsnNode insn : block.getInstructions()) {
				str.append(escape(insn.toString() + " " + insn.getAttributes()));
				str.append(NL);
			}
			return str.toString();
		} else {
			CodeWriter code = new CodeWriter(0);
			MethodGen.makeFallbackInsns(code, mth, block.getInstructions(), false);
			String str = escape(code.endl().toString());
			if (str.startsWith(NL))
				str = str.substring(NL.length());
			return str;
		}
	}

	private String escape(String string) {
		return string
				.replace(">", "\\>").replace("<", "\\<")
				.replace("{", "\\{").replace("}", "\\}")
				.replace("\"", "\\\"")
				.replace("-", "\\-")
				.replace("\n", NL);
	}
}
