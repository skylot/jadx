package jadx.core.dex.visitors.typeinference;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.NotYetImplementedExtension;
import jadx.api.JadxArgs;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.ArgType.WildcardBound;
import jadx.core.dex.nodes.RootNode;

import static jadx.core.dex.instructions.args.ArgType.BOOLEAN;
import static jadx.core.dex.instructions.args.ArgType.BYTE;
import static jadx.core.dex.instructions.args.ArgType.CHAR;
import static jadx.core.dex.instructions.args.ArgType.CLASS;
import static jadx.core.dex.instructions.args.ArgType.EXCEPTION;
import static jadx.core.dex.instructions.args.ArgType.INT;
import static jadx.core.dex.instructions.args.ArgType.NARROW;
import static jadx.core.dex.instructions.args.ArgType.NARROW_INTEGRAL;
import static jadx.core.dex.instructions.args.ArgType.OBJECT;
import static jadx.core.dex.instructions.args.ArgType.SHORT;
import static jadx.core.dex.instructions.args.ArgType.STRING;
import static jadx.core.dex.instructions.args.ArgType.THROWABLE;
import static jadx.core.dex.instructions.args.ArgType.UNKNOWN;
import static jadx.core.dex.instructions.args.ArgType.UNKNOWN_ARRAY;
import static jadx.core.dex.instructions.args.ArgType.UNKNOWN_OBJECT;
import static jadx.core.dex.instructions.args.ArgType.array;
import static jadx.core.dex.instructions.args.ArgType.generic;
import static jadx.core.dex.instructions.args.ArgType.genericType;
import static jadx.core.dex.instructions.args.ArgType.object;
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
		root.loadClasses(Collections.emptyList());
		root.initClassPath();
		compare = new TypeCompare(root);
	}

	@Test
	public void compareTypes() {
		firstIsNarrow(INT, UNKNOWN);

		firstIsNarrow(array(UNKNOWN), UNKNOWN);
		firstIsNarrow(array(UNKNOWN), NARROW);
	}

	@Test
	public void comparePrimitives() {
		check(INT, UNKNOWN_OBJECT, TypeCompareEnum.CONFLICT);
		check(INT, OBJECT, TypeCompareEnum.CONFLICT);

		check(INT, CHAR, TypeCompareEnum.WIDER);
		check(INT, SHORT, TypeCompareEnum.WIDER);

		check(BOOLEAN, INT, TypeCompareEnum.CONFLICT);
		check(BOOLEAN, CHAR, TypeCompareEnum.CONFLICT);
		check(CHAR, BYTE, TypeCompareEnum.CONFLICT);
		check(CHAR, SHORT, TypeCompareEnum.CONFLICT);

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

		firstIsNarrow(array(BYTE), OBJECT);
		firstIsNarrow(array(array(BYTE)), array(OBJECT));

		check(array(OBJECT), array(INT), TypeCompareEnum.CONFLICT);

		ArgType integerType = object("java.lang.Integer");
		check(array(OBJECT), array(integerType), TypeCompareEnum.WIDER);
		check(array(INT), array(integerType), TypeCompareEnum.CONFLICT);
		check(array(INT), array(INT), TypeCompareEnum.EQUAL);

		ArgType wildClass = generic(CLASS, wildcard());
		check(array(wildClass), array(CLASS), TypeCompareEnum.NARROW_BY_GENERIC);
		check(array(CLASS), array(wildClass), TypeCompareEnum.WIDER_BY_GENERIC);
	}

	@Test
	public void compareGenerics() {
		ArgType mapCls = object("java.util.Map");
		ArgType setCls = object("java.util.Set");

		ArgType keyType = genericType("K");
		ArgType valueType = genericType("V");
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
	public void compareWildCards() {
		ArgType clsWildcard = generic(CLASS.getObject(), wildcard());
		check(clsWildcard, CLASS, TypeCompareEnum.NARROW_BY_GENERIC);

		ArgType clsExtendedWildcard = generic(CLASS.getObject(), wildcard(STRING, WildcardBound.EXTENDS));
		check(clsWildcard, clsExtendedWildcard, TypeCompareEnum.WIDER);

		ArgType listWildcard = generic(CLASS.getObject(), wildcard(object("java.util.List"), WildcardBound.EXTENDS));
		ArgType collWildcard = generic(CLASS.getObject(), wildcard(object("java.util.Collection"), WildcardBound.EXTENDS));
		check(listWildcard, collWildcard, TypeCompareEnum.NARROW);

		ArgType collSuperWildcard = generic(CLASS.getObject(), wildcard(object("java.util.Collection"), WildcardBound.SUPER));
		check(collSuperWildcard, listWildcard, TypeCompareEnum.CONFLICT);
	}

	@Test
	public void compareGenericTypes() {
		ArgType vType = genericType("V");
		check(vType, OBJECT, TypeCompareEnum.NARROW);
		check(vType, STRING, TypeCompareEnum.CONFLICT);

		ArgType rType = genericType("R");
		check(vType, rType, TypeCompareEnum.CONFLICT);
		check(vType, vType, TypeCompareEnum.EQUAL);

		ArgType tType = genericType("T");
		ArgType tStringType = genericType("T", STRING);

		check(tStringType, STRING, TypeCompareEnum.NARROW);
		check(tStringType, OBJECT, TypeCompareEnum.NARROW);
		check(tStringType, tType, TypeCompareEnum.NARROW);

		ArgType tObjType = genericType("T", OBJECT);

		check(tObjType, OBJECT, TypeCompareEnum.NARROW);
		check(tObjType, tType, TypeCompareEnum.EQUAL);

		check(tStringType, tObjType, TypeCompareEnum.NARROW);
	}

	@Test
	public void compareGenericTypes2() {
		ArgType npeType = object("java.lang.NullPointerException");

		// check clsp graph
		check(npeType, THROWABLE, TypeCompareEnum.NARROW);
		check(npeType, EXCEPTION, TypeCompareEnum.NARROW);
		check(EXCEPTION, THROWABLE, TypeCompareEnum.NARROW);

		ArgType typeVar = genericType("T", EXCEPTION); // T extends Exception

		// target checks
		check(THROWABLE, typeVar, TypeCompareEnum.WIDER);
		check(EXCEPTION, typeVar, TypeCompareEnum.WIDER);
		check(npeType, typeVar, TypeCompareEnum.NARROW);
	}

	@Test
	public void compareOuterGenerics() {
		ArgType hashMapType = object("java.util.HashMap");
		ArgType innerEntrySetType = object("EntrySet");
		ArgType firstInstance = ArgType.outerGeneric(generic(hashMapType, STRING, STRING), innerEntrySetType);
		ArgType secondInstance = ArgType.outerGeneric(generic(hashMapType, OBJECT, OBJECT), innerEntrySetType);

		check(firstInstance, secondInstance, TypeCompareEnum.NARROW);
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
