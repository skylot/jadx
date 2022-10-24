package jadx.gui.cache.code.disk.adapters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface DataAdapter<T> {

	void write(DataOutput out, T value) throws IOException;

	T read(DataInput in) throws IOException;
}
