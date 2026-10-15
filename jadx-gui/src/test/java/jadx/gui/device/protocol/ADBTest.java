package jadx.gui.device.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ADBTest {

	private static boolean acceptSerial(String serial) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayInputStream in = new ByteArrayInputStream("OKAY".getBytes(ADB.ADB_CHARSET));
		return ADB.setSerial(serial, out, in);
	}

	@Test
	void testNetworkSerial() throws IOException {
		assertThat(acceptSerial("192.168.240.112:5555")).isTrue();
	}

	@Test
	void testMdnsSerial() throws IOException {
		assertThat(acceptSerial("adb-3D2NJAC2A3WFJK-abcdef._adb-tls-connect._tcp")).isTrue();
	}

	@Test
	void testUsbSerial() throws IOException {
		assertThat(acceptSerial("R58M12345AB")).isTrue();
	}

	@Test
	void testShortUsbSerial() throws IOException {
		assertThat(acceptSerial("b4f013dc")).isTrue();
	}

	@Test
	void testEmptySerialRejected() {
		assertThatThrownBy(() -> acceptSerial("")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testBlankSerialRejected() {
		assertThatThrownBy(() -> acceptSerial("bad serial")).isInstanceOf(IllegalArgumentException.class);
	}
}
