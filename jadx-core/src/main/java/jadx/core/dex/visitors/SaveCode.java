package jadx.core.dex.visitors;

import java.io.File;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.api.JadxArgs;
import jadx.api.plugins.utils.ZipSecurity;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;

public class SaveCode {
	private static final Logger LOG = LoggerFactory.getLogger(SaveCode.class);

	private SaveCode() {
	}

	public static void save(File dir, ClassNode cls, ICodeInfo code) {
		if (cls.contains(AFlag.DONT_GENERATE)) {
			return;
		}
		if (code == null) {
			throw new JadxRuntimeException("Code not generated for class " + cls.getFullName());
		}
		if (code == ICodeInfo.EMPTY) {
			return;
		}
		String codeStr = code.getCodeStr();
		if (codeStr.isEmpty()) {
			return;
		}
		if (cls.root().getArgs().isSkipFilesSave()) {
			return;
		}
		String fileName = cls.getClassInfo().getAliasFullPath() + getFileExtension(cls.root());
		save(codeStr, dir, fileName);
	}

	public static void save(String code, File dir, String fileName) {
		if (!ZipSecurity.isValidZipEntryName(fileName)) {
			return;
		}
		save(code, new File(dir, fileName));
	}

	public static void save(ICodeInfo codeInfo, File file) {
		save(codeInfo.getCodeStr(), file);
	}

	public static void save(String code, File file) {
		File outFile = FileUtils.prepareFile(file);
		try (PrintWriter out = new PrintWriter(outFile, "UTF-8")) {
			out.println(code);
		} catch (Exception e) {
			LOG.error("Save file error", e);
		}
	}

	public static String getFileExtension(RootNode root) {
		JadxArgs.OutputFormatEnum outputFormat = root.getArgs().getOutputFormat();
		switch (outputFormat) {
			case JAVA:
				return ".java";

			case JSON:
				return ".json";

			default:
				throw new JadxRuntimeException("Unknown output format: " + outputFormat);
		}
	}
}
