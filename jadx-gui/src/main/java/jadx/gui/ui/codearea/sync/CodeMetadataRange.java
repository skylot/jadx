package jadx.gui.ui.codearea.sync;

import java.util.Map;

import jadx.api.metadata.ICodeAnnotation;

/**
 * Marks the start and end of annotation within a CodeMetadataStorage
 */
public class CodeMetadataRange {
	// Use Map.Entry here because Java has no built in tuple/pair utility
	private final Map.Entry<Integer, ICodeAnnotation> start;
	private final Map.Entry<Integer, ICodeAnnotation> end;

	CodeMetadataRange(
			Map.Entry<Integer, ICodeAnnotation> start,
			Map.Entry<Integer, ICodeAnnotation> end) {
		this.start = start;
		this.end = end;
	}

	Map.Entry<Integer, ICodeAnnotation> getStart() {
		return start;
	}

	Map.Entry<Integer, ICodeAnnotation> getEnd() {
		return end;
	}

	@Override
	public String toString() {
		return "CodeMetadataRange{start="
				+ start.getKey()
				+ "->"
				+ start.getValue()
				+ ",end="
				+ end.getKey()
				+ "->"
				+ end.getValue()
				+ "}";
	}
}
