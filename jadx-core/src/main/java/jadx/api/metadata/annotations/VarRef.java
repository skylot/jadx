package jadx.api.metadata.annotations;

import jadx.api.metadata.ICodeAnnotation;

/**
 * Variable reference by position of VarNode in code metadata.
 * <br>
 * Because on creation position not yet known,
 * VarRef created using VarNode as a source of ref pos during serialization.
 * <br>
 * On metadata deserialization created with ref pos directly.
 */
public abstract class VarRef implements ICodeAnnotation {

	public static VarRef fromPos(int refPos) {
		if (refPos == 0) {
			throw new IllegalArgumentException("Zero refPos");
		}
		return new FixedVarRef(refPos);
	}

	public static VarRef fromVarNode(VarNode varNode) {
		return new RelatedVarRef(varNode);
	}

	public abstract int getRefPos();

	@Override
	public AnnType getAnnType() {
		return AnnType.VAR_REF;
	}

	public static final class FixedVarRef extends VarRef {
		private final int refPos;

		public FixedVarRef(int refPos) {
			this.refPos = refPos;
		}

		@Override
		public int getRefPos() {
			return refPos;
		}
	}

	public static final class RelatedVarRef extends VarRef {
		private final VarNode varNode;

		public RelatedVarRef(VarNode varNode) {
			this.varNode = varNode;
		}

		@Override
		public int getRefPos() {
			return varNode.getDefPosition();
		}

		@Override
		public String toString() {
			return "VarRef{" + varNode + ", name=" + varNode.getName() + ", mth=" + varNode.getMth() + '}';
		}
	}

	@Override
	public String toString() {
		return "VarRef{" + getRefPos() + '}';
	}
}
