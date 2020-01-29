package jadx.core.dex.visitors.typeinference;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.NotYetImplemented;
import jadx.NotYetImplementedExtension;
import jadx.api.JadxArgs;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.RootNode;

import static jadx.core.dex.instructions.args.ArgType.BOOLEAN;
import static jadx.core.dex.instructions.args.ArgType.CHAR;
import static jadx.core.dex.instructions.args.ArgType.CLASS;
import static jadx.core.dex.instructions.args.ArgType.INT;
import static jadx.core.dex.instructions.args.ArgType.NARROW;
import static jadx.core.dex.instructions.args.ArgType.NARROW_INTEGRAL;
import static jadx.core.dex.instructions.args.ArgType.OBJECT;
import static jadx.core.dex.instructions.args.ArgType.STRING;
import static jadx.core.dex.instructions.args.ArgType.UNKNOWN;
import static jadx.core.dex.instructions.args.ArgType.UNKNOWN_ARRAY;
import static jadx.core.dex.instructions.args.ArgType.UNKNOWN_OBJECT;
import static jadx.core.dex.instructions.args.ArgType.array;
import static jadx.core.dex.instructions.args.ArgType.generic;
import static jadx.core.dex.instructions.args.ArgType.wildcard;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(NotYetImplementedExtension.class)
public class TypeCompareTest {
	private static final Logger LOG = LoggerFactory.getLogger(TypeCompareTest.class);

	private TypeCompare compare;

	@BeforeEach
	public void init() {
		JadxArgs args = new JadxArgs();
		RootNode root = new RootNode(args);
		root.load(Collections.emptyList());
		root.initClassPath();
		compare = new TypeCompare(root);
	}

	@Test
	public void compareTypes() {
		firstIsNarrow(INT, UNKNOWN);

		firstIsNarrow(BOOLEAN, INT);

		firstIsNarrow(array(UNKNOWN), UNKNOWN);
		firstIsNarrow(array(UNKNOWN), NARROW);
	}

	@Test
	public void comparePrimitives() {
		check(INT, UNKNOWN_OBJECT, TypeCompareEnum.CONFLICT);
		check(INT, OBJECT, TypeCompareEnum.CONFLICT);
		check(INT, CHAR, TypeCompareEnum.WIDER);

		firstIsNarrow(CHAR, NARROW_INTEGRAL);
		firstIsNarrow(array(CHAR), UNKNOWN_OBJECT);
	}

	@Test
	public void compareArrays() {
		firstIsNarrow(array(CHAR), OBJECT);
		firstIsNarrow(array(CHAR), array(UNKNOWN));

		firstIsNarrow(array(OBJECT), OBJECT);
		firstIsNarrow(array(OBJECT), array(UNKNOWN_OBJECT));
		firstIsNarrow(array(STRING), array(UNKNOWN_OBJECT));
		firstIsNarrow(array(STRING), array(OBJECT));

		firstIsNarrow(UNKNOWN_ARRAY, OBJECT);

		check(array(OBJECT), array(INT), TypeCompareEnum.CONFLICT);

		ArgType integerType = ArgType.object("java.lang.Integer");
		check(array(OBJECT), array(integerType), TypeCompareEnum.WIDER);
		check(array(INT), array(integerType), TypeCompareEnum.CONFLICT);
		check(array(INT), array(INT), TypeCompareEnum.EQUAL);

		ArgType wildClass = generic(CLASS, wildcard());
		check(array(wildClass), array(CLASS), TypeCompareEnum.NARROW_BY_GENERIC);
		check(array(CLASS), array(wildClass), TypeCompareEnum.WIDER_BY_GENERIC);
	}

	@Test
	public void compareGenerics() {
		ArgType mapCls = ArgType.object("java.util.Map");
		ArgType setCls = ArgType.object("java.util.Set");

		ArgType keyType = ArgType.genericType("K");
		ArgType valueType = ArgType.genericType("V");
		ArgType mapGeneric = ArgType.generic(mapCls.getObject(), keyType, valueType);

		check(mapCls, mapGeneric, TypeCompareEnum.WIDER_BY_GENERIC);
		check(mapCls, setCls, TypeCompareEnum.CONFLICT);

		ArgType setGeneric = ArgType.generic(setCls.getObject(), valueType);
		ArgType setWildcard = ArgType.generic(setCls.getObject(), ArgType.wildcard());

		check(setWildcard, setGeneric, TypeCompareEnum.CONFLICT);
		check(setWildcard, setCls, TypeCompareEnum.NARROW_BY_GENERIC);
		// TODO implement compare for wildcard with bounds
	}

	@Test
	public void compareGenericTypes() {
		ArgType vType = ArgType.genericType("V");
		ArgType rType = ArgType.genericType("R");

		check(vType, ArgType.OBJECT, TypeCompareEnum.NARROW_BY_GENERIC);
		check(ArgType.OBJECT, vType, TypeCompareEnum.WIDER_BY_GENERIC);

		check(vType, rType, TypeCompareEnum.CONFLICT);
		check(vType, vType, TypeCompareEnum.EQUAL);

		ArgType tType = ArgType.genericType("T");
		tType.setExtendTypes(Collections.singletonList(ArgType.STRING));

		check(tType, ArgType.STRING, TypeCompareEnum.NARROW_BY_GENERIC);
		check(tType, ArgType.OBJECT, TypeCompareEnum.NARROW_BY_GENERIC);
	}

	@Test
	@NotYetImplemented
	public void compareGenericTypesNYI() {
		ArgType vType = ArgType.genericType("V");
		// TODO: use extend types from generic declaration for more strict checks
		check(vType, ArgType.STRING, TypeCompareEnum.CONFLICT);
	}

	private void firstIsNarrow(ArgType first, ArgType second) {
		check(first, second, TypeCompareEnum.NARROW);
	}

	private void check(ArgType first, ArgType second, TypeCompareEnum expectedResult) {
		LOG.debug("Compare: '{}' and '{}', expect: '{}'", first, second, expectedResult);

		assertThat(compare.compareTypes(first, second))
				.as("Compare '%s' and '%s'", first, second)
				.isEqualTo(expectedResult);

		assertThat(compare.compareTypes(second, first))
				.as("Compare '%s' and '%s'", second, first)
				.isEqualTo(expectedResult.invert());
	}
}
