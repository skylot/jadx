package jadx.core.dex.visitors.typeinference;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.dex.visitors.typeinference.TypeCompareEnum.CONFLICT;
import static jadx.core.dex.visitors.typeinference.TypeCompareEnum.NARROW;
import static jadx.core.dex.visitors.typeinference.TypeCompareEnum.NARROW_BY_GENERIC;
import static jadx.core.dex.visitors.typeinference.TypeCompareEnum.UNKNOWN;
import static jadx.core.dex.visitors.typeinference.TypeCompareEnum.WIDER;
import static jadx.core.dex.visitors.typeinference.TypeCompareEnum.WIDER_BY_GENERIC;

public class TypeCompare {
	private static final Logger LOG = LoggerFactory.getLogger(TypeCompare.class);

	private final RootNode root;
	private final ArgTypeComparator comparator;

	public TypeCompare(RootNode root) {
		this.root = root;
		this.comparator = new ArgTypeComparator();
	}

	/**
	 * Compare two type and return result for first argument (narrow, wider or conflict)
	 */
	public TypeCompareEnum compareTypes(ArgType first, ArgType second) {
		if (first == second || Objects.equals(first, second)) {
			return TypeCompareEnum.EQUAL;
		}
		boolean firstKnown = first.isTypeKnown();
		boolean secondKnown = second.isTypeKnown();
		if (firstKnown != secondKnown) {
			if (firstKnown) {
				return compareWithUnknown(first, second);
			} else {
				return compareWithUnknown(second, first).invert();
			}
		}
		boolean firstArray = first.isArray();
		boolean secondArray = second.isArray();
		if (firstArray != secondArray) {
			if (firstArray) {
				return compareArrayWithOtherType(first, second);
			} else {
				return compareArrayWithOtherType(second, first).invert();
			}
		}
		if (firstArray /* && secondArray */) {
			// both arrays
			return compareTypes(first.getArrayElement(), second.getArrayElement());
		}
		if (!firstKnown /* && !secondKnown */) {
			int variantLen = Integer.compare(first.getPossibleTypes().length, second.getPossibleTypes().length);
			return variantLen > 0 ? WIDER : NARROW;
		}
		boolean firstPrimitive = first.isPrimitive();
		boolean secondPrimitive = second.isPrimitive();

		boolean firstObj = first.isObject();
		boolean secondObj = second.isObject();
		if (firstObj && secondObj) {
			return compareObjects(first, second);
		} else {
			// primitive types conflicts with objects
			if (firstObj && secondPrimitive) {
				return CONFLICT;
			}
			if (firstPrimitive && secondObj) {
				return CONFLICT;
			}
		}
		if (firstPrimitive && secondPrimitive) {
			int comparePrimitives = first.getPrimitiveType().compareTo(second.getPrimitiveType());
			return comparePrimitives > 0 ? WIDER : NARROW;
		}

		LOG.warn("Type compare function not complete, can't compare {} and {}", first, second);
		return TypeCompareEnum.CONFLICT;
	}

	private TypeCompareEnum compareArrayWithOtherType(ArgType array, ArgType other) {
		if (!other.isTypeKnown()) {
			if (other.contains(PrimitiveType.ARRAY)) {
				return NARROW;
			}
			return CONFLICT;
		}
		if (other.isObject()) {
			if (other.equals(ArgType.OBJECT)) {
				return NARROW;
			}
			return CONFLICT;
		}
		if (other.isPrimitive()) {
			return CONFLICT;
		}
		throw new JadxRuntimeException("Unprocessed type: " + other + " in array compare");
	}

	private TypeCompareEnum compareWithUnknown(ArgType known, ArgType unknown) {
		if (unknown == ArgType.UNKNOWN) {
			return NARROW;
		}
		if (unknown == ArgType.UNKNOWN_OBJECT && (known.isObject() || known.isArray())) {
			return NARROW;
		}
		if (known.equals(ArgType.OBJECT) && unknown.isArray()) {
			return WIDER;
		}
		PrimitiveType knownPrimitive;
		if (known.isPrimitive()) {
			knownPrimitive = known.getPrimitiveType();
		} else if (known.isArray()) {
			knownPrimitive = PrimitiveType.ARRAY;
		} else {
			knownPrimitive = PrimitiveType.OBJECT;
		}
		PrimitiveType[] possibleTypes = unknown.getPossibleTypes();
		for (PrimitiveType possibleType : possibleTypes) {
			if (possibleType == knownPrimitive) {
				return NARROW;
			}
		}
		return CONFLICT;
	}

	private TypeCompareEnum compareObjects(ArgType first, ArgType second) {
		boolean objectsEquals = first.getObject().equals(second.getObject());
		boolean firstGenericType = first.isGenericType();
		boolean secondGenericType = second.isGenericType();
		if (firstGenericType && secondGenericType && !objectsEquals) {
			return CONFLICT;
		}
		if (firstGenericType || secondGenericType) {
			if (firstGenericType) {
				return compareGenericTypeWithObject(first, second);
			} else {
				return compareGenericTypeWithObject(second, first).invert();
			}
		}
		boolean firstGeneric = first.isGeneric();
		boolean secondGeneric = second.isGeneric();
		if (firstGeneric != secondGeneric && objectsEquals) {
			// don't check generics for now
			return firstGeneric ? NARROW_BY_GENERIC : WIDER_BY_GENERIC;
		}
		boolean firstIsObjCls = first.equals(ArgType.OBJECT);
		if (firstIsObjCls || second.equals(ArgType.OBJECT)) {
			return firstIsObjCls ? WIDER : NARROW;
		}
		if (ArgType.isInstanceOf(root, first, second)) {
			return NARROW;
		}
		if (ArgType.isInstanceOf(root, second, first)) {
			return WIDER;
		}
		if (!ArgType.isClsKnown(root, first) || !ArgType.isClsKnown(root, second)) {
			return UNKNOWN;
		}
		return TypeCompareEnum.CONFLICT;
	}

	private TypeCompareEnum compareGenericTypeWithObject(ArgType genericType, ArgType objType) {
		List<ArgType> extendTypes = genericType.getExtendTypes();
		if (extendTypes == null || extendTypes.isEmpty()) {
			if (objType.equals(ArgType.OBJECT)) {
				return NARROW_BY_GENERIC;
			}
		} else {
			if (extendTypes.contains(objType) || objType.equals(ArgType.OBJECT)) {
				return NARROW_BY_GENERIC;
			}
			for (ArgType extendType : extendTypes) {
				if (!ArgType.isInstanceOf(root, extendType, objType)) {
					return CONFLICT;
				}
			}
			return NARROW_BY_GENERIC;
		}
		// TODO: fill extendTypes
		// return CONFLICT;
		return NARROW_BY_GENERIC;
	}

	public ArgTypeComparator getComparator() {
		return comparator;
	}

	private final class ArgTypeComparator implements Comparator<ArgType> {
		@Override
		public int compare(ArgType a, ArgType b) {
			TypeCompareEnum result = compareTypes(a, b);
			switch (result) {
				case CONFLICT:
					return -2;

				case WIDER:
				case WIDER_BY_GENERIC:
					return -1;

				case NARROW:
				case NARROW_BY_GENERIC:
					return 1;

				case EQUAL:
				default:
					return 0;
			}
		}
	}
}
