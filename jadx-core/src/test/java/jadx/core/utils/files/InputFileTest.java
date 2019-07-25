package jadx.core.utils.files;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.utils.exceptions.DecodeException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

public class InputFileTest {
	private static final String TEST_SAMPLES_DIR = "test-samples/";

	@Test
	public void testApkWithFakeDex() throws IOException, DecodeException {
		File sample = getFileFromSampleDir("app-with-fake-dex.apk");

		List<InputFile> list = new ArrayList<>();
		InputFile.addFilesFrom(sample, list, false);
		assertThat(list, hasSize(1));
		InputFile inputFile = list.get(0);
		assertThat(inputFile.getDexFiles(), hasSize(1));
	}

	public static File getFileFromSampleDir(String fileName) {
		URL resource = InputFileTest.class.getClassLoader().getResource(TEST_SAMPLES_DIR + fileName);
		assertThat(resource, notNullValue());
		String pathStr = resource.getFile();
		return new File(pathStr);
	}
}
