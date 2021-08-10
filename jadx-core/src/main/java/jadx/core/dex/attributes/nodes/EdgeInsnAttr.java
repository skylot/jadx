package jadx.core.dex.attributes.nodes;

import java.util.Objects;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.AttrList;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.Edge;
import jadx.core.dex.nodes.InsnNode;

public class EdgeInsnAttr implements IJadxAttribute {

	private final BlockNode start;
	private final BlockNode end;
	private final InsnNode insn;

	public static void addEdgeInsn(Edge edge, InsnNode insn) {
		addEdgeInsn(edge.getSource(), edge.getTarget(), insn);
	}

	public static void addEdgeInsn(BlockNode start, BlockNode end, InsnNode insn) {
		EdgeInsnAttr edgeInsnAttr = new EdgeInsnAttr(start, end, insn);
		if (!start.getAll(AType.EDGE_INSN).contains(edgeInsnAttr)) {
			start.addAttr(AType.EDGE_INSN, edgeInsnAttr);
		}
		if (!end.getAll(AType.EDGE_INSN).contains(edgeInsnAttr)) {
			end.addAttr(AType.EDGE_INSN, edgeInsnAttr);
		}
	}

	private EdgeInsnAttr(BlockNode start, BlockNode end, InsnNode insn) {
		this.start = start;
		this.end = end;
		this.insn = insn;
	}

	@Override
	public AType<AttrList<EdgeInsnAttr>> getAttrType() {
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
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		EdgeInsnAttr that = (EdgeInsnAttr) o;
		return start.equals(that.start)
				&& end.equals(that.end)
				&& insn.isDeepEquals(that.insn);
	}

	@Override
	public int hashCode() {
		return Objects.hash(start, end, insn);
	}

	@Override
	public String toString() {
		return "EDGE_INSN: " + start + "->" + end + ' ' + insn;
	}
}
