package jadx.api;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ResourceFile.ZipRef;
import jadx.api.impl.SimpleCodeInfo;
import jadx.core.codegen.CodeWriter;
import jadx.core.utils.Utils;
import jadx.core.utils.android.Res9patchStreamDecoder;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.files.InputFile;
import jadx.core.utils.files.ZipSecurity;
import jadx.core.xmlgen.ResContainer;
import jadx.core.xmlgen.ResTableParser;

import static jadx.core.utils.files.FileUtils.READ_BUFFER_SIZE;
import static jadx.core.utils.files.FileUtils.copyStream;

// TODO: move to core package
public final class ResourcesLoader {
	private static final Logger LOG = LoggerFactory.getLogger(ResourcesLoader.class);

	private final JadxDecompiler jadxRef;

	ResourcesLoader(JadxDecompiler jadxRef) {
		this.jadxRef = jadxRef;
	}

	List<ResourceFile> load(List<InputFile> inputFiles) {
		List<ResourceFile> list = new ArrayList<>(inputFiles.size());
		for (InputFile file : inputFiles) {
			loadFile(list, file.getFile());
		}
		return list;
	}

	public interface ResourceDecoder<T> {
		T decode(long size, InputStream is) throws IOException;
	}

	public static <T> T decodeStream(ResourceFile rf, ResourceDecoder<T> decoder) throws JadxException {
		try {
			ZipRef zipRef = rf.getZipRef();
			if (zipRef == null) {
				File file = new File(rf.getName());
				try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
					return decoder.decode(file.length(), inputStream);
				}
			} else {
				try (ZipFile zipFile = new ZipFile(zipRef.getZipFile())) {
					ZipEntry entry = zipFile.getEntry(zipRef.getEntryName());
					if (entry == null) {
						throw new IOException("Zip entry not found: " + zipRef);
					}
					if (!ZipSecurity.isValidZipEntry(entry)) {
						return null;
					}
					try (InputStream inputStream = new BufferedInputStream(zipFile.getInputStream(entry))) {
						return decoder.decode(entry.getSize(), inputStream);
					}
				}
			}
		} catch (Exception e) {
			throw new JadxException("Error decode: " + rf.getName(), e);
		}
	}

	static ResContainer loadContent(JadxDecompiler jadxRef, ResourceFile rf) {
		try {
			return decodeStream(rf, (size, is) -> loadContent(jadxRef, rf, is));
		} catch (JadxException e) {
			LOG.error("Decode error", e);
			CodeWriter cw = new CodeWriter();
			cw.add("Error decode ").add(rf.getType().toString().toLowerCase());
			Utils.appendStackTrace(cw, e.getCause());
			return ResContainer.textResource(rf.getName(), cw.finish());
		}
	}

	private static ResContainer loadContent(JadxDecompiler jadxRef, ResourceFile rf,
			InputStream inputStream) throws IOException {
		switch (rf.getType()) {
			case MANIFEST:
			case XML:
				ICodeInfo content = jadxRef.getXmlParser().parse(inputStream);
				return ResContainer.textResource(rf.getName(), content);

			case ARSC:
				return new ResTableParser(jadxRef.getRoot()).decodeFiles(inputStream);

			case IMG:
				return decodeImage(rf, inputStream);

			default:
				return ResContainer.resourceFileLink(rf);
		}
	}

	private static ResContainer decodeImage(ResourceFile rf, InputStream inputStream) {
		String name = rf.getName();
		if (name.endsWith(".9.png")) {
			try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
				Res9patchStreamDecoder decoder = new Res9patchStreamDecoder();
				decoder.decode(inputStream, os);
				return ResContainer.decodedData(rf.getName(), os.toByteArray());
			} catch (Exception e) {
				LOG.error("Failed to decode 9-patch png image, path: {}", name, e);
			}
		}
		return ResContainer.resourceFileLink(rf);
	}

	private void loadFile(List<ResourceFile> list, File file) {
		if (file == null) {
			return;
		}
		try (ZipFile zip = new ZipFile(file)) {
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (ZipSecurity.isValidZipEntry(entry)) {
					addEntry(list, file, entry);
				}
			}
		} catch (Exception e) {
			LOG.debug("Not a zip file: {}", file.getAbsolutePath());
			addResourceFile(list, file);
		}
	}

	private void addResourceFile(List<ResourceFile> list, File file) {
		String name = file.getAbsolutePath();
		ResourceType type = ResourceType.getFileType(name);
		ResourceFile rf = ResourceFile.createResourceFile(jadxRef, name, type);
		if (rf != null) {
			list.add(rf);
		}
	}

	private void addEntry(List<ResourceFile> list, File zipFile, ZipEntry entry) {
		if (entry.isDirectory()) {
			return;
		}
		String name = entry.getName();
		ResourceType type = ResourceType.getFileType(name);
		ResourceFile rf = ResourceFile.createResourceFile(jadxRef, name, type);
		if (rf != null) {
			rf.setZipRef(new ZipRef(zipFile, name));
			list.add(rf);
		}
	}

	@SuppressWarnings("CharsetObjectCanBeUsed")
	public static ICodeInfo loadToCodeWriter(InputStream is) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(READ_BUFFER_SIZE);
		copyStream(is, baos);
		return new SimpleCodeInfo(baos.toString("UTF-8"));
	}
}
