package jadx.api;

import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.FieldNode;

public class JavaField {

	private final FieldNode field;

	public JavaField(FieldNode f) {
		this.field = f;
	}

	public String getName() {
		return field.getName();
	}

	public AccessInfo getAccessFlags() {
		return field.getAccessFlags();
	}

	public ArgType getType() {
		return field.getType();
	}

	public int getDecompiledLine() {
		return field.getDecompiledLine();
	}
}
