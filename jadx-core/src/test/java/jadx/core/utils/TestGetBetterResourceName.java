package jadx.core.utils;

import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;

import static jadx.core.utils.BetterName.getBetterResourceName;
import static org.assertj.core.api.Assertions.assertThat;

public class TestGetBetterResourceName {

	@Test
	public void testGoodNamesVsSyntheticNames() {
		assertThatBetterResourceName("color_main", "t0").isEqualTo("color_main");
		assertThatBetterResourceName("done", "oOo0oO0o").isEqualTo("done");
	}

	/**
	 * Tests {@link BetterName#getBetterResourceName(String, String)} on equally good names.
	 * In this case, according to the documentation, the method should return the first argument.
	 *
	 * @see BetterName#getBetterResourceName(String, String)
	 */
	@Test
	public void testEquallyGoodNames() {
		assertThatBetterResourceName("AAAA", "BBBB").isEqualTo("AAAA");
		assertThatBetterResourceName("BBBB", "AAAA").isEqualTo("BBBB");

		assertThatBetterResourceName("Theme.AppCompat.Light", "Theme_AppCompat_Light")
				.isEqualTo("Theme.AppCompat.Light");
		assertThatBetterResourceName("Theme_AppCompat_Light", "Theme.AppCompat.Light")
				.isEqualTo("Theme_AppCompat_Light");
	}

	private AbstractStringAssert<?> assertThatBetterResourceName(String firstName, String secondName) {
		return assertThat(getBetterResourceName(firstName, secondName));
	}
}
