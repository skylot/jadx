package jadx.gui.device.debugger;

import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import jadx.core.dex.nodes.ClassNode;
import jadx.gui.device.debugger.smali.Smali;
import jadx.gui.treemodel.JClass;

public class BreakpointManager {
	private static final Logger LOG = LoggerFactory.getLogger(BreakpointManager.class);

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type TYPE_TOKEN = new TypeToken<Map<String, List<FileBreakpoint>>>() {
	}.getType();

	private static @NotNull Map<String, List<FileBreakpoint>> bpm = Collections.emptyMap();
	private static @Nullable Path savePath;
	private static DebugController debugController;
	private static Map<String, Entry<ClassNode, Listener>> listeners = Collections.emptyMap(); // class full name as key

	public static void saveAndExit() {
		sync();
		bpm = Collections.emptyMap();
		savePath = null;
		listeners = Collections.emptyMap();
	}

	public static void init(@Nullable Path baseDir) {
		Path saveDir = baseDir != null ? baseDir : Paths.get(".");
		savePath = saveDir.resolve("breakpoints.json"); // TODO: move into project file or same dir as project file
		if (Files.exists(savePath)) {
			try (Reader reader = Files.newBufferedReader(savePath, StandardCharsets.UTF_8)) {
				bpm = GSON.fromJson(reader, TYPE_TOKEN);
			} catch (Exception e) {
				LOG.error("Failed to read breakpoints config: {}", savePath, e);
			}
		}
	}

	/**
	 * @param listener When breakpoint is failed to set during debugging, this listener will be called.
	 */
	public static void addListener(JClass topCls, Listener listener) {
		if (listeners.isEmpty()) {
			listeners = new HashMap<>();
		}
		listeners.put(DbgUtils.getRawFullName(topCls),
				new SimpleEntry<>(topCls.getCls().getClassNode(), listener));
	}

	public static void removeListener(JClass topCls) {
		listeners.remove(DbgUtils.getRawFullName(topCls));
	}

	public static List<Integer> getPositions(JClass topCls) {
		List<FileBreakpoint> bps = bpm.get(DbgUtils.getRawFullName(topCls));
		if (bps != null && bps.size() > 0) {
			Smali smali = DbgUtils.getSmali(topCls.getCls().getClassNode());
			if (smali != null) {
				List<Integer> posList = new ArrayList<>(bps.size());
				for (FileBreakpoint bp : bps) {
					int pos = smali.getInsnPosByCodeOffset(bp.getFullMthRawID(), bp.codeOffset);
					if (pos > -1) {
						posList.add(pos);
					}
				}
				return posList;
			}
		}
		return Collections.emptyList();
	}

	public static boolean set(JClass topCls, int line) {
		Entry<String, Integer> lineInfo = DbgUtils.getCodeOffsetInfoByLine(topCls, line);
		if (lineInfo != null) {
			if (bpm.isEmpty()) {
				bpm = new HashMap<>();
			}
			String name = DbgUtils.getRawFullName(topCls);
			List<FileBreakpoint> list = bpm.computeIfAbsent(name, k -> new ArrayList<>());
			FileBreakpoint bkp = list.stream()
					.filter(bp -> bp.codeOffset == lineInfo.getValue() && bp.getFullMthRawID().equals(lineInfo.getKey()))
					.findFirst()
					.orElse(null);
			boolean ok = true;
			if (bkp == null) {
				String[] sigs = DbgUtils.sepClassAndMthSig(lineInfo.getKey());
				if (sigs != null && sigs.length == 2) {
					FileBreakpoint bp = new FileBreakpoint(sigs[0], sigs[1], lineInfo.getValue());
					list.add(bp);
					if (debugController != null) {
						ok = debugController.setBreakpoint(bp);
					}
				}
			}
			return ok;
		}
		return false;
	}

	public static boolean remove(JClass topCls, int line) {
		Entry<String, Integer> lineInfo = DbgUtils.getCodeOffsetInfoByLine(topCls, line);
		if (lineInfo != null) {
			List<FileBreakpoint> bps = bpm.get(DbgUtils.getRawFullName(topCls));
			for (Iterator<FileBreakpoint> it = bps.iterator(); it.hasNext();) {
				FileBreakpoint bp = it.next();
				if (bp.codeOffset == lineInfo.getValue() && bp.getFullMthRawID().equals(lineInfo.getKey())) {
					it.remove();
					if (debugController != null) {
						return debugController.removeBreakpoint(bp);
					}
					break;
				}
			}
		}
		return true;
	}

	private static void sync() {
		if (savePath == null) {
			return;
		}
		if (bpm.isEmpty() && !Files.exists(savePath)) {
			// user didn't do anything with breakpoint so don't output breakpoint file.
			return;
		}
		try {
			Files.write(savePath, GSON.toJson(bpm).getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			LOG.error("Failed to write breakpoints config: {}", savePath, e);
		}
	}

	public interface Listener {
		void breakpointDisabled(int codeOffset);
	}

	protected static class FileBreakpoint {
		final String cls;
		final String mth;
		final long codeOffset;

		private FileBreakpoint(String cls, String mth, long codeOffset) {
			this.cls = cls;
			this.mth = mth;
			this.codeOffset = codeOffset;
		}

		protected String getFullMthRawID() {
			return cls + "." + mth;
		}

		@Override
		public int hashCode() {
			return Objects.hash(codeOffset, cls, mth);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof FileBreakpoint) {
				if (obj == this) {
					return true;
				}
				FileBreakpoint fbp = (FileBreakpoint) obj;
				return fbp.codeOffset == codeOffset && fbp.cls.equals(cls) && fbp.mth.equals(mth);
			}
			return false;
		}
	}

	protected static List<FileBreakpoint> getAllBreakpoints() {
		List<FileBreakpoint> bpList = new ArrayList<>();
		for (Entry<String, List<FileBreakpoint>> entry : bpm.entrySet()) {
			bpList.addAll(entry.getValue());
		}
		return bpList;
	}

	protected static void failBreakpoint(FileBreakpoint bp) {
		Entry<ClassNode, Listener> entry = listeners.get(bp.cls);
		if (entry != null) {
			int pos = DbgUtils.getSmali(entry.getKey())
					.getInsnPosByCodeOffset(bp.getFullMthRawID(), bp.codeOffset);
			pos = Math.max(0, pos);
			entry.getValue().breakpointDisabled(pos);
		}
	}

	protected static void setDebugController(DebugController controller) {
		debugController = controller;
	}
}
