package jadx.api;

import java.io.File;
import java.net.URL;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import jadx.core.utils.files.FileUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class JadxDecompilerTest {

	@Test
	public void testExampleUsage() {
		File sampleApk = getFileFromSampleDir("app-with-fake-dex.apk");
		File outDir = FileUtils.createTempDir("jadx-usage-example").toFile();

		// test simple apk loading
		JadxArgs args = new JadxArgs();
		args.getInputFiles().add(sampleApk);
		args.setOutDir(outDir);

		try (JadxDecompiler jadx = new JadxDecompiler(args)) {
			jadx.load();
			jadx.save();
			jadx.printErrorsReport();

			// test class print
			for (JavaClass cls : jadx.getClasses()) {
				System.out.println(cls.getCode());
			}

			assertThat(jadx.getClasses(), Matchers.hasSize(3));
			assertThat(jadx.getErrorsCount(), Matchers.is(0));
		}
	}

	private static final String TEST_SAMPLES_DIR = "test-samples/";

	public static File getFileFromSampleDir(String fileName) {
		URL resource = JadxDecompilerTest.class.getClassLoader().getResource(TEST_SAMPLES_DIR + fileName);
		assertThat(resource, notNullValue());
		String pathStr = resource.getFile();
		return new File(pathStr);
	}

	// TODO add more tests
}
