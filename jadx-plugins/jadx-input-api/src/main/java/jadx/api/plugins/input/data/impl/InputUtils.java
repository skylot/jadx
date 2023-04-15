package jadx.api.plugins.input.data.impl;

public class InputUtils {
	public static String formatOffset(int offset) {
		return String.format("0x%04x", offset);
	}
}
