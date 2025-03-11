package jadx.api;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.impl.SimpleCodeInfo;
import jadx.api.plugins.CustomResourcesLoader;
import jadx.api.plugins.resources.IResContainerFactory;
import jadx.api.plugins.resources.IResTableParserProvider;
import jadx.api.plugins.resources.IResourcesLoader;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.Utils;
import jadx.core.utils.android.Res9patchStreamDecoder;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;
import jadx.core.xmlgen.BinaryXMLParser;
import jadx.core.xmlgen.IResTableParser;
import jadx.core.xmlgen.ResContainer;
import jadx.core.xmlgen.ResTableBinaryParserProvider;
import jadx.zip.IZipEntry;
import jadx.zip.ZipContent;

import static jadx.core.utils.files.FileUtils.READ_BUFFER_SIZE;
import static jadx.core.utils.files.FileUtils.copyStream;

// TODO: move to core package
public final class ResourcesLoader implements IResourcesLoader {
	private static final Logger LOG = LoggerFactory.getLogger(ResourcesLoader.class);

	private final JadxDecompiler decompiler;

	private final List<IResTableParserProvider> resTableParserProviders = new ArrayList<>();
	private final List<IResContainerFactory> resContainerFactories = new ArrayList<>();

	private BinaryXMLParser binaryXmlParser;

	ResourcesLoader(JadxDecompiler decompiler) {
		this.decompiler = decompiler;
		this.resTableParserProviders.add(new ResTableBinaryParserProvider());
	}

	List<ResourceFile> load(RootNode root) {
		init(root);
		List<File> inputFiles = decompiler.getArgs().getInputFiles();
		List<ResourceFile> list = new ArrayList<>(inputFiles.size());
		for (File file : inputFiles) {
			loadFile(list, file);
		}
		return list;
	}

	private void init(RootNode root) {
		for (IResTableParserProvider resTableParserProvider : resTableParserProviders) {
			try {
				resTableParserProvider.init(root);
			} catch (Exception e) {
				throw new JadxRuntimeException("Failed to init res table provider: " + resTableParserProvider);
			}
		}
		for (IResContainerFactory resContainerFactory : resContainerFactories) {
			try {
				resContainerFactory.init(root);
			} catch (Exception e) {
				throw new JadxRuntimeException("Failed to init res container factory: " + resContainerFactory);
			}
		}
	}

	public interface ResourceDecoder<T> {
		T decode(long size, InputStream is) throws IOException;
	}

	@Override
	public void addResContainerFactory(IResContainerFactory resContainerFactory) {
		resContainerFactories.add(resContainerFactory);
	}

	@Override
	public void addResTableParserProvider(IResTableParserProvider resTableParserProvider) {
		resTableParserProviders.add(resTableParserProvider);
	}

	public static <T> T decodeStream(ResourceFile rf, ResourceDecoder<T> decoder) throws JadxException {
		try {
			IZipEntry zipEntry = rf.getZipEntry();
			if (zipEntry != null) {
				try (InputStream inputStream = zipEntry.getInputStream()) {
					return decoder.decode(zipEntry.getUncompressedSize(), inputStream);
				}
			} else {
				File file = new File(rf.getOriginalName());
				try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
					return decoder.decode(file.length(), inputStream);
				}
			}
		} catch (Exception e) {
			throw new JadxException("Error decode: " + rf.getOriginalName(), e);
		}
	}

	static ResContainer loadContent(JadxDecompiler jadxRef, ResourceFile rf) {
		try {
			ResourcesLoader resLoader = jadxRef.getResourcesLoader();
			return decodeStream(rf, (size, is) -> resLoader.loadContent(rf, is));
		} catch (JadxException e) {
			LOG.error("Decode error", e);
			ICodeWriter cw = jadxRef.getRoot().makeCodeWriter();
			cw.add("Error decode ").add(rf.getType().toString().toLowerCase());
			Utils.appendStackTrace(cw, e.getCause());
			return ResContainer.textResource(rf.getDeobfName(), cw.finish());
		}
	}

	private ResContainer loadContent(ResourceFile resFile, InputStream inputStream) throws IOException {
		for (IResContainerFactory customFactory : resContainerFactories) {
			ResContainer resContainer = customFactory.create(resFile, inputStream);
			if (resContainer != null) {
				return resContainer;
			}
		}
		switch (resFile.getType()) {
			case MANIFEST:
			case XML:
				ICodeInfo content = loadBinaryXmlParser().parse(inputStream);
				return ResContainer.textResource(resFile.getDeobfName(), content);

			case ARSC:
				return decodeTable(resFile, inputStream).decodeFiles();

			case IMG:
				return decodeImage(resFile, inputStream);

			default:
				return ResContainer.resourceFileLink(resFile);
		}
	}

	public IResTableParser decodeTable(ResourceFile resFile, InputStream is) throws IOException {
		if (resFile.getType() != ResourceType.ARSC) {
			throw new IllegalArgumentException("Unexpected resource type for decode: " + resFile.getType() + ", expect '.pb'/'.arsc'");
		}
		IResTableParser parser = null;
		for (IResTableParserProvider provider : resTableParserProviders) {
			parser = provider.getParser(resFile);
			if (parser != null) {
				break;
			}
		}
		if (parser == null) {
			throw new JadxRuntimeException("Unknown type of resource file: " + resFile.getOriginalName());
		}
		parser.decode(is);
		return parser;
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

		// Try to load the resources with a custom loader first
		for (CustomResourcesLoader loader : decompiler.getCustomResourcesLoaders()) {
			if (loader.load(this, list, file)) {
				LOG.debug("Custom loader used for {}", file.getAbsolutePath());
				return;
			}
		}

		// If no custom decoder was able to decode the resources, use the default decoder
		defaultLoadFile(list, file, "");
	}

	public void defaultLoadFile(List<ResourceFile> list, File file, String subDir) {
		if (FileUtils.isZipFile(file)) {
			try {
				ZipContent zipContent = decompiler.getZipReader().open(file);
				// do not close a zip now, entry content will be read later
				decompiler.addCloseable(zipContent);
				for (IZipEntry entry : zipContent.getEntries()) {
					addEntry(list, file, entry, subDir);
				}
			} catch (Exception e) {
				throw new RuntimeException("Failed to open zip file: " + file.getAbsolutePath(), e);
			}
		} else {
			ResourceType type = ResourceType.getFileType(file.getAbsolutePath());
			list.add(ResourceFile.createResourceFile(decompiler, file, type));
		}
	}

	public void addEntry(List<ResourceFile> list, File zipFile, IZipEntry entry, String subDir) {
		if (entry.isDirectory()) {
			return;
		}
		String name = entry.getName();
		ResourceType type = ResourceType.getFileType(name);
		ResourceFile rf = ResourceFile.createResourceFile(decompiler, subDir + name, type);
		if (rf != null) {
			rf.setZipEntry(entry);
			list.add(rf);
		}
	}

	public static ICodeInfo loadToCodeWriter(InputStream is) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(READ_BUFFER_SIZE);
		copyStream(is, baos);
		return new SimpleCodeInfo(baos.toString(StandardCharsets.UTF_8));
	}

	private synchronized BinaryXMLParser loadBinaryXmlParser() {
		if (binaryXmlParser == null) {
			binaryXmlParser = new BinaryXMLParser(decompiler.getRoot());
		}
		return binaryXmlParser;
	}
}
