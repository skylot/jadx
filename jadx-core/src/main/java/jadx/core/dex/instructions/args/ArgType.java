package jadx.core.dex.instructions.args;

import jadx.core.Consts;
import jadx.core.clsp.ClspGraph;
import jadx.core.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ArgType {
	private static final Logger LOG = LoggerFactory.getLogger(ArgType.class);

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
	public static final ArgType UNKNOWN_OBJECT = unknown(PrimitiveType.OBJECT, PrimitiveType.ARRAY);

	public static final ArgType NARROW = unknown(
			PrimitiveType.INT, PrimitiveType.FLOAT,
			PrimitiveType.BOOLEAN, PrimitiveType.SHORT, PrimitiveType.BYTE, PrimitiveType.CHAR,
			PrimitiveType.OBJECT, PrimitiveType.ARRAY);

	public static final ArgType NARROW_NUMBERS = unknown(
			PrimitiveType.INT, PrimitiveType.FLOAT,
			PrimitiveType.BOOLEAN, PrimitiveType.SHORT, PrimitiveType.BYTE, PrimitiveType.CHAR);

	public static final ArgType WIDE = unknown(PrimitiveType.LONG, PrimitiveType.DOUBLE);

	protected int hash;

	private static ClspGraph clsp;

	public static void setClsp(ClspGraph clsp) {
		ArgType.clsp = clsp;
	}

	private static ArgType primitive(PrimitiveType stype) {
		return new PrimitiveArg(stype);
	}

	public static ArgType object(String obj) {
		return new ObjectArg(obj);
	}

	public static ArgType genericType(String type) {
		return new GenericTypeArg(type);
	}

	public static ArgType generic(String sign) {
		return parseSignature(sign);
	}

	public static ArgType generic(String obj, ArgType[] generics) {
		return new GenericObjectArg(obj, generics);
	}

	public static ArgType array(ArgType vtype) {
		return new ArrayArg(vtype);
	}

	public static ArgType unknown(PrimitiveType... types) {
		return new UnknownArg(types);
	}

	private abstract static class KnownTypeArg extends ArgType {
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
		boolean internalEquals(Object obj) {
			return type == ((PrimitiveArg) obj).type;
		}

		@Override
		public String toString() {
			return type.toString();
		}
	}

	private static class ObjectArg extends KnownTypeArg {
		private final String object;

		public ObjectArg(String obj) {
			this.object = Utils.cleanObjectName(obj);
			this.hash = object.hashCode();
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
		boolean internalEquals(Object obj) {
			return object.equals(((ObjectArg) obj).object);
		}

		@Override
		public String toString() {
			return object;
		}
	}

	private static final class GenericTypeArg extends ObjectArg {
		public GenericTypeArg(String obj) {
			super(obj);
		}

		@Override
		public boolean isGenericType() {
			return true;
		}
	}

	private static final class GenericObjectArg extends ObjectArg {
		private final ArgType[] generics;

		public GenericObjectArg(String obj, ArgType[] generics) {
			super(obj);
			this.generics = generics;
			this.hash = obj.hashCode() + 31 * Arrays.hashCode(generics);
		}

		@Override
		public ArgType[] getGenericTypes() {
			return generics;
		}

		@Override
		boolean internalEquals(Object obj) {
			return super.internalEquals(obj)
					&& Arrays.equals(generics, ((GenericObjectArg) obj).generics);
		}

		@Override
		public String toString() {
			return super.toString() + "<" + Utils.arrayToString(generics) + ">";
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
			return 1 + arrayElement.getArrayDimension();
		}

		@Override
		public ArgType getArrayRootElement() {
			return arrayElement.getArrayRootElement();
		}

		@Override
		boolean internalEquals(Object obj) {
			return arrayElement.equals(((ArrayArg) obj).arrayElement);
		}

		@Override
		public String toString() {
			return arrayElement.toString() + "[]";
		}
	}

	private static final class UnknownArg extends ArgType {
		private final PrimitiveType[] possibleTypes;

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
			for (PrimitiveType t : possibleTypes) {
				if (t == type) {
					return true;
				}
			}
			return false;
		}

		@Override
		public ArgType selectFirst() {
			PrimitiveType f = possibleTypes[0];
			if (f == PrimitiveType.OBJECT || f == PrimitiveType.ARRAY) {
				return object(Consts.CLASS_OBJECT);
			} else {
				return primitive(f);
			}
		}

		@Override
		boolean internalEquals(Object obj) {
			return Arrays.equals(possibleTypes, ((UnknownArg) obj).possibleTypes);
		}

		@Override
		public String toString() {
			if (possibleTypes.length == PrimitiveType.values().length) {
				return "?";
			} else {
				return "?" + Arrays.toString(possibleTypes);
			}
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
		throw new UnsupportedOperationException("ArgType.getObject()");
	}

	public boolean isObject() {
		return false;
	}

	public boolean isGenericType() {
		return false;
	}

	public ArgType[] getGenericTypes() {
		return null;
	}

	public boolean isArray() {
		return false;
	}

	public int getArrayDimension() {
		return 0;
	}

	public ArgType getArrayElement() {
		return null;
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
		if (b == null || a == null) {
			return null;
		}
		if (a.equals(b)) {
			return a;
		}
		ArgType res = mergeInternal(a, b);
		if (res == null) {
			res = mergeInternal(b, a); // swap
		}
		return res;
	}

	private static ArgType mergeInternal(ArgType a, ArgType b) {
		if (a == UNKNOWN) {
			return b;
		}
		if (!a.isTypeKnown()) {
			if (b.isTypeKnown()) {
				if (a.contains(b.getPrimitiveType())) {
					return b;
				} else {
					return null;
				}
			} else {
				// both types unknown
				List<PrimitiveType> types = new ArrayList<PrimitiveType>();
				for (PrimitiveType type : a.getPossibleTypes()) {
					if (b.contains(type)) {
						types.add(type);
					}
				}
				if (types.size() == 0) {
					return null;
				} else if (types.size() == 1) {
					PrimitiveType nt = types.get(0);
					if (nt == PrimitiveType.OBJECT || nt == PrimitiveType.ARRAY) {
						return unknown(nt);
					} else {
						return primitive(nt);
					}
				} else {
					return unknown(types.toArray(new PrimitiveType[types.size()]));
				}
			}
		} else {
			if (a.isGenericType()) {
				return a;
			}
			if (b.isGenericType()) {
				return b;
			}

			if (a.isObject() && b.isObject()) {
				String aObj = a.getObject();
				String bObj = b.getObject();
				if (aObj.equals(bObj)) {
					return (a.getGenericTypes() != null ? a : b);
				} else if (aObj.equals(Consts.CLASS_OBJECT)) {
					return b;
				} else if (bObj.equals(Consts.CLASS_OBJECT)) {
					return a;
				} else {
					// different objects
					String obj = clsp.getCommonAncestor(aObj, bObj);
					return (obj == null ? null : object(obj));
				}
			}
			if (a.isArray()) {
				if (b.isArray()) {
					ArgType ea = a.getArrayElement();
					ArgType eb = b.getArrayElement();
					if (ea.isPrimitive() && eb.isPrimitive()) {
						return OBJECT;
					} else {
						ArgType res = merge(ea, eb);
						return (res == null ? null : ArgType.array(res));
					}
				} else if (b.equals(OBJECT)) {
					return OBJECT;
				} else {
					return null;
				}
			}
			if (a.isPrimitive() && b.isPrimitive() && a.getRegCount() == b.getRegCount()) {
				return primitive(PrimitiveType.getSmaller(a.getPrimitiveType(), b.getPrimitiveType()));
			}
		}
		return null;
	}

	public static boolean isCastNeeded(ArgType from, ArgType to) {
		if (from.equals(to)) {
			return false;
		}
		if (from.isObject() && to.isObject()
				&& clsp.isImplements(from.getObject(), to.getObject())) {
			return false;
		}
		return true;
	}

	public static ArgType parse(String type) {
		char f = type.charAt(0);
		switch (f) {
			case 'L':
				return object(type);
			case 'T':
				return genericType(type.substring(1, type.length() - 1));
			case '[':
				return array(parse(type.substring(1)));
			default:
				return parse(f);
		}
	}

	public static ArgType parseSignature(String sign) {
		int b = sign.indexOf('<');
		if (b == -1) {
			return parse(sign);
		}
		if (sign.charAt(0) == '[') {
			return array(parseSignature(sign.substring(1)));
		}
		String obj = sign.substring(0, b) + ";";
		String genericsStr = sign.substring(b + 1, sign.length() - 2);
		List<ArgType> generics = parseSignatureList(genericsStr);
		if (generics != null) {
			return generic(obj, generics.toArray(new ArgType[generics.size()]));
		} else {
			return object(obj);
		}
	}

	public static List<ArgType> parseSignatureList(String str) {
		try {
			return parseSignatureListInner(str, true);
		} catch (Throwable e) {
			LOG.warn("Signature parse exception: {}", str, e);
			return null;
		}
	}

	private static List<ArgType> parseSignatureListInner(String str, boolean parsePrimitives) {
		if (str.isEmpty()) {
			return Collections.emptyList();
		}
		if (str.equals("*")) {
			return Arrays.asList(UNKNOWN);
		}
		List<ArgType> signs = new ArrayList<ArgType>(3);
		int obj = 0;
		int objStart = 0;
		int gen = 0;
		int arr = 0;

		int pos = 0;
		ArgType type = null;
		while (pos < str.length()) {
			char c = str.charAt(pos);
			switch (c) {
				case 'L':
				case 'T':
					if (obj == 0 && gen == 0) {
						obj++;
						objStart = pos;
					}
					break;

				case ';':
					if (obj == 1 && gen == 0) {
						obj--;
						String o = str.substring(objStart, pos + 1);
						type = parseSignature(o);
					}
					break;

				case ':': // generic types map separator
					if (gen == 0) {
						obj = 0;
						String o = str.substring(objStart, pos);
						if (o.length() > 0) {
							type = genericType(o);
						}
					}
					break;

				case '<':
					gen++;
					break;
				case '>':
					gen--;
					break;

				case '[':
					if (obj == 0 && gen == 0) {
						arr++;
					}
					break;

				default:
					if (parsePrimitives && obj == 0 && gen == 0) {
						type = parse(c);
					}
					break;
			}

			if (type != null) {
				if (arr == 0) {
					signs.add(type);
				} else {
					for (int i = 0; i < arr; i++) {
						type = array(type);
					}
					signs.add(type);
					arr = 0;
				}
				type = null;
				objStart = pos + 1;
			}
			pos++;
		}
		return signs;
	}

	public static Map<ArgType, List<ArgType>> parseGenericMap(String gen) {
		try {
			Map<ArgType, List<ArgType>> genericMap = null;
			List<ArgType> genTypes = parseSignatureListInner(gen, false);
			if (genTypes != null) {
				genericMap = new LinkedHashMap<ArgType, List<ArgType>>(2);
				ArgType prev = null;
				List<ArgType> genList = new ArrayList<ArgType>(2);
				for (ArgType arg : genTypes) {
					if (arg.isGenericType()) {
						if (prev != null) {
							genericMap.put(prev, genList);
							genList = new ArrayList<ArgType>();
						}
						prev = arg;
					} else {
						if (!arg.getObject().equals(Consts.CLASS_OBJECT)) {
							genList.add(arg);
						}
					}
				}
				if (prev != null) {
					genericMap.put(prev, genList);
				}
			}
			return genericMap;
		} catch (Throwable e) {
			LOG.warn("Generic map parse exception: {}", gen, e);
			return null;
		}
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
		return null;
	}

	public int getRegCount() {
		if (isPrimitive()) {
			PrimitiveType type = getPrimitiveType();
			if (type == PrimitiveType.LONG || type == PrimitiveType.DOUBLE) {
				return 2;
			} else {
				return 1;
			}
		}
		if (!isTypeKnown()) {
			return 0;
		}
		return 1;
	}

	@Override
	public String toString() {
		return "ARG_TYPE";
	}

	@Override
	public int hashCode() {
		return hash;
	}

	abstract boolean internalEquals(Object obj);

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (hash != obj.hashCode()) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		return internalEquals(obj);
	}
}
