package jadx.zip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import jadx.zip.fallback.FallbackZipParser;
import jadx.zip.parser.JadxZipParser;
import jadx.zip.security.IJadxZipSecurity;
import jadx.zip.security.JadxZipSecurity;

/**
 * Jadx wrapper to provide custom zip parser ({@link JadxZipParser})
 * with fallback to default Java implementation.
 */
public class ZipReader {
	private final ZipReaderOptions options;

	public ZipReader() {
		this(ZipReaderOptions.getDefault());
	}

	public ZipReader(Set<ZipReaderFlags> flags) {
		this(new ZipReaderOptions(new JadxZipSecurity(), flags));
	}

	public ZipReader(IJadxZipSecurity security) {
		this(new ZipReaderOptions(security, ZipReaderFlags.none()));
	}

	public ZipReader(ZipReaderOptions options) {
		this.options = options;
	}

	@SuppressWarnings("resource")
	public ZipContent open(File zipFile) throws IOException {
		try {
			JadxZipParser jadxParser = new JadxZipParser(zipFile, options);
			IZipParser detectedParser = detectParser(zipFile, jadxParser);
			if (detectedParser != jadxParser) {
				jadxParser.close();
			}
			return detectedParser.open();
		} catch (Exception e) {
			if (options.getFlags().contains(ZipReaderFlags.DONT_USE_FALLBACK)) {
				throw new IOException("Failed to open zip: " + zipFile, e);
			}
			// switch to fallback parser
			return buildFallbackParser(zipFile).open();
		}
	}

	/**
	 * Visit valid entries in a zip file.
	 * Return not null value from visitor to stop iteration.
	 */
	public <R> @Nullable R visitEntries(File file, Function<IZipEntry, R> visitor) {
		try (ZipContent content = open(file)) {
			for (IZipEntry entry : content.getEntries()) {
				R result = visitor.apply(entry);
				if (result != null) {
					return result;
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to process zip file: " + file.getAbsolutePath(), e);
		}
		return null;
	}

	public void readEntries(File file, BiConsumer<IZipEntry, InputStream> visitor) {
		visitEntries(file, entry -> {
			if (!entry.isDirectory()) {
				try (InputStream in = entry.getInputStream()) {
					visitor.accept(entry, in);
				} catch (Exception e) {
					throw new RuntimeException("Failed to process zip entry: " + entry, e);
				}
			}
			return null;
		});
	}

	public ZipReaderOptions getOptions() {
		return options;
	}

	private IZipParser detectParser(File zipFile, JadxZipParser jadxParser) {
		if (zipFile.getName().endsWith(".apk")
				|| options.getFlags().contains(ZipReaderFlags.DONT_USE_FALLBACK)) {
			return jadxParser;
		}
		if (!jadxParser.canOpen()) {
			return buildFallbackParser(zipFile);
		}
		// default
		if (options.getFlags().contains(ZipReaderFlags.FALLBACK_AS_DEFAULT)) {
			return buildFallbackParser(zipFile);
		}
		return jadxParser;
	}

	private FallbackZipParser buildFallbackParser(File zipFile) {
		return new FallbackZipParser(zipFile, options);
	}
}
