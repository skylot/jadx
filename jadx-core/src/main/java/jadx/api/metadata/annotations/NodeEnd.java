package jadx.api.metadata.annotations;

import jadx.api.metadata.ICodeAnnotation;

public class NodeEnd implements ICodeAnnotation {

	public static final NodeEnd VALUE = new NodeEnd();

	private NodeEnd() {
	}

	@Override
	public AnnType getAnnType() {
		return AnnType.END;
	}

	@Override
	public String toString() {
		return "END";
	}
}
