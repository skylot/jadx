package jadx.zip;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import jadx.zip.fallback.FallbackZipParser;
import jadx.zip.parser.JadxZipParser;

public final class ZipReader {
	private static final EnumSet<ZipReaderFlags> DEFAULT_FLAGS = EnumSet.noneOf(ZipReaderFlags.class);

	public static ZipContent open(File zipFile) throws IOException {
		return open(zipFile, DEFAULT_FLAGS);
	}

	public static ZipContent open(File zipFile, Set<ZipReaderFlags> flags) throws IOException {
		return new ZipReader(zipFile.getCanonicalFile(), flags).open();
	}

	private final File zipFile;
	private final Set<ZipReaderFlags> flags;

	ZipReader(File zipFile, Set<ZipReaderFlags> flags) {
		this.zipFile = zipFile;
		this.flags = flags;
	}

	private ZipContent open() throws IOException {
		try {
			return detectParser().open();
		} catch (Exception e) {
			if (flags.contains(ZipReaderFlags.DONT_USE_FALLBACK)) {
				throw new IOException("Failed to open zip: " + zipFile, e);
			}
			// switch to fallback parser
			return buildFallbackParser().open();
		}
	}

	private IZipParser detectParser() throws IOException {
		JadxZipParser jadxParser = new JadxZipParser(zipFile, flags);
		if (zipFile.getName().endsWith(".apk")
				|| flags.contains(ZipReaderFlags.DONT_USE_FALLBACK)) {
			return jadxParser;
		}
		if (!jadxParser.canOpen()) {
			return buildFallbackParser();
		}
		// default
		if (flags.contains(ZipReaderFlags.FALLBACK_AS_DEFAULT)) {
			return buildFallbackParser();
		}
		return jadxParser;
	}

	private FallbackZipParser buildFallbackParser() {
		return new FallbackZipParser(zipFile);
	}
}
