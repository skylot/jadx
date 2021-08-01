package jadx.plugins.input.java.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jadx.plugins.input.java.data.JavaMethodProto;

public class DescriptorParser {

	public static void fillMethodProto(String mthDesc, JavaMethodProto mthProto) {
		new DescriptorParser(mthDesc).parseMethodDescriptor(mthProto);
	}

	public static JavaMethodProto parseToMethodProto(String mthDesc) {
		JavaMethodProto mthProto = new JavaMethodProto();
		new DescriptorParser(mthDesc).parseMethodDescriptor(mthProto);
		return mthProto;
	}

	private final String desc;
	private int pos;

	private DescriptorParser(String desc) {
		this.desc = desc;
	}

	private void parseMethodDescriptor(JavaMethodProto mthProto) {
		validate('(');
		if (check(')')) {
			mthProto.setArgTypes(Collections.emptyList());
		} else {
			mthProto.setArgTypes(readArgsList());
		}
		validate(')');
		mthProto.setReturnType(readType());
	}

	private List<String> readArgsList() {
		List<String> list = new ArrayList<>(5);
		do {
			list.add(readType());
		} while (!check(')'));
		return list;
	}

	private String readType() {
		int cur = pos;
		if (cur >= desc.length()) {
			return null;
		}
		char ch = desc.charAt(cur);
		switch (ch) {
			case 'L':
				int end = desc.indexOf(';', cur);
				if (end == -1) {
					throw new JavaClassParseException("Unexpected object type descriptor: " + desc);
				}
				int lastChar = end + 1;
				String type = desc.substring(cur, lastChar);
				pos = lastChar;
				return type;

			case '[':
				pos++;
				return "[" + readType();

			default:
				String primitiveType = parsePrimitiveType(ch);
				pos = cur + 1;
				return primitiveType;
		}
	}

	public String parsePrimitiveType(char f) {
		switch (f) {
			case 'Z':
				return "Z";
			case 'B':
				return "B";
			case 'C':
				return "C";
			case 'S':
				return "S";
			case 'I':
				return "I";
			case 'J':
				return "J";
			case 'F':
				return "F";
			case 'D':
				return "D";
			case 'V':
				return "V";

			default:
				throw new JavaClassParseException("Unexpected char '" + f + "' in descriptor " + desc);
		}
	}

	private boolean check(char exp) {
		return desc.charAt(pos) == exp;
	}

	private void validate(char exp) {
		if (!check(exp)) {
			throw new JavaClassParseException("Unexpected char in descriptor: " + desc + " at pos " + pos + ", expected: " + exp);
		}
		pos++;
	}
}
