package jadx.core.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;
import org.jf.baksmali.Adaptors.ClassDefinition;
import org.jf.baksmali.BaksmaliOptions;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.smali.Smali;
import org.jf.smali.SmaliOptions;
import org.jf.util.IndentingWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.nodes.DexNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

// TODO: move smali dependencies out from jadx-core
public class SmaliUtils {

	private static final Logger LOG = LoggerFactory.getLogger(SmaliUtils.class);

	public static void assembleDex(String outputDexFile, String inputSmali) {
		try {
			SmaliOptions options = new SmaliOptions();
			options.outputDexFile = outputDexFile;
			Smali.assemble(options, inputSmali);
		} catch (Exception e) {
			throw new JadxRuntimeException("Smali assemble error", e);
		}
	}

	@NotNull
	public static String getSmaliCode(DexNode dex, int clsDefOffset) {
		try {
			Path path = dex.getDexFile().getPath();
			DexBackedDexFile dexFile = DexFileFactory.loadDexFile(path.toFile(), null);
			DexBackedClassDef dexBackedClassDef = new DexBackedClassDef(dexFile, clsDefOffset);
			return getSmaliCode(dexBackedClassDef);
		} catch (Exception e) {
			LOG.error("Error generating smali", e);
			return "Error generating smali code: " + e.getMessage()
					+ '\n' + Utils.getStackTrace(e);
		}
	}

	private static String getSmaliCode(DexBackedClassDef classDef) throws IOException {
		ClassDefinition classDefinition = new ClassDefinition(new BaksmaliOptions(), classDef);
		StringWriter sw = new StringWriter();
		classDefinition.writeTo(new IndentingWriter(sw));
		return sw.toString();
	}
}
