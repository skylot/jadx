package jadx.api;

import jadx.api.ResourceFile.ZipRef;
import jadx.core.codegen.CodeWriter;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.files.InputFile;
import jadx.core.xmlgen.ResTableParser;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: move to core package
public final class ResourcesLoader {
	private static final Logger LOG = LoggerFactory.getLogger(ResourcesLoader.class);

	private static final int READ_BUFFER_SIZE = 8 * 1024;
	private static final int LOAD_SIZE_LIMIT = 10 * 1024 * 1024;

	private JadxDecompiler jadxRef;

	ResourcesLoader(JadxDecompiler jadxRef) {
		this.jadxRef = jadxRef;
	}

	List<ResourceFile> load(List<InputFile> inputFiles) {
		List<ResourceFile> list = new ArrayList<ResourceFile>(inputFiles.size());
		for (InputFile file : inputFiles) {
			loadFile(list, file.getFile());
		}
		return list;
	}

	static CodeWriter loadContent(JadxDecompiler jadxRef, ZipRef zipRef, ResourceType type) {
		if (zipRef == null) {
			return null;
		}
		ZipFile zipFile = null;
		InputStream inputStream = null;
		try {
			zipFile = new ZipFile(zipRef.getZipFile());
			ZipEntry entry = zipFile.getEntry(zipRef.getEntryName());
			if (entry != null) {
				if (entry.getSize() > LOAD_SIZE_LIMIT) {
					return new CodeWriter().add("File too big, size: "
							+ String.format("%.2f KB", entry.getSize() / 1024.));
				}
				inputStream = new BufferedInputStream(zipFile.getInputStream(entry));
				return decode(jadxRef, type, inputStream);
			} else {
				LOG.warn("Zip entry not found: {}", zipRef);
			}
		} catch (IOException e) {
			LOG.error("Error load: " + zipRef, e);
		} finally {
			try {
				if (zipFile != null) {
					zipFile.close();
				}
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (Exception e) {
				LOG.debug("Error close zip file: " + zipRef, e);
			}
		}
		return null;
	}

	private static CodeWriter decode(JadxDecompiler jadxRef, ResourceType type,
			InputStream inputStream) throws IOException {
		switch (type) {
			case MANIFEST:
			case XML:
				return jadxRef.getXmlParser().parse(inputStream);

			case ARSC:
				return new ResTableParser().decodeToCodeWriter(inputStream);
		}
		return loadToCodeWriter(inputStream);
	}

	private void loadFile(List<ResourceFile> list, File file) {
		if (file == null) {
			return;
		}
		ZipFile zip = null;
		try {
			zip = new ZipFile(file);
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				addEntry(list, file, entry);
			}
		} catch (IOException e) {
			LOG.debug("Not a zip file: " + file.getAbsolutePath());
		} finally {
			if (zip != null) {
				try {
					zip.close();
				} catch (Exception e) {
					LOG.error("Zip file close error: " + file.getAbsolutePath(), e);
				}
			}
		}
	}

	private void addEntry(List<ResourceFile> list, File zipFile, ZipEntry entry) {
		if (entry.isDirectory()) {
			return;
		}
		String name = entry.getName();
		ResourceType type = ResourceType.getFileType(name);
		ResourceFile rf = new ResourceFile(jadxRef, name, type);
		rf.setZipRef(new ZipRef(zipFile, name));
		list.add(rf);
		// LOG.debug("Add resource entry: {}, size: {}", name, entry.getSize());
	}

	private static CodeWriter loadToCodeWriter(InputStream is) throws IOException {
		CodeWriter cw = new CodeWriter();
		ByteArrayOutputStream baos = new ByteArrayOutputStream(READ_BUFFER_SIZE);
		byte[] buffer = new byte[READ_BUFFER_SIZE];
		int count;
		try {
			while ((count = is.read(buffer)) != -1) {
				baos.write(buffer, 0, count);
			}
		} finally {
			try {
				is.close();
			} catch (Exception ignore) {
			}
		}
		cw.add(baos.toString("UTF-8"));
		return cw;
	}
}
