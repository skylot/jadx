package jadx.core;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.core.codegen.CodeGen;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.visitors.DepthTraversal;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.utils.ErrorsCounter;

import static jadx.core.dex.nodes.ProcessState.GENERATED;
import static jadx.core.dex.nodes.ProcessState.NOT_LOADED;
import static jadx.core.dex.nodes.ProcessState.PROCESSED;
import static jadx.core.dex.nodes.ProcessState.STARTED;
import static jadx.core.dex.nodes.ProcessState.UNLOADED;

public final class ProcessClass {

	private ProcessClass() {
	}

	public static void process(ClassNode cls, List<IDexTreeVisitor> passes, @Nullable CodeGen codeGen) {
		if (codeGen == null && cls.getState() == PROCESSED) {
			return;
		}
		synchronized (getSyncObj(cls)) {
			try {
				if (cls.getState() == NOT_LOADED) {
					cls.load();
					cls.setState(STARTED);
					for (IDexTreeVisitor visitor : passes) {
						DepthTraversal.visit(visitor, cls);
					}
					cls.setState(PROCESSED);
				}
				if (cls.getState() == PROCESSED && codeGen != null) {
					processDependencies(cls, passes);
					codeGen.visit(cls);
					cls.setState(GENERATED);
				}
			} catch (Exception e) {
				ErrorsCounter.classError(cls, e.getClass().getSimpleName(), e);
			} finally {
				if (cls.getState() == GENERATED) {
					cls.unload();
					cls.setState(UNLOADED);
				}
			}
		}
	}

	public static Object getSyncObj(ClassNode cls) {
		return cls.getClassInfo();
	}

	private static void processDependencies(ClassNode cls, List<IDexTreeVisitor> passes) {
		cls.getDependencies().forEach(depCls -> process(depCls, passes, null));
	}
}
