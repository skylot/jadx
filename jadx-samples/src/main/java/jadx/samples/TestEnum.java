package jadx.samples;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class TestEnum extends AbstractTest {

	public enum Direction {
		NORTH, SOUTH, EAST, WEST
	}

	public static final String DOG = "DOG";

	public enum Animal {
		CAT, DOG
	}

	private static int three = 3;

	public enum Numbers {
		ONE(1), TWO(2), THREE(three), FOUR(three + 1);

		private final int num;

		private Numbers(int n) {
			this.num = n;
		}

		public int getNum() {
			return num;
		}
	}

	public enum Operation {
		PLUS {
			@Override
			int apply(int x, int y) {
				return x + y;
			}
		},
		MINUS {
			@Override
			int apply(int x, int y) {
				return x - y;
			}
		};

		abstract int apply(int x, int y);
	}

	public interface IOps {
		double apply(double x, double y);
	}

	public enum DoubleOperations implements IOps {
		TIMES("*") {
			@Override
			public double apply(double x, double y) {
				return x * y;
			}
		},
		DIVIDE("/") {
			@Override
			public double apply(double x, double y) {
				return x / y;
			}
		};

		private final String op;

		private DoubleOperations(String op) {
			this.op = op;
		}

		public String getOp() {
			return op;
		}
	}

	public enum Types {
		INT, FLOAT,
		LONG, DOUBLE,
		OBJECT, ARRAY;

		private static Set<Types> primitives = EnumSet.of(INT, FLOAT, LONG, DOUBLE);
		public static List<Types> references = new ArrayList<>();

		static {
			references.add(OBJECT);
			references.add(ARRAY);
		}

		public static Set<Types> getPrimitives() {
			return primitives;
		}
	}

	public enum EmptyEnum {
		;

		public static String getOp() {
			return "op";
		}
	}

	public enum Singleton {
		INSTANCE;

		public String test(String arg) {
			return arg.concat("test");
		}
	}

	public String testEnumSwitch(final Direction color) {
		String d;
		switch (color) {
			case NORTH:
				d = "N";
				break;
			case SOUTH:
				d = "S";
				break;
			default:
				d = "<>";
				break;
		}
		return d;
	}

	@Override
	public boolean testRun() throws Exception {
		Direction d = Direction.EAST;
		assertTrue(d.toString().equals("EAST"));
		assertTrue(d.ordinal() == 2);
		assertTrue(Numbers.THREE.getNum() == 3);
		assertTrue(Operation.PLUS.apply(2, 2) == 4);
		assertTrue(DoubleOperations.TIMES.apply(1, 1) == 1);
		assertTrue(Types.getPrimitives().contains(Types.INT));
		assertTrue(Types.references.size() == 2);
		assertTrue(EmptyEnum.values().length == 0);
		assertTrue(EmptyEnum.getOp().equals("op"));
		assertTrue(Singleton.INSTANCE.test("a").equals("atest"));
		return true;
	}

	public static void main(String[] args) throws Exception {
		new TestEnum().testRun();
	}
}
