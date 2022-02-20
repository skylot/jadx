package jadx.cli;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.api.JadxDecompiler;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.visitors.SaveCode;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;

public class SingleClassMode {
	private static final Logger LOG = LoggerFactory.getLogger(SingleClassMode.class);

	public static boolean process(JadxDecompiler jadx, JadxCLIArgs cliArgs) {
		String singleClass = cliArgs.getSingleClass();
		String singleClassOutput = cliArgs.getSingleClassOutput();
		if (singleClass == null && singleClassOutput == null) {
			return false;
		}
		ClassNode clsForProcess;
		if (singleClass != null) {
			clsForProcess = jadx.getRoot().resolveClass(singleClass);
			if (clsForProcess == null) {
				clsForProcess = jadx.getRoot().getClasses().stream()
						.filter(cls -> cls.getClassInfo().getAliasFullName().equals(singleClass))
						.findFirst().orElse(null);
			}
			if (clsForProcess == null) {
				throw new JadxRuntimeException("Input class not found: " + singleClass);
			}
			if (clsForProcess.contains(AFlag.DONT_GENERATE)) {
				throw new JadxRuntimeException("Input class can't be saved by currect jadx settings (marked as DONT_GENERATE)");
			}
			if (clsForProcess.isInner()) {
				clsForProcess = clsForProcess.getTopParentClass();
				LOG.warn("Input class is inner, parent class will be saved: {}", clsForProcess.getFullName());
			}
		} else {
			// singleClassOutput is set
			// expect only one class to be loaded
			List<ClassNode> classes = jadx.getRoot().getClasses().stream()
					.filter(c -> !c.isInner() && !c.contains(AFlag.DONT_GENERATE))
					.collect(Collectors.toList());
			int size = classes.size();
			if (size == 1) {
				clsForProcess = classes.get(0);
			} else {
				throw new JadxRuntimeException("Found " + size + " classes, single class output can't be used");
			}
		}
		ICodeInfo codeInfo;
		try {
			codeInfo = clsForProcess.decompile();
		} catch (Exception e) {
			throw new JadxRuntimeException("Class decompilation failed", e);
		}
		String fileExt = SaveCode.getFileExtension(jadx.getRoot());
		File out;
		if (singleClassOutput == null) {
			out = new File(jadx.getArgs().getOutDirSrc(), clsForProcess.getClassInfo().getAliasFullPath() + fileExt);
		} else {
			if (singleClassOutput.endsWith(fileExt)) {
				// treat as file name
				out = new File(singleClassOutput);
			} else {
				// treat as directory
				out = new File(singleClassOutput, clsForProcess.getShortName() + fileExt);
			}
		}
		File resultOut = FileUtils.prepareFile(out);
		if (clsForProcess.getClassInfo().hasAlias()) {
			LOG.info("Saving class '{}' (alias: '{}') to file '{}'",
					clsForProcess.getClassInfo().getFullName(), clsForProcess.getFullName(), resultOut.getAbsolutePath());
		} else {
			LOG.info("Saving class '{}' to file '{}'", clsForProcess.getFullName(), resultOut.getAbsolutePath());
		}
		SaveCode.save(codeInfo.getCodeStr(), resultOut);
		return true;
	}
}
