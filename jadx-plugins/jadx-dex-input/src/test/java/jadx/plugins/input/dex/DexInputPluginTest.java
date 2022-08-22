package jadx.plugins.input.dex;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import jadx.api.plugins.input.ICodeLoader;
import jadx.api.plugins.input.data.AccessFlags;
import jadx.api.plugins.input.data.AccessFlagsScope;
import jadx.api.plugins.input.data.ICodeReader;
import jadx.plugins.input.dex.utils.SmaliTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class DexInputPluginTest {

	@Test
	public void loadSampleApk() throws Exception {
		processFile(Paths.get(ClassLoader.getSystemResource("samples/app-with-fake-dex.apk").toURI()));
	}

	@Test
	public void loadHelloWorld() throws Exception {
		processFile(Paths.get(ClassLoader.getSystemResource("samples/hello.dex").toURI()));
	}

	@Test
	public void loadTestSmali() throws Exception {
		processFile(SmaliTestUtils.compileSmaliFromResource("samples/test.smali"));
	}

	private static void processFile(Path sample) throws IOException {
		System.out.println("Input file: " + sample.toAbsolutePath());
		long start = System.currentTimeMillis();
		List<Path> files = Collections.singletonList(sample);
		try (ICodeLoader result = new DexInputPlugin().loadFiles(files)) {
			AtomicInteger count = new AtomicInteger();
			result.visitClasses(cls -> {
				System.out.println();
				System.out.println("Class: " + cls.getType());
				System.out.println("AccessFlags: " + AccessFlags.format(cls.getAccessFlags(), AccessFlagsScope.CLASS));
				System.out.println("SuperType: " + cls.getSuperType());
				System.out.println("Interfaces: " + cls.getInterfacesTypes());
				System.out.println("Attributes: " + cls.getAttributes());
				count.getAndIncrement();

				cls.visitFieldsAndMethods(
						System.out::println,
						mth -> {
							System.out.println("---");
							System.out.println(mth);
							ICodeReader codeReader = mth.getCodeReader();
							if (codeReader != null) {
								codeReader.visitInstructions(insn -> {
									insn.decode();
									System.out.println(insn);
								});
							}
							System.out.println("---");
							System.out.println(mth.disassembleMethod());
							System.out.println("---");
						});
				System.out.println("----");
				System.out.println(cls.getDisassembledCode());
				System.out.println("----");
			});
			assertThat(count.get()).isGreaterThan(0);
		}
		System.out.println("Time: " + (System.currentTimeMillis() - start) + "ms");
	}
}
