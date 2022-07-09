package jadx.api.impl.passes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.pass.types.JadxDecompilePass;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.exceptions.JadxException;

public class DecompilePassWrapper extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(DecompilePassWrapper.class);

	private final JadxDecompilePass decompilePass;

	public DecompilePassWrapper(JadxDecompilePass decompilePass) {
		this.decompilePass = decompilePass;
	}

	@Override
	public void init(RootNode root) throws JadxException {
		try {
			decompilePass.init(root);
		} catch (Throwable e) {
			LOG.error("Error in decompile pass init: {}", this, e);
		}
	}

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		try {
			return decompilePass.visit(cls);
		} catch (Throwable e) {
			LOG.error("Error in decompile pass init: {}", this, e);
			return false;
		}
	}

	@Override
	public void visit(MethodNode mth) throws JadxException {
		try {
			decompilePass.visit(mth);
		} catch (Throwable e) {
			LOG.error("Error in decompile pass: {}", this, e);
		}
	}

	@Override
	public String toString() {
		return decompilePass.getInfo().getName();
	}
}
