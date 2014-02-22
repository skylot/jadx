package jadx.core;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.visitors.DepthTraverser;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.utils.exceptions.DecodeException;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProcessClass {
	private static final Logger LOG = LoggerFactory.getLogger(ProcessClass.class);

	private ProcessClass() {
	}

	public static void process(ClassNode cls, List<IDexTreeVisitor> passes) {
		try {
			cls.load();
			for (IDexTreeVisitor visitor : passes) {
				DepthTraverser.visit(visitor, cls);
			}
		} catch (DecodeException e) {
			LOG.error("Decode exception: " + cls, e);
		} catch (Exception e) {
			LOG.error("Class process exception: " + cls, e);
		} finally {
			cls.unload();
		}
	}
}
