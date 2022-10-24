package jadx.gui.cache.usage;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.usage.IUsageInfoData;
import jadx.api.usage.IUsageInfoVisitor;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

class UsageData implements IUsageInfoData {
	private static final Logger LOG = LoggerFactory.getLogger(UsageData.class);

	private final RootNode root;
	private final RawUsageData rawUsageData;

	public UsageData(RootNode root, RawUsageData rawUsageData) {
		this.root = root;
		this.rawUsageData = rawUsageData;
	}

	@Override
	public void apply() {
		for (ClsUsageData clsUsageData : rawUsageData.getClsMap().values()) {
			String clsRawName = clsUsageData.getRawName();
			ClassNode cls = root.resolveClass(clsRawName);
			if (cls == null) {
				throw new JadxRuntimeException("Failed to resolve class: " + clsRawName);
			}
			applyForClass(clsUsageData, clsRawName, cls);
		}
	}

	@Override
	public void applyForClass(ClassNode cls) {
		String clsRawName = cls.getRawName();
		ClsUsageData clsUsageData = rawUsageData.getClsMap().get(clsRawName);
		if (clsUsageData == null) {
			LOG.debug("No usage data for class: {}", clsRawName);
			return;
		}
		applyForClass(clsUsageData, clsRawName, cls);
	}

	private void applyForClass(ClsUsageData clsUsageData, String clsRawName, ClassNode cls) {
		cls.setDependencies(resolveClsList(clsUsageData.getClsDeps()));
		cls.setUseIn(resolveClsList(clsUsageData.getClsUsage()));
		cls.setUseInMth(resolveMthList(clsUsageData.getClsUseInMth()));
		for (Map.Entry<String, MthUsageData> entry : clsUsageData.getMthUsage().entrySet()) {
			MethodNode mth = cls.searchMethodByShortId(entry.getKey());
			if (mth == null) {
				throw new JadxRuntimeException("Method not found: " + clsRawName + "." + entry.getKey());
			}
			mth.setUseIn(resolveMthList(entry.getValue().getUsage()));
		}
		for (Map.Entry<String, FldUsageData> entry : clsUsageData.getFldUsage().entrySet()) {
			FieldNode fld = cls.searchFieldByShortId(entry.getKey());
			if (fld == null) {
				throw new JadxRuntimeException("Field not found: " + clsRawName + "." + entry.getKey());
			}
			fld.setUseIn(resolveMthList(entry.getValue().getUsage()));
		}
	}

	@Override
	public void visitUsageData(IUsageInfoVisitor visitor) {
		throw new JadxRuntimeException("Not implemented");
	}

	private List<ClassNode> resolveClsList(List<String> clsList) {
		return Utils.collectionMap(clsList, root::resolveClass);
	}

	private List<MethodNode> resolveMthList(List<MthRef> mthRefList) {
		return Utils.collectionMap(mthRefList, m -> root.resolveDirectMethod(m.getCls(), m.getShortId()));
	}
}
