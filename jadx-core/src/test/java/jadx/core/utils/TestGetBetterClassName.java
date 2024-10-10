package jadx.core.utils;

import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;

import static jadx.core.utils.BetterName.getBetterClassName;
import static org.assertj.core.api.Assertions.assertThat;

public class TestGetBetterClassName {

	@Test
	public void testGoodNamesVsGeneratedAliases() {
		assertThatBetterClassName("AppCompatButton", "C2404e2").isEqualTo("AppCompatButton");
		assertThatBetterClassName("ContextThemeWrapper", "C2106b1").isEqualTo("ContextThemeWrapper");
		assertThatBetterClassName("ListPopupWindow", "C2344a3").isEqualTo("ListPopupWindow");
	}

	@Test
	public void testShortGoodNamesVsGeneratedAliases() {
		assertThatBetterClassName("Room", "C2937kh").isEqualTo("Room");
		assertThatBetterClassName("Fade", "C1428qi").isEqualTo("Fade");
		assertThatBetterClassName("Scene", "C4063yi").isEqualTo("Scene");
	}

	@Test
	public void testGoodNamesVsGeneratedAliasesWithPrefix() {
		assertThatBetterClassName("AppCompatActivity", "ActivityC2646h0").isEqualTo("AppCompatActivity");
		assertThatBetterClassName("PagerAdapter", "AbstractC3038lk").isEqualTo("PagerAdapter");
		assertThatBetterClassName("Lazy", "InterfaceC6434a").isEqualTo("Lazy");
		assertThatBetterClassName("MembersInjector", "InterfaceC6435b").isEqualTo("MembersInjector");
		assertThatBetterClassName("Subscriber", "InterfaceC6439c").isEqualTo("Subscriber");
	}

	@Test
	public void testGoodNamesWithDigitsVsGeneratedAliases() {
		assertThatBetterClassName("ISO8061Formatter", "C1121uq4").isEqualTo("ISO8061Formatter");
		assertThatBetterClassName("Jdk9Platform", "C1189rn6").isEqualTo("Jdk9Platform");
		assertThatBetterClassName("WrappedDrawableApi14", "C2847i9").isEqualTo("WrappedDrawableApi14");
		assertThatBetterClassName("WrappedDrawableApi21", "C2888j9").isEqualTo("WrappedDrawableApi21");
	}

	@Test
	public void testShortNamesVsLongNames() {
		assertThatBetterClassName("az", "Observer").isEqualTo("Observer");
		assertThatBetterClassName("bb", "RenderEvent").isEqualTo("RenderEvent");
		assertThatBetterClassName("aaaa", "FontUtils").isEqualTo("FontUtils");
	}

	/**
	 * Tests {@link BetterName#getBetterClassName(String, String)} on equally good names.
	 * In this case, according to the documentation, the method should return the first argument.
	 *
	 * @see BetterName#getBetterClassName(String, String)
	 */
	@Test
	public void testEquallyGoodNames() {
		assertThatBetterClassName("AAAA", "BBBB").isEqualTo("AAAA");
		assertThatBetterClassName("BBBB", "AAAA").isEqualTo("BBBB");

		assertThatBetterClassName("XYXYXY", "YZYZYZ").isEqualTo("XYXYXY");
		assertThatBetterClassName("YZYZYZ", "XYXYXY").isEqualTo("YZYZYZ");
	}

	private AbstractStringAssert<?> assertThatBetterClassName(String firstName, String secondName) {
		return assertThat(getBetterClassName(firstName, secondName));
	}
}
