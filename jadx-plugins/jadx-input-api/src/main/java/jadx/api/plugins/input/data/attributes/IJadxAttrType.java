package jadx.api.plugins.input.data.attributes;

/**
 * Marker interface for attribute type.
 * Similar to enumeration but extensible.
 * <p>
 * Used for attach attribute instance class information (T).
 * T - class of attribute instance
 * <p>
 * To create new one define static field like this:
 * {@code
 * static final IJadxAttrType<AttrTypeClass> ATTR_TYPE = IJadxAttrType.create();
 * }
 */
public interface IJadxAttrType<T extends IJadxAttribute> {

	static <A extends IJadxAttribute> IJadxAttrType<A> create() {
		return new IJadxAttrType<>() {
		};
	}
}
