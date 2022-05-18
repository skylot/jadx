package jadx.gui.utils.codecache.disk.adapters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import jadx.api.metadata.annotations.InsnCodeOffset;

public class InsnCodeOffsetAdapter implements DataAdapter<InsnCodeOffset> {

	public static final InsnCodeOffsetAdapter INSTANCE = new InsnCodeOffsetAdapter();

	@Override
	public void write(DataOutput out, InsnCodeOffset value) throws IOException {
		out.writeShort(value.getOffset());
	}

	@Override
	public InsnCodeOffset read(DataInput in) throws IOException {
		return new InsnCodeOffset(in.readShort());
	}
}
