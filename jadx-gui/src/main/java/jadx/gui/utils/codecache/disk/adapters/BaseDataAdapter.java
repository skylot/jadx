package jadx.gui.utils.codecache.disk.adapters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.jetbrains.annotations.Nullable;

public abstract class BaseDataAdapter<T> implements DataAdapter<T> {

	public void writeNullableUTF(DataOutput out, @Nullable String str) throws IOException {
		if (str == null) {
			out.writeByte(0);
		} else {
			out.writeByte(1);
			out.writeUTF(str);
		}
	}

	public @Nullable String readNullableUTF(DataInput in) throws IOException {
		if (in.readByte() == 0) {
			return null;
		}
		return in.readUTF();
	}
}
