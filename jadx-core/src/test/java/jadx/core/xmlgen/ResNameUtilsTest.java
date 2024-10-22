package jadx.core.xmlgen;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class ResNameUtilsTest {

	@DisplayName("Check sanitizeAsResourceName(name, postfix, allowNonPrintable)")
	@ParameterizedTest(name = "({0}, {1}, {2}) -> {3}")
	@MethodSource("provideArgsForSanitizeAsResourceNameTest")
	void testSanitizeAsResourceName(String name, String postfix, boolean allowNonPrintable, String expectedResult) {
		assertThat(ResNameUtils.sanitizeAsResourceName(name, postfix, allowNonPrintable)).isEqualTo(expectedResult);
	}

	@DisplayName("Check convertToRFieldName(resourceName)")
	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("provideArgsForConvertToRFieldNameTest")
	void testConvertToRFieldName(String resourceName, String expectedResult) {
		assertThat(ResNameUtils.convertToRFieldName(resourceName)).isEqualTo(expectedResult);
	}

	private static Stream<Arguments> provideArgsForSanitizeAsResourceNameTest() {
		return Stream.of(
				Arguments.of("name", "_postfix", false, "name"),

				Arguments.of("/name", "_postfix", true, "_name_postfix"),
				Arguments.of("na/me", "_postfix", true, "na_me_postfix"),
				Arguments.of("name/", "_postfix", true, "name__postfix"),

				Arguments.of("$name", "_postfix", true, "_name_postfix"),
				Arguments.of("na$me", "_postfix", true, "na_me_postfix"),
				Arguments.of("name$", "_postfix", true, "name__postfix"),

				Arguments.of(".name", "_postfix", true, "_.name_postfix"),
				Arguments.of("na.me", "_postfix", true, "na.me"),
				Arguments.of("name.", "_postfix", true, "name."),

				Arguments.of("0name", "_postfix", true, "_0name_postfix"),
				Arguments.of("na0me", "_postfix", true, "na0me"),
				Arguments.of("name0", "_postfix", true, "name0"),

				Arguments.of("-name", "_postfix", true, "_name_postfix"),
				Arguments.of("na-me", "_postfix", true, "na_me_postfix"),
				Arguments.of("name-", "_postfix", true, "name__postfix"),

				Arguments.of("Ĉname", "_postfix", false, "_name_postfix"),
				Arguments.of("naĈme", "_postfix", false, "na_me_postfix"),
				Arguments.of("nameĈ", "_postfix", false, "name__postfix"),

				Arguments.of("Ĉname", "_postfix", true, "Ĉname"),
				Arguments.of("naĈme", "_postfix", true, "naĈme"),
				Arguments.of("nameĈ", "_postfix", true, "nameĈ"),

				// Uncomment this when XID_Start and XID_Continue characters are correctly determined.
				// Arguments.of("Жname", "_postfix", true, "Жname"),
				// Arguments.of("naЖme", "_postfix", true, "naЖme"),
				// Arguments.of("nameЖ", "_postfix", true, "nameЖ"),
				//
				// Arguments.of("€name", "_postfix", true, "_name_postfix"),
				// Arguments.of("na€me", "_postfix", true, "na_me_postfix"),
				// Arguments.of("name€", "_postfix", true, "name__postfix"),

				Arguments.of("", "_postfix", true, "_postfix"),

				Arguments.of("if", "_postfix", true, "if_postfix"),
				Arguments.of("default", "_postfix", true, "default_postfix"),
				Arguments.of("true", "_postfix", true, "true_postfix"),
				Arguments.of("_", "_postfix", true, "__postfix"));
	}

	private static Stream<Arguments> provideArgsForConvertToRFieldNameTest() {
		return Stream.of(
				Arguments.of("ThemeDesign", "ThemeDesign"),
				Arguments.of("Theme.Design", "Theme_Design"),

				Arguments.of("Ĉ_ThemeDesign_Ĉ", "Ĉ_ThemeDesign_Ĉ"),
				Arguments.of("Ĉ_Theme.Design_Ĉ", "Ĉ_Theme_Design_Ĉ"),

				// The function must return a plausible result even though the resource name is invalid.
				Arguments.of("/_ThemeDesign_/", "/_ThemeDesign_/"),
				Arguments.of("/_Theme.Design_/", "/_Theme_Design_/"));
	}
}
