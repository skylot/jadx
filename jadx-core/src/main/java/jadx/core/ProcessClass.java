package jadx.core;

import org.jetbrains.annotations.NotNull;

import jadx.api.ICodeInfo;
import jadx.core.codegen.CodeGen;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.ProcessState;
import jadx.core.dex.visitors.DepthTraversal;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.dex.nodes.ProcessState.LOADED;
import static jadx.core.dex.nodes.ProcessState.NOT_LOADED;
import static jadx.core.dex.nodes.ProcessState.PROCESS_COMPLETE;
import static jadx.core.dex.nodes.ProcessState.PROCESS_STARTED;

public final class ProcessClass {

	private ProcessClass() {
	}

	public static void process(ClassNode cls) {
		process(cls, false);
	}

	@NotNull
	public static ICodeInfo generateCode(ClassNode cls) {
		ICodeInfo codeInfo = process(cls, true);
		if (codeInfo == null) {
			throw new JadxRuntimeException("Failed to generate code for class: " + cls.getFullName());
		}
		return codeInfo;
	}

	private static ICodeInfo process(ClassNode cls, boolean generateCode) {
		ClassNode topParentClass = cls.getTopParentClass();
		if (topParentClass != cls) {
			return process(topParentClass, generateCode);
		}
		if (!generateCode && cls.getState() == PROCESS_COMPLETE) {
			// nothing to do
			return null;
		}
		synchronized (getSyncObj(cls)) {
			try {
				if (cls.getState() == NOT_LOADED) {
					cls.load();
				}
				if (cls.getState() == LOADED) {
					cls.setState(PROCESS_STARTED);
					for (IDexTreeVisitor visitor : cls.root().getPasses()) {
						DepthTraversal.visit(visitor, cls);
					}
					cls.setState(PROCESS_COMPLETE);
				}
				if (generateCode && cls.getState() == PROCESS_COMPLETE) {
					processDependencies(cls);
					ICodeInfo code = CodeGen.generate(cls);
					cls.setState(ProcessState.GENERATED);
					// TODO: unload class (need to build dependency tree or allow to load class several times)
					return code;
				}
			} catch (Throwable e) {
				ErrorsCounter.classError(cls, e.getClass().getSimpleName(), e);
			}
		}
		return null;
	}

	private static Object getSyncObj(ClassNode cls) {
		return cls.getClassInfo();
	}

	private static void processDependencies(ClassNode cls) {
		for (ClassNode depCls : cls.getDependencies()) {
			process(depCls, false);
		}
	}
}
