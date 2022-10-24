package jadx.gui.cache.usage;

import java.util.List;

import jadx.api.usage.IUsageInfoVisitor;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.Utils;

final class CollectUsageData implements IUsageInfoVisitor {
	private final RawUsageData data;

	public CollectUsageData(RawUsageData usageData) {
		data = usageData;
	}

	@Override
	public void visitClassDeps(ClassNode cls, List<ClassNode> deps) {
		data.getClassData(cls).setClsDeps(clsNodesRef(deps));
	}

	@Override
	public void visitClassUsage(ClassNode cls, List<ClassNode> usage) {
		data.getClassData(cls).setClsUsage(clsNodesRef(usage));
	}

	@Override
	public void visitClassUseInMethods(ClassNode cls, List<MethodNode> methods) {
		data.getClassData(cls).setClsUseInMth(mthNodesRef(methods));
	}

	@Override
	public void visitFieldsUsage(FieldNode fld, List<MethodNode> methods) {
		data.getFieldData(fld).setUsage(mthNodesRef(methods));
	}

	@Override
	public void visitMethodsUsage(MethodNode mth, List<MethodNode> methods) {
		data.getMethodData(mth).setUsage(mthNodesRef(methods));
	}

	@Override
	public void visitComplete() {
		data.collectClassesWithoutData();
	}

	private List<String> clsNodesRef(List<ClassNode> usage) {
		return Utils.collectionMap(usage, ClassNode::getRawName);
	}

	private List<MthRef> mthNodesRef(List<MethodNode> methods) {
		return Utils.collectionMap(methods, m -> data.getMethodData(m).getMthRef());
	}
}
