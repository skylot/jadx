package jadx.api;

import java.io.File;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import jadx.core.utils.files.FileUtils;
import jadx.core.utils.files.InputFileTest;

import static org.hamcrest.MatcherAssert.assertThat;

public class JadxDecompilerTest {

	@Test
	public void testExampleUsage() {
		File sampleApk = InputFileTest.getFileFromSampleDir("app-with-fake-dex.apk");
		File outDir = FileUtils.createTempDir("jadx-usage-example").toFile();

		// test simple apk loading
		JadxArgs args = new JadxArgs();
		args.getInputFiles().add(sampleApk);
		args.setOutDir(outDir);

		JadxDecompiler jadx = new JadxDecompiler(args);
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

	// TODO add more tests
}
