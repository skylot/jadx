package jadx.core.dex.visitors;

import java.util.function.Consumer;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxException;

public class MethodVisitor implements IDexTreeVisitor {

	private final Consumer<MethodNode> visitor;

	public MethodVisitor(Consumer<MethodNode> visitor) {
		this.visitor = visitor;
	}

	@Override
	public void visit(MethodNode mth) throws JadxException {
		visitor.accept(mth);
	}

	@Override
	public void init(RootNode root) throws JadxException {
	}

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		return true;
	}
}
