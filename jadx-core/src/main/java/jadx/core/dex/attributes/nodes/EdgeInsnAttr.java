package jadx.core.dex.attributes.nodes;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.AttrList;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;

public class EdgeInsnAttr implements IAttribute {

	private final BlockNode start;
	private final BlockNode end;
	private final InsnNode insn;

	public static void addEdgeInsn(BlockNode start, BlockNode end, InsnNode insn) {
		EdgeInsnAttr edgeInsnAttr = new EdgeInsnAttr(start, end, insn);
		start.addAttr(AType.EDGE_INSN, edgeInsnAttr);
		end.addAttr(AType.EDGE_INSN, edgeInsnAttr);
	}

	public EdgeInsnAttr(BlockNode start, BlockNode end, InsnNode insn) {
		this.start = start;
		this.end = end;
		this.insn = insn;
	}

	@Override
	public AType<AttrList<EdgeInsnAttr>> getType() {
		return AType.EDGE_INSN;
	}

	public BlockNode getStart() {
		return start;
	}

	public BlockNode getEnd() {
		return end;
	}

	public InsnNode getInsn() {
		return insn;
	}

	@Override
	public String toString() {
		return "EDGE_INSN: " + start + "->" + end + " " + insn;
	}
}
