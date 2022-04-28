package jadx.gui.utils.codecache.disk.adapters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import jadx.api.CodePosition;

public class CodePositionAdapter implements DataAdapter<CodePosition> {

	public static final CodePositionAdapter INSTANCE = new CodePositionAdapter();

	@Override
	public void write(DataOutput out, CodePosition value) throws IOException {
		out.writeInt(value.getLine());
		out.writeInt(value.getOffset());
		out.writeInt(value.getPos());
	}

	@Override
	public CodePosition read(DataInput in) throws IOException {
		int line = in.readInt();
		int offset = in.readInt();
		int pos = in.readInt();
		return new CodePosition(line, offset, pos);
	}
}
