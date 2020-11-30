package jadx.cli.tools;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.android.TextResMapFile;
import jadx.core.xmlgen.ResTableParser;

/**
 * Utility class for convert '.arsc' to simple text file with mapping id to resource name
 */
public class ConvertArscFile {
	private static final Logger LOG = LoggerFactory.getLogger(ConvertArscFile.class);
	private static int rewritesCount;

	public static void usage() {
		LOG.info("<res-map file> <input .arsc files>");
		LOG.info("");
		LOG.info("Note: If res-map already exists - it will be merged and updated");
	}

	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			usage();
			System.exit(1);
		}
		List<Path> inputPaths = Stream.of(args).map(Paths::get).collect(Collectors.toList());
		Path resMapFile = inputPaths.remove(0);
		Map<Integer, String> resMap;
		if (Files.isReadable(resMapFile)) {
			resMap = TextResMapFile.read(resMapFile);
		} else {
			resMap = new HashMap<>();
		}
		LOG.info("Input entries count: {}", resMap.size());

		RootNode root = new RootNode(new JadxArgs()); // not really needed
		rewritesCount = 0;
		for (Path resFile : inputPaths) {
			LOG.info("Processing {}", resFile);
			ResTableParser resTableParser = new ResTableParser(root, true);
			if (resFile.getFileName().toString().endsWith(".jar")) {
				// Load resources.arsc from android.jar
				try (ZipFile zip = new ZipFile(resFile.toFile())) {
					ZipEntry entry = zip.getEntry("resources.arsc");
					if (entry == null) {
						LOG.error("Failed to load \"resources.arsc\" from {}", resFile);
						continue;
					}
					try (InputStream inputStream = zip.getInputStream(entry)) {
						resTableParser.decode(inputStream);
					}
				}
			} else {
				// Load resources.arsc from extracted file
				try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(resFile))) {
					resTableParser.decode(inputStream);
				}
			}
			Map<Integer, String> singleResMap = resTableParser.getResStorage().getResourcesNames();
			mergeResMaps(resMap, singleResMap);
			LOG.info("{} entries count: {}, after merge: {}", resFile.getFileName(), singleResMap.size(), resMap.size());
		}
		LOG.info("Output entries count: {}", resMap.size());
		LOG.info("Total rewrites count: {}", rewritesCount);
		TextResMapFile.write(resMapFile, resMap);
		LOG.info("Result file size: {} B", resMapFile.toFile().length());
		LOG.info("done");
	}

	private static void mergeResMaps(Map<Integer, String> mainResMap, Map<Integer, String> newResMap) {
		for (Map.Entry<Integer, String> entry : newResMap.entrySet()) {
			Integer id = entry.getKey();
			String name = entry.getValue();
			String prevName = mainResMap.put(id, name);
			if (prevName != null && !name.equals(prevName)) {
				LOG.debug("Rewrite id: {} from: '{}' to: '{}'", Integer.toHexString(id), prevName, name);
				rewritesCount++;
			}
		}
	}
}
