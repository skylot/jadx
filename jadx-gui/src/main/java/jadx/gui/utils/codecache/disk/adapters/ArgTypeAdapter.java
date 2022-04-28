package jadx.gui.utils.codecache.disk.adapters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import jadx.core.codegen.TypeGen;
import jadx.core.dex.instructions.args.ArgType;

public class ArgTypeAdapter implements DataAdapter<ArgType> {

	public static final ArgTypeAdapter INSTANCE = new ArgTypeAdapter();

	@Override
	public void write(DataOutput out, ArgType value) throws IOException {
		if (value == null) {
			out.writeByte(0);
		} else if (!value.isTypeKnown()) {
			out.write(1);
		} else {
			out.writeByte(2);
			out.writeUTF(TypeGen.signature(value));
		}
	}

	@Override
	public ArgType read(DataInput in) throws IOException {
		switch (in.readByte()) {
			case 0:
				return null;
			case 1:
				return ArgType.UNKNOWN;
			case 2:
				return ArgType.parse(in.readUTF());
			default:
				throw new RuntimeException("Unexpected arg type tag");
		}
	}
}
