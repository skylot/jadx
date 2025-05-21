package jadx.core.deobf;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import jadx.core.utils.FileSignature;
import jadx.core.utils.StringUtils;

public class FileTypeDetector {
	private static final Pattern DOCTYPE_PATTERN = Pattern.compile("\\s*<!doctype *(\\w+)[ >]", Pattern.CASE_INSENSITIVE);
	private static final List<FileSignature> FILE_SIGNATURES = new ArrayList<>();

	static {
		register("png", "89 50 4E 47");
		register("jpg", "FF D8 FF");
		register("gif", "47 49 46 38");
		register("webp", "52 49 46 46 ?? ?? ?? ?? 57 45 42 50 56 50 38");
		register("bmp", "42 4D");
		register("bmp", "42 41");
		register("bmp", "43 49");
		register("bmp", "43 50");
		register("bmp", "49 43");
		register("bmp", "50 54");
		register("mp4", "00 00 00 ?? 66 74 79 70 69 73 6F 36");
		register("mp4", "00 00 00 ?? 66 74 79 70 6D 70 34 32");
		register("m4a", "00 00 00 ?? 66 74 79 70 4D 34 41 20");
		register("mp3", "49 44 33");
		register("ogg", "4F 67 67 53");
		register("wav", "52 49 46 46 ?? ?? ?? ?? 57 41 56 45");
		register("ttf", "00 01 00 00");
		register("ttc", "74 74 63 66");
		register("otf", "4F 54 54 4F");
		register("xml", "03 00 08 00");
	}

	public static void register(String fileType, String signature) {
		FILE_SIGNATURES.add(new FileSignature(fileType, signature));
	}

	private static String detectByHeaders(byte[] data) {
		for (FileSignature sig : FILE_SIGNATURES) {
			if (FileSignature.matches(sig, data)) {
				if (sig.getFileType().equals("png") && isNinePatch(data)) {
					return ".9.png";
				}
				return "." + sig.getFileType();
			}
		}
		return null;
	}

	public static String detectFileExtension(byte[] data) {
		// detect ext by headers
		String extByHeaders = detectByHeaders(data);
		if (!StringUtils.isEmpty(extByHeaders)) {
			return extByHeaders;
		}

		// detect ext by readable text
		String text = new String(data, StandardCharsets.UTF_8);
		if (text.startsWith("-----BEGIN CERTIFICATE-----")) {
			return ".cer";
		}
		if (text.startsWith("-----BEGIN PRIVATE KEY-----")) {
			return ".key";
		}
		if (text.contains("<html>")) {
			return ".html";
		}
		Matcher m = DOCTYPE_PATTERN.matcher(text);
		if (m.lookingAt()) {
			return "." + m.group(1).toLowerCase();
		}

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new java.io.ByteArrayInputStream(data));
			String rootTag = doc.getDocumentElement().getNodeName();

			if ("svg".equalsIgnoreCase(rootTag)) {
				return ".svg";
			}
			if ("plist".equalsIgnoreCase(rootTag)) {
				return ".plist";
			}
			if ("kml".equalsIgnoreCase(rootTag)) {
				return ".kml";
			}
			return ".xml";
		} catch (Exception ignored) {
		}

		return null;
	}

	private static int readInt(byte[] data, int offset) {
		return (data[offset] & 0xFF) << 24
				| (data[offset + 1] & 0xFF) << 16
				| (data[offset + 2] & 0xFF) << 8
				| (data[offset + 3] & 0xFF);
	}

	private static boolean isNinePatch(byte[] data) {
		int offset = 8;
		while (offset + 8 < data.length) {
			int chunkLength = readInt(data, offset);
			int chunkType = readInt(data, offset + 4);
			if (chunkType == 0x6e705463) { // 'npTc'
				return true;
			}
			offset += 8 + chunkLength + 4; // chunk + data + CRC
		}
		return false;
	}
}
