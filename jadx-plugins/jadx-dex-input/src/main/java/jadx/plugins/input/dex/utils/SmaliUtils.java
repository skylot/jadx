package jadx.plugins.input.dex.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.android.tools.smali.baksmali.Adaptors.ClassDefinition;
import com.android.tools.smali.baksmali.BaksmaliOptions;
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedClassDef;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;

public class SmaliUtils {
	private static final Logger LOG = LoggerFactory.getLogger(SmaliUtils.class);

	public static String getSmaliCode(byte[] dexBuf, int clsDefOffset) {
		StringWriter stringWriter = new StringWriter();
		try {
			DexBackedDexFile dexFile = new DexBackedDexFile(null, dexBuf);
			DexBackedClassDef dexBackedClassDef = new DexBackedClassDef(dexFile, clsDefOffset, 0);
			ClassDefinition classDefinition = new ClassDefinition(new BaksmaliOptions(), dexBackedClassDef);
			classDefinition.writeTo(new BaksmaliWriter(stringWriter));
		} catch (Exception e) {
			LOG.error("Error generating smali", e);
			stringWriter.append("Error generating smali code: ");
			stringWriter.append(e.getMessage());
			stringWriter.append(System.lineSeparator());
			e.printStackTrace(new PrintWriter(stringWriter, true));
		}
		return stringWriter.toString();
	}
}
