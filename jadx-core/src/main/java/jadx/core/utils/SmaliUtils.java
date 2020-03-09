package jadx.core.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;

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

	public static boolean getSmaliCode(DexNode dex, int clsDefOffset, StringWriter stringWriter) {
		try {
			Path path = dex.getDexFile().getPath();
			DexBackedDexFile dexFile = DexFileFactory.loadDexFile(path.toFile(), null);
			DexBackedClassDef dexBackedClassDef = new DexBackedClassDef(dexFile, clsDefOffset, 0);
			getSmaliCode(dexBackedClassDef, stringWriter);
			return true;
		} catch (Exception e) {
			LOG.error("Error generating smali", e);
			stringWriter.append("Error generating smali code: ");
			stringWriter.append(e.getMessage());
			stringWriter.append(System.lineSeparator());
			stringWriter.append(Utils.getStackTrace(e));
			return false;
		}
	}

	private static void getSmaliCode(DexBackedClassDef classDef, StringWriter stringWriter) throws IOException {
		ClassDefinition classDefinition = new ClassDefinition(new BaksmaliOptions(), classDef);
		classDefinition.writeTo(new IndentingWriter(stringWriter));
	}
}
