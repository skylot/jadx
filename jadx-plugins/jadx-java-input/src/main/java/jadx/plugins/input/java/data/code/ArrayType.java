package jadx.plugins.input.java.data.code;

import jadx.plugins.input.java.utils.JavaClassParseException;

public class ArrayType {

	public static String byValue(int val) {
		switch (val) {
			case 4:
				return "Z";
			case 5:
				return "C";
			case 6:
				return "F";
			case 7:
				return "D";
			case 8:
				return "B";
			case 9:
				return "S";
			case 10:
				return "I";
			case 11:
				return "J";

			default:
				throw new JavaClassParseException("Unknown array type value: " + val);
		}
	}
}
