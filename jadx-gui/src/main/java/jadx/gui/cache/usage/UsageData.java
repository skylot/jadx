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
		Map<String, ClsUsageData> clsMap = rawUsageData.getClsMap();
		for (ClassNode cls : root.getClasses()) {
			String clsRawName = cls.getRawName();
			ClsUsageData clsUsageData = clsMap.get(clsRawName);
			if (clsUsageData != null) {
				applyForClass(clsUsageData, cls);
			}
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
		applyForClass(clsUsageData, cls);
	}

	private void applyForClass(ClsUsageData clsUsageData, ClassNode cls) {
		cls.setDependencies(resolveClsList(clsUsageData.getClsDeps()));
		cls.setUseIn(resolveClsList(clsUsageData.getClsUsage()));
		cls.setUseInMth(resolveMthList(clsUsageData.getClsUseInMth()));

		Map<String, MthUsageData> mthUsage = clsUsageData.getMthUsage();
		for (MethodNode mth : cls.getMethods()) {
			MthUsageData mthUsageData = mthUsage.get(mth.getMethodInfo().getShortId());
			if (mthUsageData != null) {
				mth.setUseIn(resolveMthList(mthUsageData.getUsage()));
			}
		}
		Map<String, FldUsageData> fldUsage = clsUsageData.getFldUsage();
		for (FieldNode fld : cls.getFields()) {
			FldUsageData fldUsageData = fldUsage.get(fld.getFieldInfo().getShortId());
			if (fldUsageData != null) {
				fld.setUseIn(resolveMthList(fldUsageData.getUsage()));
			}
		}
	}

	@Override
	public void visitUsageData(IUsageInfoVisitor visitor) {
		throw new JadxRuntimeException("Not implemented");
	}

	private List<ClassNode> resolveClsList(List<String> clsList) {
		return Utils.collectionMap(clsList, root::resolveRawClass);
	}

	private List<MethodNode> resolveMthList(List<MthRef> mthRefList) {
		return Utils.collectionMap(mthRefList, m -> root.resolveDirectMethod(m.getCls(), m.getShortId()));
	}
}
