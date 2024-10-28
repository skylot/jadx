package jadx.core.dex.visitors.typeinference;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jadx.api.JadxArgs;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.RootNode;

import static jadx.core.dex.instructions.args.ArgType.BOOLEAN;
import static jadx.core.dex.instructions.args.ArgType.BYTE;
import static jadx.core.dex.instructions.args.ArgType.CHAR;
import static jadx.core.dex.instructions.args.ArgType.DOUBLE;
import static jadx.core.dex.instructions.args.ArgType.FLOAT;
import static jadx.core.dex.instructions.args.ArgType.INT;
import static jadx.core.dex.instructions.args.ArgType.LONG;
import static jadx.core.dex.instructions.args.ArgType.SHORT;
import static jadx.core.dex.instructions.args.ArgType.VOID;
import static jadx.core.dex.visitors.typeinference.TypeCompareEnum.CONFLICT;
import static jadx.core.dex.visitors.typeinference.TypeCompareEnum.EQUAL;
import static jadx.core.dex.visitors.typeinference.TypeCompareEnum.NARROW;
import static jadx.core.dex.visitors.typeinference.TypeCompareEnum.WIDER;
import static org.assertj.core.api.Assertions.assertThat;

public class PrimitiveConversionsTests {

	private static TypeCompare comparator;

	@BeforeAll
	static void before() {
		JadxArgs args = new JadxArgs();
		RootNode root = new RootNode(args);
		comparator = new TypeCompare(root);
	}

	@DisplayName("Check conversion of numeric types")
	@ParameterizedTest(name = "{0} -> {1} (should be {2})")
	@MethodSource("provideArgsForNumericConversionsTest")
	void testNumericConversions(ArgType firstType, ArgType secondType, TypeCompareEnum expectedResult) {
		assertThat(comparator.compareTypes(firstType, secondType)).isEqualTo(expectedResult);
	}

	@DisplayName("Ensure that `boolean` is not convertible to other primitive types")
	@ParameterizedTest(name = "{0} <-> boolean")
	@MethodSource("providePrimitiveTypesWithVoid")
	void testBooleanConversions(ArgType type) {
		final var expectedResult = type.equals(BOOLEAN) ? EQUAL : CONFLICT;
		assertThat(comparator.compareTypes(type, BOOLEAN)).isEqualTo(expectedResult);
		assertThat(comparator.compareTypes(BOOLEAN, type)).isEqualTo(expectedResult);
	}

	@DisplayName("Ensure that `void` is not convertible to other primitive types")
	@ParameterizedTest(name = "{0} <-> void")
	@MethodSource("providePrimitiveTypesWithVoid")
	void testVoidConversions(ArgType type) {
		final var expectedResult = type.equals(VOID) ? EQUAL : CONFLICT;
		assertThat(comparator.compareTypes(type, VOID)).isEqualTo(expectedResult);
		assertThat(comparator.compareTypes(VOID, type)).isEqualTo(expectedResult);
	}

	private static Stream<Arguments> provideArgsForNumericConversionsTest() {
		return Stream.of(
				Arguments.of(BYTE, BYTE, EQUAL),
				Arguments.of(BYTE, SHORT, NARROW),
				Arguments.of(BYTE, CHAR, WIDER),
				Arguments.of(BYTE, INT, NARROW),
				Arguments.of(BYTE, LONG, NARROW),
				Arguments.of(BYTE, FLOAT, NARROW),
				Arguments.of(BYTE, DOUBLE, NARROW),

				Arguments.of(SHORT, BYTE, WIDER),
				Arguments.of(SHORT, SHORT, EQUAL),
				Arguments.of(SHORT, CHAR, WIDER),
				Arguments.of(SHORT, INT, NARROW),
				Arguments.of(SHORT, LONG, NARROW),
				Arguments.of(SHORT, FLOAT, NARROW),
				Arguments.of(SHORT, DOUBLE, NARROW),

				Arguments.of(CHAR, BYTE, WIDER),
				Arguments.of(CHAR, SHORT, WIDER),
				Arguments.of(CHAR, CHAR, EQUAL),
				Arguments.of(CHAR, INT, NARROW),
				Arguments.of(CHAR, LONG, NARROW),
				Arguments.of(CHAR, FLOAT, NARROW),
				Arguments.of(CHAR, DOUBLE, NARROW),

				Arguments.of(INT, BYTE, WIDER),
				Arguments.of(INT, SHORT, WIDER),
				Arguments.of(INT, CHAR, WIDER),
				Arguments.of(INT, INT, EQUAL),
				Arguments.of(INT, LONG, NARROW),
				Arguments.of(INT, FLOAT, NARROW),
				Arguments.of(INT, DOUBLE, NARROW),

				Arguments.of(LONG, BYTE, WIDER),
				Arguments.of(LONG, SHORT, WIDER),
				Arguments.of(LONG, CHAR, WIDER),
				Arguments.of(LONG, INT, WIDER),
				Arguments.of(LONG, LONG, EQUAL),
				Arguments.of(LONG, FLOAT, NARROW),
				Arguments.of(LONG, DOUBLE, NARROW),

				Arguments.of(FLOAT, BYTE, WIDER),
				Arguments.of(FLOAT, SHORT, WIDER),
				Arguments.of(FLOAT, CHAR, WIDER),
				Arguments.of(FLOAT, INT, WIDER),
				Arguments.of(FLOAT, LONG, WIDER),
				Arguments.of(FLOAT, FLOAT, EQUAL),
				Arguments.of(FLOAT, DOUBLE, NARROW),

				Arguments.of(DOUBLE, BYTE, WIDER),
				Arguments.of(DOUBLE, SHORT, WIDER),
				Arguments.of(DOUBLE, CHAR, WIDER),
				Arguments.of(DOUBLE, INT, WIDER),
				Arguments.of(DOUBLE, LONG, WIDER),
				Arguments.of(DOUBLE, FLOAT, WIDER),
				Arguments.of(DOUBLE, DOUBLE, EQUAL));
	}

	private static Stream<ArgType> providePrimitiveTypesWithVoid() {
		return Stream.of(BYTE, SHORT, CHAR, INT, LONG, FLOAT, DOUBLE, BOOLEAN, VOID);
	}
}
