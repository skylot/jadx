package jadx.core.dex.visitors.typeinference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.ArgType.WildcardBound;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.dex.visitors.typeinference.TypeCompareEnum.CONFLICT;
import static jadx.core.dex.visitors.typeinference.TypeCompareEnum.EQUAL;
import static jadx.core.dex.visitors.typeinference.TypeCompareEnum.NARROW;
import static jadx.core.dex.visitors.typeinference.TypeCompareEnum.NARROW_BY_GENERIC;
import static jadx.core.dex.visitors.typeinference.TypeCompareEnum.UNKNOWN;
import static jadx.core.dex.visitors.typeinference.TypeCompareEnum.WIDER;
import static jadx.core.dex.visitors.typeinference.TypeCompareEnum.WIDER_BY_GENERIC;
import static jadx.core.utils.Utils.isEmpty;

public class TypeCompare {
	private static final Logger LOG = LoggerFactory.getLogger(TypeCompare.class);

	private final RootNode root;
	private final Comparator<ArgType> comparator;
	private final Comparator<ArgType> reversedComparator;

	public TypeCompare(RootNode root) {
		this.root = root;
		this.comparator = new ArgTypeComparator();
		this.reversedComparator = comparator.reversed();
	}

	public TypeCompareEnum compareTypes(ClassNode first, ClassNode second) {
		return compareObjects(first.getType(), second.getType());
	}

	public TypeCompareEnum compareTypes(ClassInfo first, ClassInfo second) {
		return compareObjects(first.getType(), second.getType());
	}

	public TypeCompareEnum compareObjects(ArgType first, ArgType second) {
		if (first == second || Objects.equals(first, second)) {
			return TypeCompareEnum.EQUAL;
		}
		return compareObjectsNoPreCheck(first, second);
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
			return compareObjectsNoPreCheck(first, second);
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
			PrimitiveType firstPrimitiveType = first.getPrimitiveType();
			PrimitiveType secondPrimitiveType = second.getPrimitiveType();
			if (firstPrimitiveType == PrimitiveType.BOOLEAN
					|| secondPrimitiveType == PrimitiveType.BOOLEAN) {
				return CONFLICT;
			}
			if (swapEquals(firstPrimitiveType, secondPrimitiveType, PrimitiveType.CHAR, PrimitiveType.BYTE)
					|| swapEquals(firstPrimitiveType, secondPrimitiveType, PrimitiveType.CHAR, PrimitiveType.SHORT)) {
				return CONFLICT;
			}
			return firstPrimitiveType.compareTo(secondPrimitiveType) > 0 ? WIDER : NARROW;
		}

		LOG.warn("Type compare function not complete, can't compare {} and {}", first, second);
		return TypeCompareEnum.CONFLICT;
	}

	private boolean swapEquals(PrimitiveType first, PrimitiveType second, PrimitiveType a, PrimitiveType b) {
		return (first == a && second == b) || (first == b && second == a);
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

	private TypeCompareEnum compareObjectsNoPreCheck(ArgType first, ArgType second) {
		boolean objectsEquals = first.getObject().equals(second.getObject());
		boolean firstGenericType = first.isGenericType();
		boolean secondGenericType = second.isGenericType();
		if (firstGenericType && secondGenericType && !objectsEquals) {
			return CONFLICT;
		}
		boolean firstGeneric = first.isGeneric();
		boolean secondGeneric = second.isGeneric();

		if (firstGenericType || secondGenericType) {
			ArgType firstWildcardType = first.getWildcardType();
			ArgType secondWildcardType = second.getWildcardType();
			if (firstWildcardType != null || secondWildcardType != null) {
				if (firstWildcardType != null && secondGenericType && first.getWildcardBound() == WildcardBound.UNBOUND) {
					return CONFLICT;
				}
				if (firstGenericType && secondWildcardType != null && second.getWildcardBound() == WildcardBound.UNBOUND) {
					return CONFLICT;
				}
			}
			if (firstGenericType) {
				return compareGenericTypeWithObject(first, second);
			} else {
				return compareGenericTypeWithObject(second, first).invert();
			}
		}
		if (objectsEquals) {
			if (firstGeneric != secondGeneric) {
				return firstGeneric ? NARROW_BY_GENERIC : WIDER_BY_GENERIC;
			}
			// both generics on same object
			if (first.getWildcardBound() != null && second.getWildcardBound() != null) {
				// both wildcards
				return compareWildcardTypes(first, second);
			}
			List<ArgType> firstGenericTypes = first.getGenericTypes();
			List<ArgType> secondGenericTypes = second.getGenericTypes();
			if (isEmpty(firstGenericTypes) || isEmpty(secondGenericTypes)) {
				// check outer types
				ArgType firstOuterType = first.getOuterType();
				ArgType secondOuterType = second.getOuterType();
				if (firstOuterType != null && secondOuterType != null) {
					return compareTypes(firstOuterType, secondOuterType);
				}
			} else {
				// compare generics arrays
				int len = firstGenericTypes.size();
				if (len == secondGenericTypes.size()) {
					for (int i = 0; i < len; i++) {
						TypeCompareEnum res = compareTypes(firstGenericTypes.get(i), secondGenericTypes.get(i));
						if (res != EQUAL) {
							return res;
						}
					}
				}
			}
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

	private TypeCompareEnum compareWildcardTypes(ArgType first, ArgType second) {
		WildcardBound firstWildcardBound = first.getWildcardBound();
		WildcardBound secondWildcardBound = second.getWildcardBound();
		if (firstWildcardBound == WildcardBound.UNBOUND) {
			return WIDER;
		}
		if (secondWildcardBound == WildcardBound.UNBOUND) {
			return NARROW;
		}
		TypeCompareEnum wildcardCompare = compareTypes(first.getWildcardType(), second.getWildcardType());
		if (firstWildcardBound == secondWildcardBound) {
			return wildcardCompare;
		}
		return CONFLICT;
	}

	private TypeCompareEnum compareGenericTypeWithObject(ArgType genericType, ArgType objType) {
		if (objType.isGenericType()) {
			return compareTypeVariables(genericType, objType);
		}
		boolean rootObject = objType.equals(ArgType.OBJECT);
		List<ArgType> extendTypes = genericType.getExtendTypes();
		if (extendTypes.isEmpty()) {
			return rootObject ? NARROW : CONFLICT;
		}
		if (extendTypes.contains(objType) || rootObject) {
			return NARROW;
		}
		for (ArgType extendType : extendTypes) {
			TypeCompareEnum res = compareObjectsNoPreCheck(extendType, objType);
			if (!res.isNarrow()) {
				return res;
			}
		}
		return NARROW;
	}

	private TypeCompareEnum compareTypeVariables(ArgType first, ArgType second) {
		if (first.getObject().equals(second.getObject())) {
			List<ArgType> firstExtendTypes = removeObject(first.getExtendTypes());
			List<ArgType> secondExtendTypes = removeObject(second.getExtendTypes());
			if (firstExtendTypes.equals(secondExtendTypes)) {
				return EQUAL;
			}
			int firstExtSize = firstExtendTypes.size();
			int secondExtSize = secondExtendTypes.size();
			if (firstExtSize == 0) {
				return WIDER;
			}
			if (secondExtSize == 0) {
				return NARROW;
			}
			if (firstExtSize == 1 && secondExtSize == 1) {
				return compareTypes(firstExtendTypes.get(0), secondExtendTypes.get(0));
			}
		}
		return CONFLICT;
	}

	private List<ArgType> removeObject(List<ArgType> extendTypes) {
		if (extendTypes.contains(ArgType.OBJECT)) {
			if (extendTypes.size() == 1) {
				return Collections.emptyList();
			}
			List<ArgType> result = new ArrayList<>(extendTypes);
			result.remove(ArgType.OBJECT);
			return result;
		}
		return extendTypes;
	}

	public Comparator<ArgType> getComparator() {
		return comparator;
	}

	public Comparator<ArgType> getReversedComparator() {
		return reversedComparator;
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
