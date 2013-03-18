package jadx.dex.instructions.args;

import jadx.Consts;
import jadx.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ArgType {

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

	private final PrimitiveType type;
	private final String object;
	private final ArgType arrayElement;
	private final PrimitiveType possibleTypes[];

	private final int hash;

	private ArgType(PrimitiveType type, String object, ArgType arrayElement) {
		this.type = type;
		this.object = (object == null ? null : Utils.cleanObjectName(object));
		this.arrayElement = arrayElement;
		this.possibleTypes = null;
		this.hash = calcHashCode();
	}

	private ArgType(PrimitiveType[] posTypes) {
		this.type = null;
		this.object = null;
		this.arrayElement = null;
		this.possibleTypes = posTypes;
		this.hash = calcHashCode();
	}

	public static ArgType primitive(PrimitiveType stype) {
		assert stype != PrimitiveType.OBJECT && stype != PrimitiveType.ARRAY;
		return new ArgType(stype, null, null);
	}

	public static ArgType object(String obj) {
		assert obj != null;
		return new ArgType(PrimitiveType.OBJECT, obj, null);
	}

	public static ArgType array(ArgType vtype) {
		return new ArgType(PrimitiveType.ARRAY, null, vtype);
	}

	public static ArgType unknown(PrimitiveType... types) {
		return new ArgType(types);
	}

	public boolean isTypeKnown() {
		return type != null;
	}

	public PrimitiveType getPrimitiveType() {
		return type;
	}

	public boolean isPrimitive() {
		return type != null && type != PrimitiveType.OBJECT && type != PrimitiveType.ARRAY;
	}

	public String getObject() {
		return object;
	}

	public boolean isObject() {
		return type == PrimitiveType.OBJECT;
	}

	public ArgType getArrayElement() {
		return arrayElement;
	}

	public boolean isArray() {
		return type == PrimitiveType.ARRAY;
	}

	public int getArrayDimension() {
		if (isArray())
			return 1 + arrayElement.getArrayDimension();
		else
			return 0;
	}

	public ArgType getArrayRootElement() {
		if (isArray())
			return arrayElement.getArrayRootElement();
		else
			return this;
	}

	public boolean contains(PrimitiveType type) {
		for (PrimitiveType t : possibleTypes)
			if (t == type)
				return true;
		return false;
	}

	public ArgType selectFirst() {
		assert possibleTypes != null;
		PrimitiveType f = possibleTypes[0];
		if (f == PrimitiveType.OBJECT || f == PrimitiveType.ARRAY)
			return object(Consts.CLASS_OBJECT);
		else
			return primitive(f);
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

		if (a.possibleTypes != null) {
			if (b.isTypeKnown()) {
				if (a.contains(b.getPrimitiveType()))
					return b;
				else
					return null;
			} else {
				// both types unknown
				List<PrimitiveType> types = new ArrayList<PrimitiveType>();
				for (PrimitiveType type : a.possibleTypes) {
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
				if (a.getObject().equals(b.getObject()))
					return a;
				else if (a.getObject().equals(OBJECT.getObject()))
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
		if (f == 'L')
			return object(type);
		else if (f == '[')
			return array(parse(type.substring(1)));
		else
			return parse(f);
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
		if (type == PrimitiveType.LONG || type == PrimitiveType.DOUBLE)
			return 2;
		else
			return 1;
	}

	@Override
	public String toString() {
		if (this == UNKNOWN)
			return "ANY";

		if (type != null) {
			if (type == PrimitiveType.OBJECT)
				return object;
			else if (type == PrimitiveType.ARRAY)
				return arrayElement + "[]";
			else
				return type.toString();
		} else {
			return "?" + Arrays.asList(possibleTypes).toString();
		}
	}

	private int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((object == null) ? 0 : object.hashCode());
		result = prime * result + ((arrayElement == null) ? 0 : arrayElement.hashCode());
		result = prime * result + Arrays.hashCode(possibleTypes);
		return result;
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
		if (getClass() != obj.getClass()) return false;
		ArgType other = (ArgType) obj;
		if (type != other.type) return false;
		if (!Arrays.equals(possibleTypes, other.possibleTypes)) return false;
		if (arrayElement == null) {
			if (other.arrayElement != null) return false;
		} else if (!arrayElement.equals(other.arrayElement)) return false;
		if (object == null) {
			if (other.object != null) return false;
		} else if (!object.equals(other.object)) return false;
		return true;
	}

}
