package jadx.core.dex.instructions.args;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.core.Consts;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.parser.SignatureParser;
import jadx.core.utils.Utils;

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
	public static final ArgType ENUM = object(Consts.CLASS_ENUM);
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

	private static ArgType primitive(PrimitiveType stype) {
		return new PrimitiveArg(stype);
	}

	public static ArgType object(String obj) {
		return new ObjectType(obj);
	}

	public static ArgType genericType(String type) {
		return new GenericType(type);
	}

	public static ArgType wildcard() {
		return new WildcardType(OBJECT, 0);
	}

	public static ArgType wildcard(ArgType obj, int bound) {
		return new WildcardType(obj, bound);
	}

	public static ArgType generic(String sign) {
		return new SignatureParser(sign).consumeType();
	}

	public static ArgType generic(String obj, ArgType[] generics) {
		return new GenericObject(obj, generics);
	}

	public static ArgType genericInner(ArgType genericType, String innerName, ArgType[] generics) {
		return new GenericObject((GenericObject) genericType, innerName, generics);
	}

	public static ArgType array(ArgType vtype) {
		return new ArrayArg(vtype);
	}

	public static ArgType unknown(PrimitiveType... types) {
		return new UnknownArg(types);
	}

	private abstract static class KnownType extends ArgType {

		private static final PrimitiveType[] EMPTY_POSSIBLES = new PrimitiveType[0];

		@Override
		public boolean isTypeKnown() {
			return true;
		}

		@Override
		public boolean contains(PrimitiveType type) {
			return getPrimitiveType() == type;
		}

		@Override
		public ArgType selectFirst() {
			return null;
		}

		@Override
		public PrimitiveType[] getPossibleTypes() {
			return EMPTY_POSSIBLES;
		}
	}

	private static final class PrimitiveArg extends KnownType {
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

	private static class ObjectType extends KnownType {
		private final String objName;

		public ObjectType(String obj) {
			this.objName = Utils.cleanObjectName(obj);
			this.hash = objName.hashCode();
		}

		@Override
		public String getObject() {
			return objName;
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
			return objName.equals(((ObjectType) obj).objName);
		}

		@Override
		public String toString() {
			return objName;
		}
	}

	private static final class GenericType extends ObjectType {
		public GenericType(String obj) {
			super(obj);
		}

		@Override
		public boolean isGenericType() {
			return true;
		}
	}

	private static final class WildcardType extends ObjectType {
		private final ArgType type;
		private final int bounds;

		public WildcardType(ArgType obj, int bound) {
			super(OBJECT.getObject());
			this.type = obj;
			this.bounds = bound;
		}

		@Override
		public boolean isGeneric() {
			return true;
		}

		@Override
		public ArgType getWildcardType() {
			return type;
		}

		/**
		 * Return wildcard bounds:
		 * <ul>
		 * <li> 1 for upper bound (? extends A) </li>
		 * <li> 0  no bounds (?) </li>
		 * <li>-1  for lower bound (? super A) </li>
		 * </ul>
		 */
		@Override
		public int getWildcardBounds() {
			return bounds;
		}

		@Override
		boolean internalEquals(Object obj) {
			return super.internalEquals(obj)
					&& bounds == ((WildcardType) obj).bounds
					&& type.equals(((WildcardType) obj).type);
		}

		@Override
		public String toString() {
			if (bounds == 0) {
				return "?";
			}
			return "? " + (bounds == -1 ? "super" : "extends") + " " + type;
		}
	}

	private static class GenericObject extends ObjectType {
		private final ArgType[] generics;
		private final GenericObject outerType;

		public GenericObject(String obj, ArgType[] generics) {
			super(obj);
			this.outerType = null;
			this.generics = generics;
			this.hash = obj.hashCode() + 31 * Arrays.hashCode(generics);
		}

		public GenericObject(GenericObject outerType, String innerName, ArgType[] generics) {
			super(outerType.getObject() + "$" + innerName);
			this.outerType = outerType;
			this.generics = generics;
			this.hash = outerType.hashCode() + 31 * innerName.hashCode()
					+ 31 * 31 * Arrays.hashCode(generics);
		}

		@Override
		public boolean isGeneric() {
			return true;
		}

		@Override
		public ArgType[] getGenericTypes() {
			return generics;
		}

		@Override
		public ArgType getOuterType() {
			return outerType;
		}

		@Override
		boolean internalEquals(Object obj) {
			return super.internalEquals(obj)
					&& Arrays.equals(generics, ((GenericObject) obj).generics);
		}

		@Override
		public String toString() {
			return super.toString() + "<" + Utils.arrayToString(generics) + ">";
		}
	}

	private static final class ArrayArg extends KnownType {
		private static final PrimitiveType[] ARRAY_POSSIBLES = new PrimitiveType[]{PrimitiveType.ARRAY};
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
		public boolean isTypeKnown() {
			return arrayElement.isTypeKnown();
		}

		@Override
		public ArgType selectFirst() {
			return array(arrayElement.selectFirst());
		}

		@Override
		public PrimitiveType[] getPossibleTypes() {
			return ARRAY_POSSIBLES;
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
			return arrayElement + "[]";
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
			if (contains(PrimitiveType.OBJECT)) {
				return OBJECT;
			} else if (contains(PrimitiveType.ARRAY)) {
				return array(OBJECT);
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
		throw new UnsupportedOperationException("ArgType.getObject(), call class: " + this.getClass());
	}

	public boolean isObject() {
		return false;
	}

	public boolean isGeneric() {
		return false;
	}

	public boolean isGenericType() {
		return false;
	}

	public ArgType[] getGenericTypes() {
		return null;
	}

	public ArgType getWildcardType() {
		return null;
	}

	/**
	 * @see WildcardType#getWildcardBounds()
	 */
	public int getWildcardBounds() {
		return 0;
	}

	public ArgType getOuterType() {
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

	public abstract boolean contains(PrimitiveType type);

	public abstract ArgType selectFirst();

	public abstract PrimitiveType[] getPossibleTypes();

	@Nullable
	public static ArgType merge(@Nullable DexNode dex, ArgType a, ArgType b) {
		if (a == null || b == null) {
			return null;
		}
		if (a.equals(b)) {
			return a;
		}
		ArgType res = mergeInternal(dex, a, b);
		if (res == null) {
			res = mergeInternal(dex, b, a); // swap
		}
		return res;
	}

	private static ArgType mergeInternal(@Nullable DexNode dex, ArgType a, ArgType b) {
		if (a == UNKNOWN) {
			return b;
		}
		if (a.isArray()) {
			return mergeArrays(dex, (ArrayArg) a, b);
		} else if (b.isArray()) {
			return mergeArrays(dex, (ArrayArg) b, a);
		}
		if (!a.isTypeKnown()) {
			if (b.isTypeKnown()) {
				if (a.contains(b.getPrimitiveType())) {
					return b;
				}
				return null;
			} else {
				// both types unknown
				List<PrimitiveType> types = new ArrayList<>();
				for (PrimitiveType type : a.getPossibleTypes()) {
					if (b.contains(type)) {
						types.add(type);
					}
				}
				if (types.isEmpty()) {
					return null;
				}
				if (types.size() == 1) {
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
					return a.getGenericTypes() != null ? a : b;
				}
				if (aObj.equals(Consts.CLASS_OBJECT)) {
					return b;
				}
				if (bObj.equals(Consts.CLASS_OBJECT)) {
					return a;
				}
				if (dex == null) {
					return null;
				}
				String obj = dex.root().getClsp().getCommonAncestor(aObj, bObj);
				return obj == null ? null : object(obj);
			}
			if (a.isPrimitive() && b.isPrimitive() && a.getRegCount() == b.getRegCount()) {
				return primitive(PrimitiveType.getSmaller(a.getPrimitiveType(), b.getPrimitiveType()));
			}
		}
		return null;
	}

	private static ArgType mergeArrays(DexNode dex, ArrayArg array, ArgType b) {
		if (b.isArray()) {
			ArgType ea = array.getArrayElement();
			ArgType eb = b.getArrayElement();
			if (ea.isPrimitive() && eb.isPrimitive()) {
				return OBJECT;
			}
			ArgType res = merge(dex, ea, eb);
			return res == null ? null : array(res);
		}
		if (b.contains(PrimitiveType.ARRAY)) {
			return array;
		}
		if (b.equals(OBJECT)) {
			return OBJECT;
		}
		return null;
	}

	public static boolean isCastNeeded(DexNode dex, ArgType from, ArgType to) {
		if (from.equals(to)) {
			return false;
		}
		if (from.isObject() && to.isObject()
				&& dex.root().getClsp().isImplements(from.getObject(), to.getObject())) {
			return false;
		}
		return true;
	}

	public static boolean isInstanceOf(DexNode dex, ArgType type, ArgType of) {
		if (type.equals(of)) {
			return true;
		}
		if (!type.isObject() || !of.isObject()) {
			return false;
		}
		return dex.root().getClsp().isImplements(type.getObject(), of.getObject());
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

	public static ArgType parse(char f) {
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

			default:
				return null;
		}
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
