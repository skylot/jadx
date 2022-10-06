package jadx.gui.device.debugger;

import io.github.skylot.jdwp.JDWP;

public enum RuntimeType {
	ARRAY(91, "[]"),
	BYTE(66, "byte"),
	CHAR(67, "char"),
	OBJECT(76, "object"),
	FLOAT(70, "float"),
	DOUBLE(68, "double"),
	INT(73, "int"),
	LONG(74, "long"),
	SHORT(83, "short"),
	VOID(86, "void"),
	BOOLEAN(90, "boolean"),
	STRING(115, "string"),
	THREAD(116, "thread"),
	THREAD_GROUP(103, "thread_group"),
	CLASS_LOADER(108, "class_loader"),
	CLASS_OBJECT(99, "class_object");

	private final int jdwpTag;
	private final String desc;

	RuntimeType(int tag, String desc) {
		this.jdwpTag = tag;
		this.desc = desc;
	}

	public int getTag() {
		return jdwpTag;
	}

	public String getDesc() {
		return this.desc;
	}

	/**
	 * Converts a <code>JDWP.Tag</code> to a {@link RuntimeType}
	 *
	 * @param tag
	 * @return
	 * @throws SmaliDebuggerException
	 */
	public static RuntimeType fromJdwpTag(int tag) throws SmaliDebuggerException {
		switch (tag) {
			case JDWP.Tag.ARRAY:
				return RuntimeType.ARRAY;
			case JDWP.Tag.BYTE:
				return RuntimeType.BYTE;
			case JDWP.Tag.CHAR:
				return RuntimeType.CHAR;
			case JDWP.Tag.OBJECT:
				return RuntimeType.OBJECT;
			case JDWP.Tag.FLOAT:
				return RuntimeType.FLOAT;
			case JDWP.Tag.DOUBLE:
				return RuntimeType.DOUBLE;
			case JDWP.Tag.INT:
				return RuntimeType.INT;
			case JDWP.Tag.LONG:
				return RuntimeType.LONG;
			case JDWP.Tag.SHORT:
				return RuntimeType.SHORT;
			case JDWP.Tag.VOID:
				return RuntimeType.VOID;
			case JDWP.Tag.BOOLEAN:
				return RuntimeType.BOOLEAN;
			case JDWP.Tag.STRING:
				return RuntimeType.STRING;
			case JDWP.Tag.THREAD:
				return RuntimeType.THREAD;
			case JDWP.Tag.THREAD_GROUP:
				return RuntimeType.THREAD_GROUP;
			case JDWP.Tag.CLASS_LOADER:
				return RuntimeType.CLASS_LOADER;
			case JDWP.Tag.CLASS_OBJECT:
				return RuntimeType.CLASS_OBJECT;
			default:
				throw new SmaliDebuggerException("Unexpected value: " + tag);
		}
	}
}
