package jadx.gui.cache.usage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.usage.IUsageInfoData;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;
import jadx.gui.cache.code.disk.adapters.DataAdapterHelper;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public class UsageFileAdapter extends DataAdapterHelper {
	private static final Logger LOG = LoggerFactory.getLogger(UsageFileAdapter.class);

	private static final int USAGE_DATA_VERSION = 1;
	private static final byte[] JADX_USAGE_HEADER = "jadx.usage".getBytes(StandardCharsets.US_ASCII);

	public static synchronized @Nullable RawUsageData load(Path usageFile, List<File> inputs) {
		if (!Files.isRegularFile(usageFile)) {
			return null;
		}
		long start = System.currentTimeMillis();
		try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(usageFile)))) {
			in.skipBytes(JADX_USAGE_HEADER.length);
			int dataVersion = in.readInt();
			if (dataVersion != USAGE_DATA_VERSION) {
				LOG.debug("Found old usage data format");
				FileUtils.deleteFileIfExists(usageFile);
				return null;
			}
			String inputsHash = buildInputsHash(inputs);
			String fileInputsHash = in.readUTF();
			if (!inputsHash.equals(fileInputsHash)) {
				LOG.debug("Found usage data with different inputs hash");
				FileUtils.deleteFileIfExists(usageFile);
				return null;
			}
			RawUsageData data = readData(in);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Loaded usage data from disk cache, classes count: {}, time: {}ms, file: {}",
						data.getClsMap().size(), System.currentTimeMillis() - start, usageFile);
			}
			return data;
		} catch (Exception e) {
			try {
				FileUtils.deleteFileIfExists(usageFile);
			} catch (IOException ex) {
				// ignore
			}
			LOG.error("Failed to load usage data file", e);
			return null;
		}
	}

	public static synchronized void save(IUsageInfoData data, Path usageFile, List<File> inputs) {
		long start = System.currentTimeMillis();
		FileUtils.makeDirsForFile(usageFile);
		String inputsHash = buildInputsHash(inputs);
		RawUsageData usageData = new RawUsageData();
		data.visitUsageData(new CollectUsageData(usageData));
		try (OutputStream fileOutput = Files.newOutputStream(usageFile, WRITE, CREATE, TRUNCATE_EXISTING);
				DataOutputStream out = new DataOutputStream(new BufferedOutputStream(fileOutput))) {
			out.write(JADX_USAGE_HEADER);
			out.writeInt(USAGE_DATA_VERSION);
			out.writeUTF(inputsHash);
			writeData(out, usageData);
		} catch (Exception e) {
			LOG.error("Failed to save usage data file", e);
			try {
				FileUtils.deleteFileIfExists(usageFile);
			} catch (IOException ex) {
				LOG.error("Failed to delete usage data file: {}", usageFile, ex);
			}
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("Usage data saved, time: {}ms, file: {}", System.currentTimeMillis() - start, usageFile);
		}
	}

	private static RawUsageData readData(DataInputStream in) throws IOException {
		RawUsageData data = new RawUsageData();
		int clsCount = readUVInt(in);
		int clsWithoutDataCount = readUVInt(in);

		String[] clsNames = new String[clsCount + clsWithoutDataCount];
		ClsUsageData[] classes = new ClsUsageData[clsCount];
		int c = 0;
		for (int i = 0; i < clsCount; i++) {
			String clsRawName = in.readUTF();
			classes[i] = data.getClassData(clsRawName);
			clsNames[c++] = clsRawName;
		}
		for (int i = 0; i < clsWithoutDataCount; i++) {
			clsNames[c++] = in.readUTF();
		}
		int mthCount = readUVInt(in);
		MthRef[] methods = new MthRef[mthCount];
		for (int i = 0; i < mthCount; i++) {
			int clsId = readUVInt(in);
			String mthShortId = in.readUTF();
			ClsUsageData cls = classes[clsId];
			MthRef mthRef = new MthRef(cls.getRawName(), mthShortId);
			cls.getMthUsage().put(mthShortId, new MthUsageData(mthRef));
			methods[i] = mthRef;
		}
		for (int i = 0; i < clsCount; i++) {
			ClsUsageData cls = data.getClassData(clsNames[i]);
			cls.setClsDeps(readClsList(in, clsNames));
			cls.setClsUsage(readClsList(in, clsNames));
			cls.setClsUseInMth(readMthList(in, methods));

			int mCount = readUVInt(in);
			for (int m = 0; m < mCount; m++) {
				MthRef mthRef = methods[readUVInt(in)];
				cls.getMthUsage().get(mthRef.getShortId())
						.setUsage(readMthList(in, methods));
			}
			int fCount = readUVInt(in);
			for (int f = 0; f < fCount; f++) {
				String fldShortId = in.readUTF();
				cls.getFldUsage().computeIfAbsent(fldShortId,
						fldId -> new FldUsageData(new FldRef(cls.getRawName(), fldId)))
						.setUsage(readMthList(in, methods));
			}
		}
		return data;
	}

	private static void writeData(DataOutputStream out, RawUsageData usageData) throws IOException {
		Map<String, Integer> clsMap = new HashMap<>();
		Map<MthRef, Integer> mthMap = new HashMap<>();
		Map<String, ClsUsageData> clsDataMap = usageData.getClsMap();
		List<String> classes = new ArrayList<>(clsDataMap.keySet());
		Collections.sort(classes);
		List<String> classesWithoutData = usageData.getClassesWithoutData();

		writeUVInt(out, classes.size());
		writeUVInt(out, classesWithoutData.size());
		int i = 0;
		for (String cls : classes) {
			out.writeUTF(cls);
			clsMap.put(cls, i++);
		}
		for (String cls : classesWithoutData) {
			out.writeUTF(cls);
			clsMap.put(cls, i++);
		}
		List<MthRef> methods = clsDataMap.values().stream()
				.flatMap(c -> c.getMthUsage().values().stream())
				.map(MthUsageData::getMthRef)
				.collect(Collectors.toList());
		writeUVInt(out, methods.size());
		int j = 0;
		for (MthRef mth : methods) {
			writeUVInt(out, clsMap.get(mth.getCls()));
			out.writeUTF(mth.getShortId());
			mthMap.put(mth, j++);
		}
		for (String cls : classes) {
			ClsUsageData clsData = clsDataMap.get(cls);
			writeClsList(out, clsMap, clsData.getClsDeps());
			writeClsList(out, clsMap, clsData.getClsUsage());
			writeMthList(out, mthMap, clsData.getClsUseInMth());

			writeUVInt(out, clsData.getMthUsage().size());
			for (MthUsageData mthData : clsData.getMthUsage().values()) {
				writeUVInt(out, mthMap.get(mthData.getMthRef()));
				writeMthList(out, mthMap, mthData.getUsage());
			}

			writeUVInt(out, clsData.getFldUsage().size());
			for (FldUsageData fldData : clsData.getFldUsage().values()) {
				out.writeUTF(fldData.getFldRef().getShortId());
				writeMthList(out, mthMap, fldData.getUsage());
			}
		}
	}

	private static List<String> readClsList(DataInputStream in, String[] classes) throws IOException {
		int count = readUVInt(in);
		if (count == 0) {
			return Collections.emptyList();
		}
		List<String> list = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			list.add(classes[readUVInt(in)]);
		}
		return list;
	}

	private static void writeClsList(DataOutputStream out, Map<String, Integer> clsMap, List<String> clsList) throws IOException {
		if (Utils.isEmpty(clsList)) {
			writeUVInt(out, 0);
			return;
		}
		writeUVInt(out, clsList.size());
		for (String cls : clsList) {
			Integer clsId = clsMap.get(cls);
			if (clsId == null) {
				throw new JadxRuntimeException("Unknown class in usage: " + cls);
			}
			writeUVInt(out, clsId);
		}
	}

	private static List<MthRef> readMthList(DataInputStream in, MthRef[] methods) throws IOException {
		int count = readUVInt(in);
		if (count == 0) {
			return Collections.emptyList();
		}
		List<MthRef> list = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			list.add(methods[readUVInt(in)]);
		}
		return list;
	}

	private static void writeMthList(DataOutputStream out, Map<MthRef, Integer> mthMap, List<MthRef> mthList) throws IOException {
		if (Utils.isEmpty(mthList)) {
			writeUVInt(out, 0);
			return;
		}
		writeUVInt(out, mthList.size());
		for (MthRef mth : mthList) {
			writeUVInt(out, mthMap.get(mth));
		}
	}

	private static String buildInputsHash(List<File> inputs) {
		List<Path> paths = inputs.stream()
				.filter(f -> !f.getName().endsWith(".jadx.kts"))
				.map(File::toPath)
				.collect(Collectors.toList());
		return FileUtils.buildInputsHash(paths);
	}
}
