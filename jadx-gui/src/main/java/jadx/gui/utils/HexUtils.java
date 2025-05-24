package jadx.gui.utils;

public class HexUtils {

	public static boolean isValidHexString(String hexString) {
		String cleanS = hexString.replace(" ", "");
		int len = cleanS.length();
		try {
			boolean isPair = len % 2 == 0;
			if (isPair) {
				Long.parseLong(cleanS, 16);
				return true;
			}
		} catch (NumberFormatException ex) {
			// ignore error
			return false;
		}
		return false;
	}

	public static byte[] hexStringToByteArray(String hexString) {
		if (hexString == null || hexString.isEmpty()) {
			return new byte[0];
		}
		String cleanS = hexString.replace(" ", "");
		int len = cleanS.length();
		if (!isValidHexString(hexString)) {
			throw new IllegalArgumentException("Hex string must have even length. Input length: " + len);
		}

		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			String byteString = cleanS.substring(i, i + 2);
			try {
				int intValue = Integer.parseInt(byteString, 16);
				data[i / 2] = (byte) intValue;
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Input string contains non-hex characters at index " + i + ": " + byteString, e);
			}
		}
		return data;
	}
}
