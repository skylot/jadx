package jadx.core.utils.android;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jadx.core.utils.exceptions.JadxRuntimeException;

public class TextResMapFile {
	private static final int SPLIT_POS = 8;

	public static Map<Integer, String> read(Path resMapFile) {
		try {
			Map<Integer, String> resMap = new HashMap<>();
			for (String line : Files.readAllLines(resMapFile)) {
				int id = Integer.parseInt(line.substring(0, SPLIT_POS), 16);
				String name = line.substring(SPLIT_POS + 1);
				resMap.put(id, name);
			}
			return resMap;
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to read res-map file", e);
		}
	}

	public static void write(Path resMapFile, Map<Integer, String> inputResMap) {
		try {
			Map<Integer, String> resMap = new TreeMap<>(inputResMap);
			List<String> lines = new ArrayList<>(resMap.size());
			for (Map.Entry<Integer, String> entry : resMap.entrySet()) {
				lines.add(String.format("%08x=%s", entry.getKey(), entry.getValue()));
			}
			Files.write(resMapFile, lines);
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to write res-map file", e);
		}
	}
}
