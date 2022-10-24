package jadx.gui.cache.code.disk.adapters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class ArgTypeAdapter implements DataAdapter<ArgType> {

	public static final ArgTypeAdapter INSTANCE = new ArgTypeAdapter();

	private enum Types {
		NULL,
		UNKNOWN,
		PRIMITIVE,
		ARRAY,
		OBJECT,
		WILDCARD,
		GENERIC,
		TYPE_VARIABLE,
		OUTER_GENERIC
	}

	@Override
	public void write(DataOutput out, ArgType value) throws IOException {
		if (value == null) {
			writeType(out, Types.NULL);
			return;
		}
		if (!value.isTypeKnown()) {
			writeType(out, Types.UNKNOWN);
			return;
		}
		if (value.isPrimitive()) {
			writeType(out, Types.PRIMITIVE);
			out.writeByte(value.getPrimitiveType().getShortName().charAt(0));
			return;
		}
		if (value.getOuterType() != null) {
			writeType(out, Types.OUTER_GENERIC);
			write(out, value.getOuterType());
			write(out, value.getInnerType());
			return;
		}
		if (value.getWildcardType() != null) {
			writeType(out, Types.WILDCARD);
			ArgType.WildcardBound bound = value.getWildcardBound();
			out.writeByte(bound.getNum());
			if (bound != ArgType.WildcardBound.UNBOUND) {
				write(out, value.getWildcardType());
			}
			return;
		}
		if (value.isGeneric()) {
			writeType(out, Types.GENERIC);
			out.writeUTF(value.getObject());
			writeTypesList(out, value.getGenericTypes());
			return;
		}
		if (value.isGenericType()) {
			writeType(out, Types.TYPE_VARIABLE);
			out.writeUTF(value.getObject());
			writeTypesList(out, value.getExtendTypes());
			return;
		}
		if (value.isObject()) {
			writeType(out, Types.OBJECT);
			out.writeUTF(value.getObject());
			return;
		}
		if (value.isArray()) {
			writeType(out, Types.ARRAY);
			out.writeByte(value.getArrayDimension());
			write(out, value.getArrayRootElement());
			return;
		}
		throw new JadxRuntimeException("Cannot save type: " + value + ", cls: " + value.getClass());
	}

	private void writeType(DataOutput out, Types type) throws IOException {
		out.writeByte(type.ordinal());
	}

	@Override
	public ArgType read(DataInput in) throws IOException {
		byte typeOrdinal = in.readByte();
		Types type = Types.values()[typeOrdinal];
		switch (type) {
			case NULL:
				return null;

			case UNKNOWN:
				return ArgType.UNKNOWN;

			case PRIMITIVE:
				char shortName = (char) in.readByte();
				return ArgType.parse(shortName);

			case OUTER_GENERIC:
				ArgType outerType = read(in);
				ArgType innerType = read(in);
				return ArgType.outerGeneric(outerType, innerType);

			case WILDCARD:
				ArgType.WildcardBound bound = ArgType.WildcardBound.getByNum(in.readByte());
				if (bound == ArgType.WildcardBound.UNBOUND) {
					return ArgType.WILDCARD;
				}
				ArgType objType = read(in);
				return ArgType.wildcard(objType, bound);

			case GENERIC:
				String clsType = in.readUTF();
				return ArgType.generic(clsType, readTypesList(in));

			case TYPE_VARIABLE:
				String typeVar = in.readUTF();
				List<ArgType> extendTypes = readTypesList(in);
				return ArgType.genericType(typeVar, extendTypes);

			case OBJECT:
				return ArgType.object(in.readUTF());

			case ARRAY:
				int dim = in.readByte();
				ArgType rootType = read(in);
				return ArgType.array(rootType, dim);

			default:
				throw new RuntimeException("Unexpected arg type: " + type);
		}
	}

	private void writeTypesList(DataOutput out, List<ArgType> types) throws IOException {
		out.writeByte(types.size());
		for (ArgType type : types) {
			write(out, type);
		}
	}

	private List<ArgType> readTypesList(DataInput in) throws IOException {
		byte size = in.readByte();
		if (size == 0) {
			return Collections.emptyList();
		}
		List<ArgType> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			list.add(read(in));
		}
		return list;
	}
}
