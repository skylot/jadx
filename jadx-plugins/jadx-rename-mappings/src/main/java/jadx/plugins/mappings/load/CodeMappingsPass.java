package jadx.plugins.mappings.load;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MappingTreeView.ClassMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodArgMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodMappingView;

import jadx.api.plugins.pass.JadxPassInfo;
import jadx.api.plugins.pass.impl.OrderedJadxPassInfo;
import jadx.api.plugins.pass.types.JadxDecompilePass;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.plugins.mappings.RenameMappingsData;
import jadx.plugins.mappings.utils.DalvikToJavaBytecodeUtils;

public class CodeMappingsPass implements JadxDecompilePass {
	private Map<String, ClassMappingView> clsRenamesMap;

	@Override
	public JadxPassInfo getInfo() {
		return new OrderedJadxPassInfo(
				"CodeMappings",
				"Apply mappings to method args and vars")
						.before("CodeRenameVisitor");
	}

	@Override
	public void init(RootNode root) {
		RenameMappingsData data = RenameMappingsData.getData(root);
		if (data == null) {
			return;
		}
		MappingTreeView mappingTree = data.getMappings();
		updateMappingsMap(mappingTree);
		root.registerCodeDataUpdateListener(codeData -> updateMappingsMap(mappingTree));
	}

	@Override
	public boolean visit(ClassNode cls) {
		ClassMappingView classMapping = getMapping(cls);
		if (classMapping != null) {
			applyRenames(cls, classMapping);
		}
		cls.getInnerClasses().forEach(this::visit);
		return false;
	}

	@Override
	public void visit(MethodNode mth) {
	}

	private static void applyRenames(ClassNode cls, ClassMappingView classMapping) {
		for (MethodNode mth : cls.getMethods()) {
			String methodName = mth.getMethodInfo().getName();
			String methodDesc = mth.getMethodInfo().getShortId().substring(methodName.length());
			List<SSAVar> ssaVars = mth.getSVars();
			if (ssaVars.isEmpty()) {
				continue;
			}
			MethodMappingView methodMapping = classMapping.getMethod(methodName, methodDesc);
			if (methodMapping == null) {
				continue;
			}
			// Method args
			for (MethodArgMappingView argMapping : methodMapping.getArgs()) {
				Integer mappingLvIndex = argMapping.getLvIndex();
				for (SSAVar ssaVar : ssaVars) {
					Integer actualLvIndex = DalvikToJavaBytecodeUtils.getMethodArgLvIndex(ssaVar, mth);
					if (actualLvIndex.equals(mappingLvIndex)) {
						ssaVar.getCodeVar().setName(argMapping.getDstName(0));
						break;
					}
				}
			}
			// TODO: Method vars (if ever feasible)
		}
	}

	private ClassMappingView getMapping(ClassNode cls) {
		if (clsRenamesMap == null || clsRenamesMap.isEmpty()) {
			return null;
		}
		String classPath = cls.getClassInfo().makeRawFullName().replace('.', '/');
		return clsRenamesMap.get(classPath);
	}

	private void updateMappingsMap(MappingTreeView mappings) {
		clsRenamesMap = new HashMap<>();
		for (ClassMappingView cls : mappings.getClasses()) {
			for (MethodMappingView mth : cls.getMethods()) {
				if (!mth.getArgs().isEmpty() || !mth.getVars().isEmpty()) {
					clsRenamesMap.put(cls.getSrcName(), cls);
					break;
				}
			}
		}
	}
}
