package jadx.cli;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static jadx.core.utils.Utils.newConstStringMap;
import static org.assertj.core.api.Assertions.assertThat;

public class JadxCLIArgsTest {

	private static final Logger LOG = LoggerFactory.getLogger(JadxCLIArgsTest.class);

	@Test
	public void testInvertedBooleanOption() {
		assertThat(parse("--no-replace-consts").isReplaceConsts()).isFalse();
		assertThat(parse("").isReplaceConsts()).isTrue();
	}

	@Test
	public void testEscapeUnicodeOption() {
		assertThat(parse("--escape-unicode").isEscapeUnicode()).isTrue();
		assertThat(parse("").isEscapeUnicode()).isFalse();
	}

	@Test
	public void testSrcOption() {
		assertThat(parse("--no-src").isSkipSources()).isTrue();
		assertThat(parse("-s").isSkipSources()).isTrue();
		assertThat(parse("").isSkipSources()).isFalse();
	}

	@Test
	public void testOptionsOverride() {
		assertThat(override(new JadxCLIArgs(), "--no-imports").isUseImports()).isFalse();
		assertThat(override(new JadxCLIArgs(), "--no-debug-info").isDebugInfo()).isFalse();
		assertThat(override(new JadxCLIArgs(), "").isUseImports()).isTrue();

		JadxCLIArgs args = new JadxCLIArgs();
		args.useImports = false;
		assertThat(override(args, "--no-imports").isUseImports()).isFalse();
		args.debugInfo = false;
		assertThat(override(args, "--no-debug-info").isDebugInfo()).isFalse();

		args = new JadxCLIArgs();
		args.useImports = false;
		assertThat(override(args, "").isUseImports()).isFalse();
	}

	@Test
	public void testPluginOptionsOverride() {
		// add key to empty base map
		checkPluginOptionsMerge(
				Collections.emptyMap(),
				"-Poption=otherValue",
				newConstStringMap("option", "otherValue"));

		// override one key
		checkPluginOptionsMerge(
				newConstStringMap("option", "value"),
				"-Poption=otherValue",
				newConstStringMap("option", "otherValue"));

		// merge different keys
		checkPluginOptionsMerge(
				Collections.singletonMap("option1", "value1"),
				"-Poption2=otherValue2",
				newConstStringMap("option1", "value1", "option2", "otherValue2"));

		// merge and override
		checkPluginOptionsMerge(
				newConstStringMap("option1", "value1", "option2", "value2"),
				"-Poption2=otherValue2",
				newConstStringMap("option1", "value1", "option2", "otherValue2"));
	}

	private void checkPluginOptionsMerge(Map<String, String> baseMap, String providedArgs, Map<String, String> expectedMap) {
		JadxCLIArgs args = new JadxCLIArgs();
		args.pluginOptions = baseMap;
		Map<String, String> resultMap = override(args, providedArgs).getPluginOptions();
		assertThat(resultMap).isEqualTo(expectedMap);
	}

	private JadxCLIArgs parse(String... args) {
		return parse(new JadxCLIArgs(), args);
	}

	private JadxCLIArgs parse(JadxCLIArgs jadxArgs, String... args) {
		return check(jadxArgs, jadxArgs.processArgs(args));
	}

	private JadxCLIArgs override(JadxCLIArgs jadxArgs, String... args) {
		return check(jadxArgs, overrideProvided(jadxArgs, args));
	}

	private static boolean overrideProvided(JadxCLIArgs jadxArgs, String[] args) {
		JCommanderWrapper jcw = new JCommanderWrapper(new JadxCLIArgs());
		if (!jcw.parse(args)) {
			return false;
		}
		jcw.overrideProvided(jadxArgs);
		return jadxArgs.process(jcw);
	}

	private static JadxCLIArgs check(JadxCLIArgs jadxArgs, boolean res) {
		assertThat(res).isTrue();
		LOG.info("Jadx args: {}", jadxArgs.toJadxArgs());
		return jadxArgs;
	}
}
