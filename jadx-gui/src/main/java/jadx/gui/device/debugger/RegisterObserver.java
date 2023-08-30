package jadx.gui.device.debugger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.core.dex.instructions.args.ArgType;
import jadx.gui.device.debugger.SmaliDebugger.RuntimeVarInfo;
import jadx.gui.device.debugger.smali.SmaliRegister;

public class RegisterObserver {

	private Map<Long, List<Info>> infoMap;
	private final List<SmaliRegisterMapping> regList;
	private final ArtAdapter.IArtAdapter art;
	private final String mthFullID;
	private boolean hasDbgInfo = false;

	private RegisterObserver(ArtAdapter.IArtAdapter art, String mthFullID) {
		this.regList = new ArrayList<>();
		this.infoMap = Collections.emptyMap();
		this.art = art;
		this.mthFullID = mthFullID;
	}

	@NotNull
	public static RegisterObserver merge(List<RuntimeVarInfo> rtRegs, List<SmaliRegister> smaliRegs, ArtAdapter.IArtAdapter art,
			String mthFullID) {
		RegisterObserver adapter = new RegisterObserver(art, mthFullID);
		adapter.hasDbgInfo = !rtRegs.isEmpty();
		if (adapter.hasDbgInfo) {
			adapter.infoMap = new HashMap<>();
		}
		for (SmaliRegister sr : smaliRegs) {
			adapter.regList.add(new SmaliRegisterMapping(sr));
		}
		adapter.regList.sort(Comparator.comparingInt(r -> r.getSmaliRegister().getRuntimeRegNum()));
		for (RuntimeVarInfo rt : rtRegs) {
			final SmaliRegisterMapping smaliRegMapping = adapter.getRegListEntry(rt.getRegNum());
			final SmaliRegister smaliReg = smaliRegMapping.getSmaliRegister();
			smaliRegMapping.addRuntimeVarInfo(rt);

			String type = rt.getSignature();
			if (type.isEmpty()) {
				type = rt.getType();
			}
			ArgType at = ArgType.parse(type);
			if (at != null) {
				type = at.toString();
			}
			Info load = new Info(smaliReg.getRegNum(), true, rt.getName(), type);
			Info unload = new Info(smaliReg.getRegNum(), false, null, null);
			adapter.infoMap.computeIfAbsent((long) rt.getStartOffset(), k -> new ArrayList<>())
					.add(load);
			adapter.infoMap.computeIfAbsent((long) rt.getEndOffset(), k -> new ArrayList<>())
					.add(unload);
		}
		return adapter;
	}

	@NotNull
	public List<SmaliRegister> getInitializedList(long codeOffset) {
		List<SmaliRegister> ret = Collections.emptyList();
		for (SmaliRegisterMapping smaliRegisterMapping : regList) {
			if (smaliRegisterMapping.getSmaliRegister().isInitialized(codeOffset)) {
				if (ret.isEmpty()) {
					ret = new ArrayList<>();
				}
				ret.add(smaliRegisterMapping.getSmaliRegister());
			}
		}
		return ret;
	}

	@Nullable
	public RuntimeVarInfo getInfo(int runtimeNum, long codeOffset) {
		SmaliRegisterMapping list = getRegListEntry(runtimeNum);
		for (RuntimeVarInfo info : list.getRuntimeVarInfoList()) {
			if (info.getStartOffset() > codeOffset) {
				break;
			}
			if (info.isInitialized(codeOffset)) {
				return info;
			}
		}
		return null;
	}

	private SmaliRegisterMapping getRegListEntry(int regNum) {
		try {
			return regList.get(regNum);
		} catch (IndexOutOfBoundsException e) {
			throw new RuntimeException(
					String.format("Register %d does not exist (size: %d).\n %s\n Method: %s",
							regNum, regList.size(), buildDeviceInfo(), mthFullID),
					e);
		}
	}

	private String buildDeviceInfo() {
		DebugSettings debugSettings = DebugSettings.INSTANCE;
		return "Device: " + debugSettings.getDevice().getDeviceInfo()
				+ ", Android: " + debugSettings.getVer()
				+ ", ArtAdapter: " + art.getClass().getSimpleName();
	}

	@NotNull
	public List<Info> getInfoAt(long codeOffset) {
		if (hasDbgInfo) {
			List<Info> list = infoMap.get(codeOffset);
			if (list != null) {
				return list;
			}
		}
		return Collections.emptyList();
	}

	public static class SmaliRegisterMapping {
		private final SmaliRegister smaliRegister;

		private List<RuntimeVarInfo> rtList = Collections.emptyList();

		public SmaliRegisterMapping(SmaliRegister smaliRegister) {
			this.smaliRegister = smaliRegister;
		}

		public SmaliRegister getSmaliRegister() {
			return smaliRegister;
		}

		@NotNull
		public List<RuntimeVarInfo> getRuntimeVarInfoList() {
			return rtList;
		}

		public void addRuntimeVarInfo(RuntimeVarInfo rt) {
			if (rtList.isEmpty()) {
				rtList = new ArrayList<>();
			}
			rtList.add(rt);
		}
	}

	public static class Info {
		private final int smaliRegNum;
		private final boolean load;
		private final String name;
		private final String type;

		private Info(int smaliRegNum, boolean load, String name, String type) {
			this.smaliRegNum = smaliRegNum;
			this.load = load;
			this.name = name;
			this.type = type;
		}

		public int getSmaliRegNum() {
			return smaliRegNum;
		}

		public boolean isLoad() {
			return load;
		}

		public String getName() {
			return name;
		}

		public String getType() {
			return type;
		}
	}
}
