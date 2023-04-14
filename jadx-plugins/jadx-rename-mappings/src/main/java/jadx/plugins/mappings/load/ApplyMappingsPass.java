package jadx.plugins.mappings.load;

import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.FieldMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodMapping;

import jadx.api.plugins.pass.JadxPassInfo;
import jadx.api.plugins.pass.impl.OrderedJadxPassInfo;
import jadx.api.plugins.pass.types.JadxPreparePass;
import jadx.core.codegen.TypeGen;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.plugins.mappings.RenameMappingsData;

public class ApplyMappingsPass implements JadxPreparePass {

	@Override
	public JadxPassInfo getInfo() {
		return new OrderedJadxPassInfo(
				"ApplyMappings",
				"Apply mappings to classes, fields and methods")
						.after("LoadMappings")
						.before("RenameVisitor");
	}

	@Override
	public void init(RootNode root) {
		RenameMappingsData data = RenameMappingsData.getData(root);
		if (data == null) {
			return;
		}
		MappingTree mappingTree = data.getMappings();
		process(root, mappingTree);
		root.registerCodeDataUpdateListener(codeData -> process(root, mappingTree));
	}

	private void process(RootNode root, MappingTree mappingTree) {
		for (ClassNode cls : root.getClasses()) {
			String clsRawName = cls.getClassInfo().getRawName().replace('.', '/');
			ClassMapping mapping = mappingTree.getClass(clsRawName);
			if (mapping != null) {
				processClass(cls, mapping);
			}
		}
	}

	private static void processClass(ClassNode cls, ClassMapping classMapping) {
		String alias = classMapping.getDstName(0);
		if (alias != null) {
			cls.rename(alias.replace('/', '.'));
		}
		if (classMapping.getComment() != null) {
			cls.addCodeComment(classMapping.getComment());
		}
		for (FieldNode field : cls.getFields()) {
			FieldInfo fieldInfo = field.getFieldInfo();
			String signature = TypeGen.signature(fieldInfo.getType());
			FieldMapping fieldMapping = classMapping.getField(fieldInfo.getName(), signature);
			if (fieldMapping != null) {
				processField(field, fieldMapping);
			}
		}
		for (MethodNode method : cls.getMethods()) {
			MethodInfo methodInfo = method.getMethodInfo();
			String methodName = methodInfo.getName();
			String methodDesc = methodInfo.getShortId().substring(methodName.length());
			MethodMapping methodMapping = classMapping.getMethod(methodName, methodDesc);
			if (methodMapping != null) {
				processMethod(method, methodMapping);
			}
		}
	}

	private static void processField(FieldNode field, FieldMapping fieldMapping) {
		String alias = fieldMapping.getDstName(0);
		if (alias != null) {
			field.rename(alias);
		}
		String comment = fieldMapping.getComment();
		if (comment != null) {
			field.addCodeComment(comment);
		}
	}

	private static void processMethod(MethodNode method, MethodMapping methodMapping) {
		String alias = methodMapping.getDstName(0);
		if (alias != null) {
			method.rename(alias);
		}
		String comment = methodMapping.getComment();
		if (comment != null) {
			method.addCodeComment(comment);
		}
		// Method args & vars are handled in CodeMappingsPass
	}
}
