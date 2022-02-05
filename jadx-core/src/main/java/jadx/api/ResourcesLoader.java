package jadx.api;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ResourceFile.ZipRef;
import jadx.api.impl.SimpleCodeInfo;
import jadx.api.plugins.utils.ZipSecurity;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.Utils;
import jadx.core.utils.android.Res9patchStreamDecoder;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.files.FileUtils;
import jadx.core.xmlgen.ResContainer;
import jadx.core.xmlgen.ResProtoParser;
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

	List<ResourceFile> load() {
		List<File> inputFiles = jadxRef.getArgs().getInputFiles();
		List<ResourceFile> list = new ArrayList<>(inputFiles.size());
		for (File file : inputFiles) {
			loadFile(list, file);
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
				File file = new File(rf.getOriginalName());
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
					try (InputStream inputStream = ZipSecurity.getInputStreamForEntry(zipFile, entry)) {
						return decoder.decode(entry.getSize(), inputStream);
					}
				}
			}
		} catch (Exception e) {
			throw new JadxException("Error decode: " + rf.getDeobfName(), e);
		}
	}

	static ResContainer loadContent(JadxDecompiler jadxRef, ResourceFile rf) {
		try {
			return decodeStream(rf, (size, is) -> loadContent(jadxRef, rf, is));
		} catch (JadxException e) {
			LOG.error("Decode error", e);
			ICodeWriter cw = jadxRef.getRoot().makeCodeWriter();
			cw.add("Error decode ").add(rf.getType().toString().toLowerCase());
			Utils.appendStackTrace(cw, e.getCause());
			return ResContainer.textResource(rf.getDeobfName(), cw.finish());
		}
	}

	private static ResContainer loadContent(JadxDecompiler jadxRef, ResourceFile rf,
			InputStream inputStream) throws IOException {
		RootNode root = jadxRef.getRoot();
		switch (rf.getType()) {
			case MANIFEST:
			case XML: {
				ICodeInfo content;
				if (root.isProto()) {
					content = jadxRef.getProtoXmlParser().parse(inputStream);
				} else {
					content = jadxRef.getBinaryXmlParser().parse(inputStream);
				}
				return ResContainer.textResource(rf.getDeobfName(), content);
			}

			case ARSC:
				if (root.isProto()) {
					return new ResProtoParser(root).decodeFiles(inputStream);
				} else {
					return new ResTableParser(root).decodeFiles(inputStream);
				}

			case IMG:
				return decodeImage(rf, inputStream);

			default:
				return ResContainer.resourceFileLink(rf);
		}
	}

	private static ResContainer decodeImage(ResourceFile rf, InputStream inputStream) {
		String name = rf.getOriginalName();
		if (name.endsWith(".9.png")) {
			try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
				Res9patchStreamDecoder decoder = new Res9patchStreamDecoder();
				if (decoder.decode(inputStream, os)) {
					return ResContainer.decodedData(rf.getDeobfName(), os.toByteArray());
				}
			} catch (Exception e) {
				LOG.error("Failed to decode 9-patch png image, path: {}", name, e);
			}
		}
		return ResContainer.resourceFileLink(rf);
	}

	private void loadFile(List<ResourceFile> list, File file) {
		if (file == null || file.isDirectory()) {
			return;
		}
		if (FileUtils.isZipFile(file)) {
			ZipSecurity.visitZipEntries(file, (zipFile, entry) -> {
				addEntry(list, file, entry);
				return null;
			});
		} else {
			ResourceType type = ResourceType.getFileType(file.getAbsolutePath());
			list.add(ResourceFile.createResourceFile(jadxRef, file, type));
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

	public static ICodeInfo loadToCodeWriter(InputStream is) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(READ_BUFFER_SIZE);
		copyStream(is, baos);
		return new SimpleCodeInfo(baos.toString("UTF-8"));
	}
}
