package jadx.core.dex.visitors.rename;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodArgMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodMapping;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.InitCodeVariables;
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.dex.visitors.debuginfo.DebugInfoApplyVisitor;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.mappings.DalvikToJavaBytecodeUtils;

@JadxVisitor(
		name = "ApplyCodeMappings",
		desc = "Apply mappings to method args and vars",
		runAfter = {
				InitCodeVariables.class,
				DebugInfoApplyVisitor.class
		}
)
public class CodeMappingsVisitor extends AbstractVisitor {

	private static final Logger LOG = LoggerFactory.getLogger(CodeMappingsVisitor.class);

	private Map<String, ClassMapping> clsRenamesMap;

	@Override
	public void init(RootNode root) throws JadxException {
		updateMappingsMap(root.getMappingTree());
		root.registerMappingsUpdateListener(this::updateMappingsMap);
	}

	@Override
	public boolean visit(ClassNode cls) {
		ClassMapping classMapping = getMapping(cls);
		if (classMapping != null) {
			applyRenames(cls, classMapping);
		}
		cls.getInnerClasses().forEach(this::visit);
		return false;
	}

	private static void applyRenames(ClassNode cls, ClassMapping classMapping) {
		for (MethodNode mth : cls.getMethods()) {
			String methodName = mth.getMethodInfo().getName();
			String methodDesc = mth.getMethodInfo().getShortId().substring(methodName.length());
			List<SSAVar> ssaVars = mth.getSVars();
			if (ssaVars.isEmpty()) {
				continue;
			}
			MethodMapping methodMapping = classMapping.getMethod(methodName, methodDesc);
			if (methodMapping == null) {
				continue;
			}
			// Method args
			for (MethodArgMapping argMapping : methodMapping.getArgs()) {
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

	private ClassMapping getMapping(ClassNode cls) {
		if (clsRenamesMap == null || clsRenamesMap.isEmpty()) {
			return null;
		}
		String classPath = cls.getClassInfo().makeRawFullName().replace('.', '/');
		ClassMapping clsMapping = clsRenamesMap.get(classPath);
		return clsMapping;
	}

	private void updateMappingsMap(@Nullable MemoryMappingTree mappingTree) {
		clsRenamesMap = new HashMap<>();
		if (mappingTree == null) {
			return;
		}
		for (ClassMapping cls : mappingTree.getClasses()) {
			for (MethodMapping mth : cls.getMethods()) {
				if (!mth.getArgs().isEmpty() || !mth.getVars().isEmpty()) {
					clsRenamesMap.put(cls.getSrcName(), cls);
					break;
				}
			}
		}
	}
}
