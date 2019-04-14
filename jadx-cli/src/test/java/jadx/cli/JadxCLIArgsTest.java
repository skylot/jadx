package jadx.cli;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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

	@Test
	public void testOptionsOverride() {
		assertThat(override(new JadxCLIArgs(), "--no-imports").isUseImports(), is(false));
		assertThat(override(new JadxCLIArgs(), "--no-debug-info").isDebugInfo(), is(false));
		assertThat(override(new JadxCLIArgs(), "").isUseImports(), is(true));

		JadxCLIArgs args = new JadxCLIArgs();
		args.useImports = false;
		assertThat(override(args, "--no-imports").isUseImports(), is(false));
		args.debugInfo = false;
		assertThat(override(args, "--no-debug-info").isDebugInfo(), is(false));

		args = new JadxCLIArgs();
		args.useImports = false;
		assertThat(override(args, "").isUseImports(), is(false));
	}

	private JadxCLIArgs parse(String... args) {
		return parse(new JadxCLIArgs(), args);
	}

	private JadxCLIArgs parse(JadxCLIArgs jadxArgs, String... args) {
		boolean res = jadxArgs.processArgs(args);
		assertThat(res, is(true));
		LOG.info("Jadx args: {}", jadxArgs.toJadxArgs());
		return jadxArgs;
	}

	private JadxCLIArgs override(JadxCLIArgs jadxArgs, String... args) {
		boolean res = jadxArgs.overrideProvided(args);
		assertThat(res, is(true));
		LOG.info("Jadx args: {}", jadxArgs.toJadxArgs());
		return jadxArgs;
	}
}
