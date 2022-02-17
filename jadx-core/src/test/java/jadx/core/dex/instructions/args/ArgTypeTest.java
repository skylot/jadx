package jadx.core.dex.instructions.args;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static jadx.core.dex.instructions.args.ArgType.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArgTypeTest {

	private static final Logger LOG = LoggerFactory.getLogger(ArgTypeTest.class);

	@Test
	public void testEqualsOfGenericTypes() {
		ArgType first = ArgType.generic("java.lang.List", ArgType.STRING);
		ArgType second = ArgType.generic("Ljava/lang/List;", ArgType.STRING);

		assertEquals(first, second);
	}

	@Test
	void testContainsGenericType() {
		ArgType wildcard = ArgType.wildcard(ArgType.genericType("T"), ArgType.WildcardBound.SUPER);
		assertTrue(wildcard.containsTypeVariable());

		ArgType type = ArgType.generic("java.lang.List", wildcard);
		assertTrue(type.containsTypeVariable());
	}

	@Test
	void testInnerGeneric() {
		ArgType[] genericTypes = new ArgType[] { ArgType.genericType("K"), ArgType.genericType("V") };
		ArgType base = ArgType.generic("java.util.Map", genericTypes);

		ArgType genericInner = ArgType.outerGeneric(base, ArgType.generic("Entry", genericTypes));
		assertThat(genericInner.toString(), is("java.util.Map<K, V>$Entry<K, V>"));
		assertTrue(genericInner.containsTypeVariable());

		ArgType genericInner2 = ArgType.outerGeneric(base, ArgType.object("Entry"));
		assertThat(genericInner2.toString(), is("java.util.Map<K, V>$Entry"));
		assertTrue(genericInner2.containsTypeVariable());
	}


	/*
	*	Test Class: UnknownArg.class of ArgType.class
	*	Test Function: selectFirst
	* */
	@Test
	void testSelectFirst() {
		//test if UNKNOWN OBJECT's first choice is an OBJECT
		ArgType object = UNKNOWN_OBJECT.selectFirst();
		assertEquals(object, OBJECT);

		//test if UNKNOWN OBJECT's first choice is an OBJECT, without ARRAY
		ArgType objectNoArray = UNKNOWN_OBJECT_NO_ARRAY.selectFirst();
		assertEquals(objectNoArray, OBJECT);

		//test if UNKNOWN ALL PrimitiveType's first choice is an OBJECT
		ArgType argType = UNKNOWN.selectFirst();
		assertEquals(argType, OBJECT);

		//test if Array with UNKNOWN first choice is an OBJECT[]
		ArgType argTypeArray = UNKNOWN_ARRAY.selectFirst();
		assertEquals(argTypeArray, array(OBJECT));

		//test if UNKNOWN with Array first choice is an OBJECT[]
		final ArgType UNKNOWN_OBJECT_ARRAY = unknown(PrimitiveType.ARRAY);
		ArgType objectArray = UNKNOWN_OBJECT_ARRAY.selectFirst();
		assertEquals(objectArray, array(OBJECT));

		//test if UNKNOWN with PRIMITIVE BOOLEAN first choice is a BOOLEAN
		final ArgType UNKNOWN_PRIMITIVE_BOOLEAN = unknown(PrimitiveType.BOOLEAN);
		ArgType primitiveBoolean = UNKNOWN_PRIMITIVE_BOOLEAN.selectFirst();
		assertEquals(primitiveBoolean, BOOLEAN);

		//test if UNKNOWN with PRIMITIVE LONG first choice is a LONG
		final ArgType UNKNOWN_PRIMITIVE_LONG = unknown(PrimitiveType.LONG);
		ArgType primitiveLONG = UNKNOWN_PRIMITIVE_LONG.selectFirst();
		assertEquals(primitiveLONG, LONG);
	}

	/*
	 *	Test Class: UnknownArg.class of ArgType.class
	 *	Test Function: internalEquals
	 * */
	@Test
	void testInternalEquals() {
		//test if two unknowns that contain object and array are equal
		ArgType unknown1 = unknown(PrimitiveType.OBJECT, PrimitiveType.ARRAY);
		ArgType unknown2 = unknown(PrimitiveType.OBJECT, PrimitiveType.ARRAY);
		boolean booleanUnknownObject = unknown1.internalEquals(unknown2);
		assertEquals(booleanUnknownObject, true);

		//test if two unknowns that only contain object are equal
		ArgType unknown3 = unknown(PrimitiveType.OBJECT);
		ArgType unknown4 = unknown(PrimitiveType.OBJECT);
		boolean booleanUnknownObjectNoArray = unknown3.internalEquals(unknown4);
		assertEquals(booleanUnknownObjectNoArray, true);

		//test if two unknowns that contain PrimitiveTypes are equal
		ArgType unknown5 = unknown(PrimitiveType.values());
		ArgType unknown6 = unknown(PrimitiveType.values());
		boolean booleanUnknown = unknown5.internalEquals(unknown6);
		assertEquals(booleanUnknown, true);

		//test if two unknown arrays are equal
		ArgType unknown7 = array(UNKNOWN);
		ArgType unknown8 = array(UNKNOWN);
		boolean booleanArray = unknown7.internalEquals(unknown8);
		assertEquals(booleanArray, true);
	}
}
