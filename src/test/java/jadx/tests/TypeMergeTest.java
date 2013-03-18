package jadx.tests;

import static jadx.dex.instructions.args.ArgType.BOOLEAN;
import static jadx.dex.instructions.args.ArgType.CHAR;
import static jadx.dex.instructions.args.ArgType.INT;
import static jadx.dex.instructions.args.ArgType.LONG;
import static jadx.dex.instructions.args.ArgType.NARROW;
import static jadx.dex.instructions.args.ArgType.OBJECT;
import static jadx.dex.instructions.args.ArgType.UNKNOWN;
import static jadx.dex.instructions.args.ArgType.UNKNOWN_OBJECT;
import static jadx.dex.instructions.args.ArgType.object;
import static jadx.dex.instructions.args.ArgType.unknown;
import jadx.dex.instructions.args.ArgType;
import jadx.dex.instructions.args.PrimitiveType;
import junit.framework.TestCase;

public class TypeMergeTest extends TestCase {

	public void testMerge() {
		first(INT, INT);
		first(BOOLEAN, INT);
		reject(INT, LONG);
		first(INT, UNKNOWN);
		reject(INT, UNKNOWN_OBJECT);

		first(INT, NARROW);
		first(CHAR, INT);

		merge(unknown(PrimitiveType.INT, PrimitiveType.BOOLEAN, PrimitiveType.FLOAT),
				unknown(PrimitiveType.INT, PrimitiveType.BOOLEAN),
				unknown(PrimitiveType.INT, PrimitiveType.BOOLEAN));

		merge(unknown(PrimitiveType.INT, PrimitiveType.FLOAT),
				unknown(PrimitiveType.INT, PrimitiveType.BOOLEAN),
				INT);

		merge(unknown(PrimitiveType.INT, PrimitiveType.OBJECT),
				unknown(PrimitiveType.OBJECT, PrimitiveType.ARRAY),
				unknown(PrimitiveType.OBJECT));

		first(object("Lsomeobj;"), object("Lsomeobj;"));
		merge(object("Lsomeobj;"), object("Lotherobj;"), OBJECT);
		first(object("Lsomeobj;"), OBJECT);

		// first(object("Lsomeobj;"), object("Lsomeobj;"));
		// merge(object("Lsomeobj;"), object("Lotherobj;"), null);
		// merge(object("Lsomeobj;"), OBJECT, null);
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
		ArgType res = ArgType.merge(t1, t2);
		if (exp == null) {
			assertNull("Incorrect accept: " + format(t1, t2, exp, res), res);
		} else {
			assertNotNull("Incorrect reject: " + format(t1, t2, exp, res), res);
			assertTrue("Incorrect result: " + format(t1, t2, exp, res), exp.equals(res));
		}
	}

	private String format(ArgType t1, ArgType t2, ArgType exp, ArgType res) {
		return t1 + " <+> " + t2 + " = '" + res + "', expected: " + exp;
	}
}
