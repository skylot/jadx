package jadx.api;

import java.io.File;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class JadxDecompilerTest {

	@Test
	@Disabled
	public void testExampleUsage() {
		JadxArgs args = new JadxArgs();
		args.getInputFiles().add(new File("test.apk"));
		args.setOutDir(new File("jadx-test-output"));

		JadxDecompiler jadx = new JadxDecompiler(args);
		jadx.load();
		jadx.save();
	}

	// TODO make more tests
}
