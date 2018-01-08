package jadx.tests.functional;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import jadx.core.clsp.ClspGraph;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.DecodeException;

import static jadx.core.dex.instructions.args.ArgType.BOOLEAN;
import static jadx.core.dex.instructions.args.ArgType.BYTE;
import static jadx.core.dex.instructions.args.ArgType.CHAR;
import static jadx.core.dex.instructions.args.ArgType.INT;
import static jadx.core.dex.instructions.args.ArgType.LONG;
import static jadx.core.dex.instructions.args.ArgType.NARROW;
import static jadx.core.dex.instructions.args.ArgType.OBJECT;
import static jadx.core.dex.instructions.args.ArgType.STRING;
import static jadx.core.dex.instructions.args.ArgType.UNKNOWN;
import static jadx.core.dex.instructions.args.ArgType.UNKNOWN_OBJECT;
import static jadx.core.dex.instructions.args.ArgType.array;
import static jadx.core.dex.instructions.args.ArgType.genericType;
import static jadx.core.dex.instructions.args.ArgType.object;
import static jadx.core.dex.instructions.args.ArgType.unknown;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypeMergeTest {

	private DexNode dex;

	@Before
	public void initClsp() throws IOException, DecodeException {
		ClspGraph clsp = new ClspGraph();
		clsp.load();
		dex = mock(DexNode.class);
		RootNode rootNode = mock(RootNode.class);
		when(rootNode.getClsp()).thenReturn(clsp);
		when(dex.root()).thenReturn(rootNode);
	}

	@Test
	public void testMerge() throws IOException, DecodeException {
		first(INT, INT);
		first(BOOLEAN, INT);
		reject(INT, LONG);
		first(INT, UNKNOWN);
		reject(INT, UNKNOWN_OBJECT);

		first(INT, NARROW);
		first(CHAR, INT);

		check(unknown(PrimitiveType.INT, PrimitiveType.BOOLEAN, PrimitiveType.FLOAT),
				unknown(PrimitiveType.INT, PrimitiveType.BOOLEAN),
				unknown(PrimitiveType.INT, PrimitiveType.BOOLEAN));

		check(unknown(PrimitiveType.INT, PrimitiveType.FLOAT),
				unknown(PrimitiveType.INT, PrimitiveType.BOOLEAN),
				INT);

		check(unknown(PrimitiveType.INT, PrimitiveType.OBJECT),
				unknown(PrimitiveType.OBJECT, PrimitiveType.ARRAY),
				unknown(PrimitiveType.OBJECT));

		ArgType objExc = object("java.lang.Exception");
		ArgType objThr = object("java.lang.Throwable");
		ArgType objIO = object("java.io.IOException");
		ArgType objArr = object("java.lang.ArrayIndexOutOfBoundsException");
		ArgType objList = object("java.util.List");

		first(objExc, objExc);
		check(objExc, objList, OBJECT);
		first(objExc, OBJECT);

		check(objExc, objThr, objThr);
		check(objIO, objArr, objExc);

		ArgType generic = genericType("T");
		first(generic, objExc);
	}

	@Test
	public void testArrayMerge() {
		check(array(INT), array(BYTE), ArgType.OBJECT);
		first(array(INT), array(INT));
		first(array(STRING), array(STRING));

		first(OBJECT, array(INT));
		first(OBJECT, array(STRING));

		first(array(unknown(PrimitiveType.INT, PrimitiveType.FLOAT)), unknown(PrimitiveType.ARRAY));
	}

	private void first(ArgType t1, ArgType t2) {
		check(t1, t2, t1);
	}

	private void reject(ArgType t1, ArgType t2) {
		check(t1, t2, null);
	}

	private void check(ArgType t1, ArgType t2, ArgType exp) {
		merge(t1, t2, exp);
		merge(t2, t1, exp);
	}

	private void merge(ArgType t1, ArgType t2, ArgType exp) {
		ArgType res = ArgType.merge(dex, t1, t2);
		String msg = format(t1, t2, exp, res);
		if (exp == null) {
			assertNull("Incorrect accept: " + msg, res);
		} else {
			assertNotNull("Incorrect reject: " + msg, res);
			assertTrue("Incorrect result: " + msg, exp.equals(res));
		}
	}

	private String format(ArgType t1, ArgType t2, ArgType exp, ArgType res) {
		return t1 + " <+> " + t2 + " = '" + res + "', expected: " + exp;
	}
}
