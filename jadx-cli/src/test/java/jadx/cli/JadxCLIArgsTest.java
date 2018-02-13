package jadx.cli;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class JadxCLIArgsTest {

	private static final Logger LOG = LoggerFactory.getLogger(JadxCLIArgsTest.class);

	@Test
	public void testInvertedBooleanOption() {
		assertThat(parse("--no-replace-consts").isReplaceConsts(), is(false));
		assertThat(parse("").isReplaceConsts(), is(true));
	}

	@Test
	public void testEscapeUnicodeOption() {
		assertThat(parse("--escape-unicode").isEscapeUnicode(), is(true));
		assertThat(parse("").isEscapeUnicode(), is(false));
	}

	@Test
	public void testSrcOption() {
		assertThat(parse("--no-src").isSkipSources(), is(true));
		assertThat(parse("-s").isSkipSources(), is(true));
		assertThat(parse("").isSkipSources(), is(false));
	}

	private JadxCLIArgs parse(String... args) {
		JadxCLIArgs jadxArgs = new JadxCLIArgs();
		boolean res = jadxArgs.processArgs(args);
		assertThat(res, is(true));
		LOG.info("Jadx args: {}", jadxArgs.toJadxArgs());
		return jadxArgs;
	}
}
