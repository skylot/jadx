package jadx.api.impl.passes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.pass.JadxPass;
import jadx.api.plugins.pass.types.JadxDecompilePass;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.exceptions.JadxException;

public class DecompilePassWrapper extends AbstractVisitor implements IPassWrapperVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(DecompilePassWrapper.class);

	private final JadxDecompilePass decompilePass;

	public DecompilePassWrapper(JadxDecompilePass decompilePass) {
		this.decompilePass = decompilePass;
	}

	@Override
	public JadxPass getPass() {
		return decompilePass;
	}

	@Override
	public void init(RootNode root) throws JadxException {
		try {
			decompilePass.init(root);
		} catch (StackOverflowError | Exception e) {
			LOG.error("Error in decompile pass init: {}", this, e);
		}
	}

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		try {
			return decompilePass.visit(cls);
		} catch (StackOverflowError | Exception e) {
			cls.addError("Error in decompile pass: " + this, e);
			return false;
		}
	}

	@Override
	public void visit(MethodNode mth) throws JadxException {
		try {
			decompilePass.visit(mth);
		} catch (StackOverflowError | Exception e) {
			mth.addError("Error in decompile pass: " + this, e);
		}
	}

	@Override
	public String getName() {
		return decompilePass.getInfo().getName();
	}
}
