package jadx.gui.utils.cache.code.disk.adapters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import jadx.gui.cache.code.disk.adapters.DataAdapterHelper;

import static org.assertj.core.api.Assertions.assertThat;

class DataAdapterHelperTest {

	@Test
	void uvInt() throws IOException {
		checkUVIntFor(0);
		checkUVIntFor(7);
		checkUVIntFor(0x7f);
		checkUVIntFor(0x80);
		checkUVIntFor(0x256);
		checkUVIntFor(Byte.MAX_VALUE);
		checkUVIntFor(Short.MAX_VALUE);
		checkUVIntFor(Integer.MAX_VALUE);
	}

	private void checkUVIntFor(int val) throws IOException {
		assertThat(writeReadUVInt(val)).isEqualTo(val);
	}

	private int writeReadUVInt(int val) throws IOException {
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(byteOut);
		DataAdapterHelper.writeUVInt(out, val);

		DataInput in = new DataInputStream(new ByteArrayInputStream(byteOut.toByteArray()));
		return DataAdapterHelper.readUVInt(in);
	}
}
