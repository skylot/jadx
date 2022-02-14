package jadx.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeInfo;
import jadx.core.codegen.CodeGen;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.LoadStage;
import jadx.core.dex.visitors.DepthTraversal;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.dex.nodes.ProcessState.GENERATED_AND_UNLOADED;
import static jadx.core.dex.nodes.ProcessState.LOADED;
import static jadx.core.dex.nodes.ProcessState.NOT_LOADED;
import static jadx.core.dex.nodes.ProcessState.PROCESS_COMPLETE;
import static jadx.core.dex.nodes.ProcessState.PROCESS_STARTED;

public final class ProcessClass {

	private ProcessClass() {
	}

	@Nullable
	private static ICodeInfo process(ClassNode cls, boolean codegen) {
		if (!codegen && cls.getState() == PROCESS_COMPLETE) {
			// nothing to do
			return null;
		}
		synchronized (cls.getClassInfo()) {
			try {
				if (cls.contains(AFlag.CLASS_DEEP_RELOAD)) {
					cls.remove(AFlag.CLASS_DEEP_RELOAD);
					cls.deepUnload();
					cls.add(AFlag.CLASS_UNLOADED);
				}
				if (cls.contains(AFlag.CLASS_UNLOADED)) {
					cls.root().runPreDecompileStageForClass(cls);
					cls.remove(AFlag.CLASS_UNLOADED);
				}
				if (cls.getState() == GENERATED_AND_UNLOADED) {
					// force loading code again
					cls.setState(NOT_LOADED);
				}
				if (codegen) {
					cls.setLoadStage(LoadStage.CODEGEN_STAGE);
					if (cls.contains(AFlag.RELOAD_AT_CODEGEN_STAGE)) {
						cls.remove(AFlag.RELOAD_AT_CODEGEN_STAGE);
						cls.unload();
					}
				} else {
					cls.setLoadStage(LoadStage.PROCESS_STAGE);
				}
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
				if (codegen) {
					ICodeInfo code = CodeGen.generate(cls);
					if (!cls.contains(AFlag.DONT_UNLOAD_CLASS)) {
						cls.unload();
						cls.setState(GENERATED_AND_UNLOADED);
					}
					return code;
				}
				return null;
			} catch (Throwable e) {
				if (codegen) {
					throw e;
				}
				cls.addError("Class process error: " + e.getClass().getSimpleName(), e);
				return null;
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
			for (ClassNode depCls : cls.getDependencies()) {
				process(depCls, false);
			}
			if (!cls.getCodegenDeps().isEmpty()) {
				process(cls, false);
				for (ClassNode codegenDep : cls.getCodegenDeps()) {
					process(codegenDep, false);
				}
			}
			ICodeInfo code = process(cls, true);
			if (code == null) {
				throw new JadxRuntimeException("Codegen failed");
			}
			return code;
		} catch (Throwable e) {
			throw new JadxRuntimeException("Failed to generate code for class: " + cls.getFullName(), e);
		}
	}
}
