package jadx.core.dex.visitors;

import java.util.function.Consumer;

import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.JadxException;

public class MethodVisitor extends AbstractVisitor {

	private final String name;
	private final Consumer<MethodNode> visitor;

	public MethodVisitor(String name, Consumer<MethodNode> visitor) {
		this.name = name;
		this.visitor = visitor;
	}

	@Override
	public void visit(MethodNode mth) throws JadxException {
		visitor.accept(mth);
	}

	@Override
	public String getName() {
		return name;
	}
}
