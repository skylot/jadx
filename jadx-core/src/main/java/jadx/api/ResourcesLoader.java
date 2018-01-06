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
import jadx.core.codegen.CodeWriter;
import jadx.core.utils.Utils;
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

	private static final int LOAD_SIZE_LIMIT = 10 * 1024 * 1024;

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

	public interface ResourceDecoder {
		ResContainer decode(long size, InputStream is) throws IOException;
	}

	public static ResContainer decodeStream(ResourceFile rf, ResourceDecoder decoder) throws JadxException {
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
			return decodeStream(rf, (size, is) -> loadContent(jadxRef, rf, is, size));
		} catch (JadxException e) {
			LOG.error("Decode error", e);
			CodeWriter cw = new CodeWriter();
			cw.add("Error decode ").add(rf.getType().toString().toLowerCase());
			cw.startLine(Utils.getStackTrace(e.getCause()));
			return ResContainer.singleFile(rf.getName(), cw);
		}
	}

	private static ResContainer loadContent(JadxDecompiler jadxRef, ResourceFile rf,
	                                        InputStream inputStream, long size) throws IOException {
		switch (rf.getType()) {
			case MANIFEST:
			case XML:
				return ResContainer.singleFile(rf.getName(),
						jadxRef.getXmlParser().parse(inputStream));

			case ARSC:
				return new ResTableParser().decodeFiles(inputStream);

			case IMG:
				return ResContainer.singleImageFile(rf.getName(), inputStream);

			default:
				if (size > LOAD_SIZE_LIMIT) {
					return ResContainer.singleFile(rf.getName(),
							new CodeWriter().add("File too big, size: " + String.format("%.2f KB", size / 1024.)));
				}
				return ResContainer.singleFile(rf.getName(), loadToCodeWriter(inputStream));
		}
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
		ResourceFile rf = ResourceFile.createResourceFileInstance(jadxRef, name, type);
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
		ResourceFile rf = ResourceFile.createResourceFileInstance(jadxRef, name, type);
		if (rf != null) {
			rf.setZipRef(new ZipRef(zipFile, name));
			list.add(rf);
		}
	}

	public static CodeWriter loadToCodeWriter(InputStream is) throws IOException {
		CodeWriter cw = new CodeWriter();
		ByteArrayOutputStream baos = new ByteArrayOutputStream(READ_BUFFER_SIZE);
		copyStream(is, baos);
		cw.add(baos.toString("UTF-8"));
		return cw;
	}
}
