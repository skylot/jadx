package jadx.cli;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class JadxCLIArgsTest {

	@Test
	public void testInvertedBooleanOption() throws Exception {
		assertThat(parse("--no-replace-consts").isReplaceConsts(), is(false));
		assertThat(parse("").isReplaceConsts(), is(true));
	}

	private JadxCLIArgs parse(String... args) {
		JadxCLIArgs jadxArgs = new JadxCLIArgs();
		boolean res = jadxArgs.processArgs(args);
		assertThat(res, is(true));
		return jadxArgs;
	}
}
