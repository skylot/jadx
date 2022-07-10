package jadx.core.dex.instructions.args;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import jadx.core.Consts;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.typeinference.TypeCompareEnum;
import jadx.core.utils.ListUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

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

	public static final ArgType OBJECT = objectNoCache(Consts.CLASS_OBJECT);
	public static final ArgType CLASS = objectNoCache(Consts.CLASS_CLASS);
	public static final ArgType STRING = objectNoCache(Consts.CLASS_STRING);
	public static final ArgType ENUM = objectNoCache(Consts.CLASS_ENUM);
	public static final ArgType THROWABLE = objectNoCache(Consts.CLASS_THROWABLE);
	public static final ArgType EXCEPTION = objectNoCache(Consts.CLASS_EXCEPTION);
	public static final ArgType OBJECT_ARRAY = array(OBJECT);
	public static final ArgType WILDCARD = wildcard();

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
	public static final ArgType INT_BOOLEAN = unknown(PrimitiveType.INT, PrimitiveType.BOOLEAN);
	public static final ArgType BYTE_BOOLEAN = unknown(PrimitiveType.BYTE, PrimitiveType.BOOLEAN);

	protected int hash;

	private static ArgType primitive(PrimitiveType stype) {
		return new PrimitiveArg(stype);
	}

	private static ArgType objectNoCache(String obj) {
		return new ObjectType(obj);
	}

	public static ArgType object(String obj) {
		// TODO: add caching
		String cleanObjectName = Utils.cleanObjectName(obj);
		switch (cleanObjectName) {
			case Consts.CLASS_OBJECT:
				return OBJECT;
			case Consts.CLASS_STRING:
				return STRING;
			case Consts.CLASS_CLASS:
				return CLASS;
			case Consts.CLASS_THROWABLE:
				return THROWABLE;
			case Consts.CLASS_EXCEPTION:
				return EXCEPTION;
			default:
				return new ObjectType(cleanObjectName);
		}
	}

	public static ArgType genericType(String type) {
		return new GenericType(type);
	}

	public static ArgType genericType(String type, ArgType extendType) {
		return new GenericType(type, extendType);
	}

	public static ArgType genericType(String type, List<ArgType> extendTypes) {
		return new GenericType(type, extendTypes);
	}

	public static ArgType wildcard() {
		return new WildcardType(OBJECT, WildcardBound.UNBOUND);
	}

	public static ArgType wildcard(ArgType obj, WildcardBound bound) {
		return new WildcardType(obj, bound);
	}

	public static ArgType generic(ArgType obj, List<ArgType> generics) {
		if (!obj.isObject()) {
			throw new IllegalArgumentException("Expected Object as ArgType, got: " + obj);
		}
		return new GenericObject(obj.getObject(), generics);
	}

	public static ArgType generic(ArgType obj, ArgType... generics) {
		return generic(obj, Arrays.asList(generics));
	}

	public static ArgType generic(String obj, List<ArgType> generics) {
		return new GenericObject(Utils.cleanObjectName(obj), generics);
	}

	public static ArgType generic(String obj, ArgType generic) {
		return generic(obj, Collections.singletonList(generic));
	}

	@TestOnly
	public static ArgType generic(String obj, ArgType... generics) {
		return generic(obj, Arrays.asList(generics));
	}

	public static ArgType outerGeneric(ArgType genericOuterType, ArgType innerType) {
		return new OuterGenericObject((ObjectType) genericOuterType, (ObjectType) innerType);
	}

	public static ArgType array(@NotNull ArgType vtype) {
		return new ArrayArg(vtype);
	}

	public static ArgType array(@NotNull ArgType type, int dimension) {
		if (dimension == 1) {
			return new ArrayArg(type);
		}
		ArgType arrType = type;
		for (int i = 0; i < dimension; i++) {
			arrType = new ArrayArg(arrType);
		}
		return arrType;
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
		protected final String objName;

		public ObjectType(String obj) {
			this.objName = obj;
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
			this(obj, Collections.emptyList());
		}

		public GenericType(String obj, ArgType extendType) {
			this(obj, Collections.singletonList(extendType));
		}

		public GenericType(String obj, List<ArgType> extendTypes) {
			super(obj);
			this.extendTypes = extendTypes;
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

		@Override
		boolean internalEquals(Object obj) {
			return super.internalEquals(obj)
					&& extendTypes.equals(((GenericType) obj).extendTypes);
		}

		@Override
		public String toString() {
			List<ArgType> extTypes = this.extendTypes;
			if (extTypes.isEmpty()) {
				return objName;
			}
			return objName + " extends " + Utils.listToString(extTypes, " & ");
		}
	}

	public enum WildcardBound {
		EXTENDS(1, "? extends "), // upper bound (? extends A)
		UNBOUND(0, "?"), // no bounds (?)
		SUPER(-1, "? super "); // lower bound (? super A)

		private final int num;
		private final String str;

		WildcardBound(int val, String str) {
			this.num = val;
			this.str = str;
		}

		public int getNum() {
			return num;
		}

		public String getStr() {
			return str;
		}

		public static WildcardBound getByNum(int num) {
			return num == 0 ? UNBOUND : (num == 1 ? EXTENDS : SUPER);
		}
	}

	private static final class WildcardType extends ObjectType {
		private final ArgType type;
		private final WildcardBound bound;

		public WildcardType(ArgType obj, WildcardBound bound) {
			super(OBJECT.getObject());
			this.type = Objects.requireNonNull(obj);
			this.bound = Objects.requireNonNull(bound);
		}

		@Override
		public boolean isWildcard() {
			return true;
		}

		@Override
		public boolean isGeneric() {
			return true;
		}

		@Override
		public ArgType getWildcardType() {
			return type;
		}

		@Override
		public WildcardBound getWildcardBound() {
			return bound;
		}

		@Override
		boolean internalEquals(Object obj) {
			return super.internalEquals(obj)
					&& bound == ((WildcardType) obj).bound
					&& type.equals(((WildcardType) obj).type);
		}

		@Override
		public String toString() {
			if (bound == WildcardBound.UNBOUND) {
				return bound.getStr();
			}
			return bound.getStr() + type;
		}
	}

	private static class GenericObject extends ObjectType {
		private final List<ArgType> generics;

		public GenericObject(String obj, List<ArgType> generics) {
			super(obj);
			this.generics = Objects.requireNonNull(generics);
			this.hash = calcHash();
		}

		private int calcHash() {
			return objName.hashCode() + 31 * generics.hashCode();
		}

		@Override
		public boolean isGeneric() {
			return true;
		}

		@Override
		public List<ArgType> getGenericTypes() {
			return generics;
		}

		@Override
		boolean internalEquals(Object obj) {
			return super.internalEquals(obj)
					&& Objects.equals(generics, ((GenericObject) obj).generics);
		}

		@Override
		public String toString() {
			return super.toString() + '<' + Utils.listToString(generics) + '>';
		}
	}

	private static class OuterGenericObject extends ObjectType {
		private final ObjectType outerType;
		private final ObjectType innerType;

		public OuterGenericObject(ObjectType outerType, ObjectType innerType) {
			super(outerType.getObject() + '$' + innerType.getObject());
			this.outerType = outerType;
			this.innerType = innerType;
			this.hash = calcHash();
		}

		private int calcHash() {
			return objName.hashCode() + 31 * (outerType.hashCode() + 31 * innerType.hashCode());
		}

		@Override
		public boolean isGeneric() {
			return true;
		}

		@Override
		public List<ArgType> getGenericTypes() {
			return innerType.getGenericTypes();
		}

		@Override
		public ArgType getOuterType() {
			return outerType;
		}

		@Override
		public ArgType getInnerType() {
			return innerType;
		}

		@Override
		boolean internalEquals(Object obj) {
			return super.internalEquals(obj)
					&& Objects.equals(outerType, ((OuterGenericObject) obj).outerType)
					&& Objects.equals(innerType, ((OuterGenericObject) obj).innerType);
		}

		@Override
		public String toString() {
			return outerType.toString() + '$' + innerType.toString();
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
				return "??";
			} else {
				return "??[" + Utils.arrayToStr(possibleTypes) + ']';
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

	public List<ArgType> getGenericTypes() {
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

	public WildcardBound getWildcardBound() {
		return null;
	}

	public boolean isWildcard() {
		return false;
	}

	public ArgType getOuterType() {
		return null;
	}

	public ArgType getInnerType() {
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

	public static boolean isCastNeeded(RootNode root, ArgType from, ArgType to) {
		if (from.equals(to)) {
			return false;
		}
		TypeCompareEnum result = root.getTypeCompare().compareTypes(from, to);
		return !result.isNarrow();
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

	public boolean canBeAnyNumber() {
		if (isPrimitive()) {
			return !getPrimitiveType().isObjectOrArray();
		}
		for (PrimitiveType primitiveType : getPossibleTypes()) {
			if (!primitiveType.isObjectOrArray()) {
				return true;
			}
		}
		return false;
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
		if (type == null || type.isEmpty()) {
			throw new JadxRuntimeException("Failed to parse type string: " + type);
		}
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

	public boolean containsGeneric() {
		if (isGeneric() || isGenericType()) {
			return true;
		}
		if (isArray()) {
			ArgType arrayElement = getArrayElement();
			if (arrayElement != null) {
				return arrayElement.containsGeneric();
			}
		}
		return false;
	}

	public boolean containsTypeVariable() {
		if (isGenericType()) {
			return true;
		}
		ArgType wildcardType = getWildcardType();
		if (wildcardType != null) {
			return wildcardType.containsTypeVariable();
		}
		if (isGeneric()) {
			List<ArgType> genericTypes = getGenericTypes();
			if (genericTypes != null) {
				for (ArgType genericType : genericTypes) {
					if (genericType.containsTypeVariable()) {
						return true;
					}
				}
			}
			ArgType outerType = getOuterType();
			if (outerType != null) {
				return outerType.containsTypeVariable();
			}
			return false;
		}
		if (isArray()) {
			ArgType arrayElement = getArrayElement();
			if (arrayElement != null) {
				return arrayElement.containsTypeVariable();
			}
		}
		return false;
	}

	public boolean isVoid() {
		return isPrimitive() && getPrimitiveType() == PrimitiveType.VOID;
	}

	/**
	 * Recursively visit all subtypes of this type.
	 * To exit return non-null value.
	 */
	@Nullable
	public <R> R visitTypes(Function<ArgType, R> visitor) {
		R r = visitor.apply(this);
		if (r != null) {
			return r;
		}
		if (isArray()) {
			ArgType arrayElement = getArrayElement();
			if (arrayElement != null) {
				return arrayElement.visitTypes(visitor);
			}
		}
		ArgType wildcardType = getWildcardType();
		if (wildcardType != null) {
			R res = wildcardType.visitTypes(visitor);
			if (res != null) {
				return res;
			}
		}
		if (isGeneric()) {
			List<ArgType> genericTypes = getGenericTypes();
			if (genericTypes != null) {
				for (ArgType genericType : genericTypes) {
					R res = genericType.visitTypes(visitor);
					if (res != null) {
						return res;
					}
				}
			}
		}
		return null;
	}

	public static ArgType tryToResolveClassAlias(RootNode root, ArgType type) {
		if (type.isGenericType()) {
			return type;
		}
		if (type.isArray()) {
			ArgType rootType = type.getArrayRootElement();
			ArgType aliasType = tryToResolveClassAlias(root, rootType);
			if (aliasType == rootType) {
				return type;
			}
			return ArgType.array(aliasType, type.getArrayDimension());
		}
		if (type.isObject()) {
			ArgType wildcardType = type.getWildcardType();
			if (wildcardType != null) {
				return new WildcardType(tryToResolveClassAlias(root, wildcardType), type.getWildcardBound());
			}
			ClassInfo clsInfo = ClassInfo.fromName(root, type.getObject());
			ArgType baseType = clsInfo.hasAlias() ? ArgType.object(clsInfo.getAliasFullName()) : type;
			if (!type.isGeneric()) {
				return baseType;
			}
			List<ArgType> genericTypes = type.getGenericTypes();
			if (genericTypes != null) {
				return new GenericObject(baseType.getObject(), tryToResolveClassAlias(root, genericTypes));
			}
		}
		return type;
	}

	public static List<ArgType> tryToResolveClassAlias(RootNode root, List<ArgType> types) {
		return ListUtils.map(types, t -> tryToResolveClassAlias(root, t));
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
