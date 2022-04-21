package jadx.api;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.files.FileUtils;

import static jadx.core.utils.files.FileUtils.toFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class JadxArgsValidatorOutDirsTest {

	private static final Logger LOG = LoggerFactory.getLogger(JadxArgsValidatorOutDirsTest.class);
	public JadxArgs args;

	@Test
	public void checkAllSet() {
		setOutDirs("r", "s", "r");
		checkOutDirs("r", "s", "r");
	}

	@Test
	public void checkRootOnly() {
		setOutDirs("out", null, null);
		checkOutDirs("out", "out/" + JadxArgs.DEFAULT_SRC_DIR, "out/" + JadxArgs.DEFAULT_RES_DIR);
	}

	@Test
	public void checkSrcOnly() {
		setOutDirs(null, "src", null);
		checkOutDirs("src", "src", "src/" + JadxArgs.DEFAULT_RES_DIR);
	}

	@Test
	public void checkResOnly() {
		setOutDirs(null, null, "res");
		checkOutDirs("res", "res/" + JadxArgs.DEFAULT_SRC_DIR, "res");
	}

	@Test
	public void checkNone() {
		setOutDirs(null, null, null);
		String inputFileBase = args.getInputFiles().get(0).getName().replace(".apk", "");
		checkOutDirs(inputFileBase,
				inputFileBase + '/' + JadxArgs.DEFAULT_SRC_DIR,
				inputFileBase + '/' + JadxArgs.DEFAULT_RES_DIR);
	}

	private void setOutDirs(String outDir, String srcDir, String resDir) {
		args = makeArgs();
		args.setOutDir(toFile(outDir));
		args.setOutDirSrc(toFile(srcDir));
		args.setOutDirRes(toFile(resDir));
		LOG.debug("Set dirs: out={}, src={}, res={}", outDir, srcDir, resDir);
	}

	private void checkOutDirs(String outDir, String srcDir, String resDir) {
		JadxArgsValidator.validate(new JadxDecompiler(args));
		LOG.debug("Got dirs: out={}, src={}, res={}", args.getOutDir(), args.getOutDirSrc(), args.getOutDirRes());
		assertThat(args.getOutDir(), is(toFile(outDir)));
		assertThat(args.getOutDirSrc(), is(toFile(srcDir)));
		assertThat(args.getOutDirRes(), is(toFile(resDir)));
	}

	private JadxArgs makeArgs() {
		JadxArgs args = new JadxArgs();
		args.getInputFiles().add(FileUtils.createTempFile("some.apk").toFile());
		return args;
	}
}
