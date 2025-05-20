package jadx.core.utils;

public class FileSignature {
	private final byte[] signatureBytes;
	private final String fileType;

	public FileSignature(String fileType, String signatureHex) {
		this.fileType = fileType;
		String[] parts = signatureHex.split(" ");
		this.signatureBytes = new byte[parts.length];
		for (int i = 0; i < parts.length; i++) {
			if (parts[i].length() != 2) {
				throw new RuntimeException(signatureHex);
			}
			if (!parts[i].equals("??")) {
				this.signatureBytes[i] = (byte) Integer.parseInt(parts[i], 16);
			}
		}
	}

	public static boolean matches(FileSignature sig, byte[] data) {
		if (data.length < sig.signatureBytes.length) {
			return false;
		}
		for (int i = 0; i < sig.signatureBytes.length; i++) {
			byte b = sig.signatureBytes[i];
			if (b != data[i]) {
				return false;
			}
		}
		return true;
	}

	public String getFileType() {
		return fileType;
	}
}
