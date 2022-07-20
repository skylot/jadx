package jadx.api.metadata.annotations;

import java.util.Objects;

import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeNodeRef;

public class NodeDeclareRef implements ICodeAnnotation {

	private final ICodeNodeRef node;

	private int defPos;

	public NodeDeclareRef(ICodeNodeRef node) {
		this.node = Objects.requireNonNull(node);
	}

	public ICodeNodeRef getNode() {
		return node;
	}

	public int getDefPos() {
		return defPos;
	}

	public void setDefPos(int defPos) {
		this.defPos = defPos;
	}

	@Override
	public AnnType getAnnType() {
		return AnnType.DECLARATION;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof NodeDeclareRef)) {
			return false;
		}
		return node.equals(((NodeDeclareRef) o).node);
	}

	@Override
	public int hashCode() {
		return node.hashCode();
	}

	@Override
	public String toString() {
		return "NodeDeclareRef{" + node + '}';
	}
}
