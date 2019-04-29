package jadx.core.dex.instructions.args;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jadx.core.Consts;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.RootNode;
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
	public static final ArgType OBJECT_ARRAY = array(OBJECT);

	public static final ArgType UNKNOWN = unknown(PrimitiveType.values());
	public static final ArgType UNKNOWN_OBJECT = unknown(PrimitiveType.OBJECT, PrimitiveType.ARRAY);
	public static final ArgType UNKNOWN_OBJECT_NO_ARRAY = unknown(PrimitiveType.OBJECT);
	public static final ArgType UNKNOWN_ARRAY = array(UNKNOWN);

	public static final ArgType NARROW = unknown(
			PrimitiveType.INT, PrimitiveType.FLOAT,
			PrimitiveType.BOOLEAN, PrimitiveType.SHORT, PrimitiveType.BYTE, PrimitiveType.CHAR,
			PrimitiveType.OBJECT, PrimitiveType.ARRAY);

	public static final ArgType NARROW_NUMBERS = unknown(
			PrimitiveType.BOOLEAN, PrimitiveType.INT, PrimitiveType.FLOAT,
			PrimitiveType.SHORT, PrimitiveType.BYTE, PrimitiveType.CHAR);

	public static final ArgType NARROW_INTEGRAL = unknown(
			PrimitiveType.INT, PrimitiveType.SHORT, PrimitiveType.BYTE, PrimitiveType.CHAR);

	public static final ArgType NARROW_NUMBERS_NO_BOOL = unknown(
			PrimitiveType.INT, PrimitiveType.FLOAT,
			PrimitiveType.SHORT, PrimitiveType.BYTE, PrimitiveType.CHAR);

	public static final ArgType NARROW_NUMBERS_NO_FLOAT = unknown(
			PrimitiveType.INT, PrimitiveType.BOOLEAN,
			PrimitiveType.SHORT, PrimitiveType.BYTE, PrimitiveType.CHAR);

	public static final ArgType WIDE = unknown(PrimitiveType.LONG, PrimitiveType.DOUBLE);

	public static final ArgType INT_FLOAT = unknown(PrimitiveType.INT, PrimitiveType.FLOAT);

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

	public static ArgType wildcard(ArgType obj, int bounds) {
		return new WildcardType(obj, bounds);
	}

	public static ArgType generic(String sign) {
		return new SignatureParser(sign).consumeType();
	}

	public static ArgType generic(String obj, ArgType... generics) {
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
		private List<ArgType> extendTypes;

		public GenericType(String obj) {
			super(obj);
		}

		@Override
		public boolean isGenericType() {
			return true;
		}

		@Override
		public List<ArgType> getExtendTypes() {
			return extendTypes;
		}

		@Override
		public void setExtendTypes(List<ArgType> extendTypes) {
			this.extendTypes = extendTypes;
		}
	}

	private static final class WildcardType extends ObjectType {
		private final ArgType type;
		private final int bounds;

		public WildcardType(ArgType obj, int bounds) {
			super(OBJECT.getObject());
			this.type = obj;
			this.bounds = bounds;
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
		 * <li>1 for upper bound (? extends A)</li>
		 * <li>0 no bounds (?)</li>
		 * <li>-1 for lower bound (? super A)</li>
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
			return "? " + (bounds == -1 ? "super" : "extends") + ' ' + type;
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
			super(outerType.getObject() + '$' + innerName);
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
			return super.toString() + '<' + Utils.arrayToStr(generics) + '>';
		}
	}

	private static final class ArrayArg extends KnownType {
		private static final PrimitiveType[] ARRAY_POSSIBLES = new PrimitiveType[] { PrimitiveType.ARRAY };
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
		boolean internalEquals(Object other) {
			ArrayArg otherArr = (ArrayArg) other;
			return this.arrayElement.equals(otherArr.getArrayElement());
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
			if (contains(PrimitiveType.OBJECT)) {
				return OBJECT;
			}
			if (contains(PrimitiveType.ARRAY)) {
				return array(OBJECT);
			}
			return primitive(possibleTypes[0]);
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
				return "?[" + Utils.arrayToStr(possibleTypes) + ']';
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

	public List<ArgType> getExtendTypes() {
		return Collections.emptyList();
	}

	public void setExtendTypes(List<ArgType> extendTypes) {
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

	public static boolean isInstanceOf(RootNode root, ArgType type, ArgType of) {
		if (type.equals(of)) {
			return true;
		}
		if (!type.isObject() || !of.isObject()) {
			return false;
		}
		return root.getClsp().isImplements(type.getObject(), of.getObject());
	}

	public static boolean isClsKnown(RootNode root, ArgType cls) {
		if (cls.isObject()) {
			return root.getClsp().isClsKnown(cls.getObject());
		}
		return false;
	}

	public boolean canBeObject() {
		return isObject() || (!isTypeKnown() && contains(PrimitiveType.OBJECT));
	}

	public boolean canBeArray() {
		return isArray() || (!isTypeKnown() && contains(PrimitiveType.ARRAY));
	}

	public boolean canBePrimitive(PrimitiveType primitiveType) {
		return (isPrimitive() && getPrimitiveType() == primitiveType)
				|| (!isTypeKnown() && contains(primitiveType));
	}

	public static ArgType convertFromPrimitiveType(PrimitiveType primitiveType) {
		switch (primitiveType) {
			case BOOLEAN:
				return BOOLEAN;
			case CHAR:
				return CHAR;
			case BYTE:
				return BYTE;
			case SHORT:
				return SHORT;
			case INT:
				return INT;
			case FLOAT:
				return FLOAT;
			case LONG:
				return LONG;
			case DOUBLE:
				return DOUBLE;
			case OBJECT:
				return OBJECT;
			case ARRAY:
				return OBJECT_ARRAY;
			case VOID:
				return ArgType.VOID;
		}
		return OBJECT;
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
