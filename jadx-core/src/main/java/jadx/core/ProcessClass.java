package jadx.core;

import org.jetbrains.annotations.NotNull;

import jadx.api.ICodeInfo;
import jadx.core.codegen.CodeGen;
import jadx.core.dex.nodes.ClassNode;
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
		ClassNode topParentClass = cls.getTopParentClass();
		if (topParentClass != cls) {
			process(topParentClass);
			return;
		}
		if (cls.getState().isProcessed()) {
			// nothing to do
			return;
		}
		synchronized (cls.getClassInfo()) {
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
			} catch (Throwable e) {
				ErrorsCounter.classError(cls, e.getClass().getSimpleName(), e);
			}
		}
	}

	@NotNull
	public static ICodeInfo generateCode(ClassNode cls) {
		ClassNode topParentClass = cls.getTopParentClass();
		if (topParentClass != cls) {
			return generateCode(topParentClass);
		}
		try {
			process(cls);
			cls.getDependencies().forEach(ProcessClass::process);

			ICodeInfo code = CodeGen.generate(cls);
			cls.unload();
			return code;
		} catch (Throwable e) {
			throw new JadxRuntimeException("Failed to generate code for class: " + cls.getFullName(), e);
		}
	}
}
