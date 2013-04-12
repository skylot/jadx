package jadx.dex.instructions.args;

import jadx.Consts;
import jadx.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class ArgType {
	public static final ArgType INT = primitive(PrimitiveType.INT);
	public static final ArgType BOOLEAN = primitive(PrimitiveType.BOOLEAN);
	public static final ArgType BYTE = primitive(PrimitiveType.BYTE);
	public static final ArgType SHORT = primitive(PrimitiveType.SHORT);
	public static final ArgType CHAR = primitive(PrimitiveType.CHAR);
	public static final ArgType FLOAT = primitive(PrimitiveType.FLOAT);
	public static final ArgType DOUBLE = primitive(PrimitiveType.DOUBLE);
	public static final ArgType LONG = primitive(PrimitiveType.LONG);
	public static final ArgType VOID = primitive(PrimitiveType.VOID);

	public static final ArgType OBJECT = object(Consts.CLASS_OBJECT);
	public static final ArgType CLASS = object(Consts.CLASS_CLASS);
	public static final ArgType STRING = object(Consts.CLASS_STRING);
	public static final ArgType THROWABLE = object(Consts.CLASS_THROWABLE);

	public static final ArgType UNKNOWN = unknown(PrimitiveType.values());

	public static final ArgType NARROW = unknown(
			PrimitiveType.INT, PrimitiveType.FLOAT,
			PrimitiveType.BOOLEAN, PrimitiveType.SHORT, PrimitiveType.BYTE, PrimitiveType.CHAR,
			PrimitiveType.OBJECT, PrimitiveType.ARRAY);

	public static final ArgType WIDE = unknown(PrimitiveType.LONG, PrimitiveType.DOUBLE);

	public static final ArgType UNKNOWN_OBJECT = unknown(PrimitiveType.OBJECT, PrimitiveType.ARRAY);

	protected int hash;

	private static ArgType primitive(PrimitiveType stype) {
		return new PrimitiveArg(stype);
	}

	public static ArgType object(String obj) {
		return new ObjectArg(Utils.cleanObjectName(obj));
	}

	public static ArgType generic(String obj, String signature) {
		return new GenericObjectArg(obj, signature);
	}

	public static ArgType array(ArgType vtype) {
		return new ArrayArg(vtype);
	}

	public static ArgType unknown(PrimitiveType... types) {
		return new UnknownArg(types);
	}

	private static abstract class KnownTypeArg extends ArgType {
		@Override
		public boolean isTypeKnown() {
			return true;
		}
	}

	private static final class PrimitiveArg extends KnownTypeArg {
		private final PrimitiveType type;

		public PrimitiveArg(PrimitiveType type) {
			this.type = type;
			this.hash = type.hashCode();
		}

		@Override
		public PrimitiveType getPrimitiveType() {
			return type;
		}

		@Override
		public boolean isPrimitive() {
			return true;
		}

		@Override
		public String toString() {
			return type.toString();
		}
	}

	private static class ObjectArg extends KnownTypeArg {
		private final String object;

		public ObjectArg(String obj) {
			this.object = obj;
			this.hash = obj.hashCode();
		}

		@Override
		public String getObject() {
			return object;
		}

		@Override
		public boolean isObject() {
			return true;
		}

		@Override
		public PrimitiveType getPrimitiveType() {
			return PrimitiveType.OBJECT;
		}

		@Override
		public String toString() {
			return object;
		}
	}

	private static final class GenericObjectArg extends ObjectArg {
		private final ArgType[] generics;

		public GenericObjectArg(String obj, String signature) {
			super(obj);
			this.generics = parseSignature(signature);
			this.hash = obj.hashCode() + 31 * Arrays.hashCode(generics);
		}

		@Override
		public ArgType[] getGenericTypes() {
			return generics;
		}

		@Override
		public String toString() {
			return super.toString() + "<" + Arrays.toString(generics) + ">";
		}
	}

	private static final class ArrayArg extends KnownTypeArg {
		private final ArgType arrayElement;

		public ArrayArg(ArgType arrayElement) {
			this.arrayElement = arrayElement;
			this.hash = arrayElement.hashCode();
		}

		@Override
		public ArgType getArrayElement() {
			return arrayElement;
		}

		@Override
		public boolean isArray() {
			return true;
		}

		@Override
		public PrimitiveType getPrimitiveType() {
			return PrimitiveType.ARRAY;
		}

		@Override
		public int getArrayDimension() {
			if (isArray())
				return 1 + arrayElement.getArrayDimension();
			else
				return 0;
		}

		@Override
		public ArgType getArrayRootElement() {
			if (isArray())
				return arrayElement.getArrayRootElement();
			else
				return this;
		}

		@Override
		public String toString() {
			return arrayElement.toString();
		}
	}

	private static final class UnknownArg extends ArgType {
		private final PrimitiveType possibleTypes[];

		public UnknownArg(PrimitiveType[] types) {
			this.possibleTypes = types;
			this.hash = Arrays.hashCode(possibleTypes);
		}

		@Override
		public PrimitiveType[] getPossibleTypes() {
			return possibleTypes;
		}

		@Override
		public boolean isTypeKnown() {
			return false;
		}

		@Override
		public boolean contains(PrimitiveType type) {
			for (PrimitiveType t : possibleTypes)
				if (t == type)
					return true;
			return false;
		}

		@Override
		public ArgType selectFirst() {
			assert possibleTypes != null;
			PrimitiveType f = possibleTypes[0];
			if (f == PrimitiveType.OBJECT || f == PrimitiveType.ARRAY)
				return object(Consts.CLASS_OBJECT);
			else
				return primitive(f);
		}

		@Override
		public String toString() {
			if (possibleTypes.length == PrimitiveType.values().length)
				return "*";
			else
				return "?" + Arrays.toString(possibleTypes);
		}
	}

	public boolean isTypeKnown() {
		return false;
	}

	public PrimitiveType getPrimitiveType() {
		return null;
	}

	public boolean isPrimitive() {
		return false;
	}

	public String getObject() {
		throw new UnsupportedOperationException();
	}

	public boolean isObject() {
		return false;
	}

	public ArgType[] getGenericTypes() {
		return null;
	}

	public ArgType getArrayElement() {
		return null;
	}

	public boolean isArray() {
		return false;
	}

	public int getArrayDimension() {
		return 0;
	}

	public ArgType getArrayRootElement() {
		return this;
	}

	public boolean contains(PrimitiveType type) {
		throw new UnsupportedOperationException();
	}

	public ArgType selectFirst() {
		throw new UnsupportedOperationException();
	}

	public PrimitiveType[] getPossibleTypes() {
		return null;
	}

	public static ArgType merge(ArgType a, ArgType b) {
		if (a == b)
			return a;

		if (b == null || a == null)
			return null;

		ArgType res = mergeInternal(a, b);
		if (res == null)
			res = mergeInternal(b, a); // swap
		return res;
	}

	private static ArgType mergeInternal(ArgType a, ArgType b) {
		if (a == UNKNOWN)
			return b;

		if (!a.isTypeKnown()) {
			if (b.isTypeKnown()) {
				if (a.contains(b.getPrimitiveType()))
					return b;
				else
					return null;
			} else {
				// both types unknown
				List<PrimitiveType> types = new ArrayList<PrimitiveType>();
				for (PrimitiveType type : a.getPossibleTypes()) {
					if (b.contains(type))
						types.add(type);
				}
				if (types.size() == 0) {
					return null;
				} else if (types.size() == 1) {
					PrimitiveType nt = types.get(0);
					if (nt == PrimitiveType.OBJECT || nt == PrimitiveType.ARRAY)
						return unknown(nt);
					else
						return primitive(nt);
				} else {
					return unknown(types.toArray(new PrimitiveType[types.size()]));
				}
			}
		} else {
			if (a.isObject() && b.isObject()) {
				if (a.getObject().equals(b.getObject())) {
					if (a.getGenericTypes() != null)
						return a;
					else
						return b;
				} else if (a.getObject().equals(OBJECT.getObject()))
					return b;
				else if (b.getObject().equals(OBJECT.getObject()))
					return a;
				else
					// different objects
					return OBJECT;
				// return null;
			}

			if (a.isArray() && b.isArray()) {
				ArgType res = merge(a.getArrayElement(), b.getArrayElement());
				return (res == null ? null : ArgType.array(res));
			}

			if (a.isPrimitive() && b.isPrimitive()) {
				if (a.getRegCount() == b.getRegCount())
					// return primitive(PrimitiveType.getWidest(a.getPrimitiveType(), b.getPrimitiveType()));
					return primitive(PrimitiveType.getSmaller(a.getPrimitiveType(), b.getPrimitiveType()));
			}
		}
		return null;
	}

	public static ArgType parse(String type) {
		assert type.length() > 0 : "Empty type";
		char f = type.charAt(0);
		if (f == 'L') {
			return object(type);
		} else if (f == '[') {
			return array(parse(type.substring(1)));
		} else {
			return parse(f);
		}
	}

	public static ArgType[] parseSignature(String signature) {
		int b = signature.indexOf('<') + 1;
		int e = signature.lastIndexOf('>');
		String gens = signature.substring(b, e);
		String[] split = gens.split(";");
		ArgType[] result = new ArgType[split.length];
		for (int i = 0; i < split.length; i++) {
			String g = split[i];
			switch (g.charAt(0)) {
				case 'L':
					result[i] = object(g + ";");
					break;

				case '*':
				case '?':
					result[i] = UNKNOWN;
					break;

				default:
					result[i] = UNKNOWN_OBJECT;
					break;
			}
		}
		return result;
	}

	private static ArgType parse(char f) {
		switch (f) {
			case 'Z':
				return BOOLEAN;
			case 'B':
				return BYTE;
			case 'C':
				return CHAR;
			case 'S':
				return SHORT;
			case 'I':
				return INT;
			case 'J':
				return LONG;
			case 'F':
				return FLOAT;
			case 'D':
				return DOUBLE;
			case 'V':
				return VOID;
		}
		throw new RuntimeException("Unknown type: " + f);
	}

	public int getRegCount() {
		if (isPrimitive()) {
			PrimitiveType type = getPrimitiveType();
			if (type == PrimitiveType.LONG || type == PrimitiveType.DOUBLE)
				return 2;
		}
		return 1;
	}

	@Override
	public String toString() {
		return "UNKNOWN";
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (hash != obj.hashCode()) return false;
		if (getClass() != obj.getClass()) {
			return false;
		}
		// TODO: don't use toString
		if (!toString().equals(obj.toString())) {
			return false;
		}
		return true;
	}

}
