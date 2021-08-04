package jadx.gui.device.debugger;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.reactivex.annotations.Nullable;

import jadx.core.dex.instructions.args.ArgType;
import jadx.gui.device.debugger.SmaliDebugger.RuntimeVarInfo;
import jadx.gui.device.debugger.smali.RegisterInfo;
import jadx.gui.device.debugger.smali.SmaliRegister;

public class RegisterObserver {

	private Map<Long, List<Info>> infoMap;
	private final List<Entry<SmaliRegister, List<RuntimeVarInfo>>> regList;
	private boolean hasDbgInfo = false;

	private RegisterObserver() {
		regList = new ArrayList<>();
		infoMap = Collections.emptyMap();
	}

	public static RegisterObserver merge(List<RuntimeVarInfo> rtRegs, List<SmaliRegister> smaliRegs) {
		RegisterObserver adapter = new RegisterObserver();
		adapter.hasDbgInfo = rtRegs.size() > 0;
		if (adapter.hasDbgInfo) {
			adapter.infoMap = new HashMap<>();
		}
		for (SmaliRegister sr : smaliRegs) {
			adapter.regList.add(new SimpleEntry<>(sr, Collections.emptyList()));
		}
		adapter.regList.sort(Comparator.comparingInt(r -> r.getKey().getRuntimeRegNum()));
		for (RuntimeVarInfo rt : rtRegs) {
			Entry<SmaliRegister, List<RuntimeVarInfo>> entry = adapter.regList.get(rt.getRegNum());
			if (entry.getValue().isEmpty()) {
				entry.setValue(new ArrayList<>());
			}
			entry.getValue().add(rt);

			String type = rt.getSignature();
			if (type.isEmpty()) {
				type = rt.getType();
			}
			ArgType at = ArgType.parse(type);
			if (at != null) {
				type = at.toString();
			}
			Info load = new Info(entry.getKey().getRegNum(), true,
					new SimpleEntry<>(rt.getName(), type));
			Info unload = new Info(entry.getKey().getRegNum(), false, null);
			adapter.infoMap.computeIfAbsent((long) rt.getStartOffset(), k -> new ArrayList<>())
					.add(load);
			adapter.infoMap.computeIfAbsent((long) rt.getEndOffset(), k -> new ArrayList<>())
					.add(unload);
		}
		return adapter;
	}

	public List<SmaliRegister> getInitializedList(long codeOffset) {
		List<SmaliRegister> ret = Collections.emptyList();
		for (Entry<SmaliRegister, List<RuntimeVarInfo>> info : regList) {
			if (info.getKey().isInitialized(codeOffset)) {
				if (ret.isEmpty()) {
					ret = new ArrayList<>();
				}
				ret.add(info.getKey());
			}
		}
		return ret;
	}

	@Nullable
	public Entry<String, String> getInfo(int runtimeNum, long codeOffset) {
		Entry<SmaliRegister, List<RuntimeVarInfo>> list = regList.get(runtimeNum);
		for (RegisterInfo info : list.getValue()) {
			if (info.getStartOffset() > codeOffset) {
				break;
			}
			if (info.isInitialized(codeOffset)) {
				return new SimpleEntry<>(info.getName(), info.getType());
			}
		}
		return null;
	}

	public List<Info> getInfoAt(long codeOffset) {
		if (hasDbgInfo) {
			List<Info> list = infoMap.get(codeOffset);
			if (list != null) {
				return list;
			}
		}
		return Collections.emptyList();
	}

	public static class Info {
		private final int smaliRegNum;
		private final boolean load;
		private final Entry<String, String> info;

		private Info(int smaliRegNum, boolean load, Entry<String, String> info) {
			this.smaliRegNum = smaliRegNum;
			this.load = load;
			this.info = info;
		}

		public int getSmaliRegNum() {
			return smaliRegNum;
		}

		public boolean isLoad() {
			return load;
		}

		public Entry<String, String> getInfo() {
			return info;
		}
	}
}
