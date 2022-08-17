package jadx.plugins.mappings.load;

import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.FieldMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodMapping;

import jadx.api.core.nodes.IRootNode;
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

public class MappingsVisitor implements JadxPreparePass {

	private final MappingTree mappingTree;

	public MappingsVisitor(MappingTree mappingTree) {
		this.mappingTree = mappingTree;
	}

	@Override
	public JadxPassInfo getInfo() {
		return new OrderedJadxPassInfo(
				"MappingVisitor",
				"Apply mappings to classes, fields and methods")
						.before("RenameVisitor");
	}

	@Override
	public void init(IRootNode iroot) {
		RootNode root = (RootNode) iroot;
		process(root);
		root.registerCodeDataUpdateListener(codeData -> process(root));
	}

	private void process(RootNode root) {
		for (ClassNode cls : root.getClasses()) {
			ClassMapping mapping = mappingTree.getClass(cls.getClassInfo().makeRawFullName().replace('.', '/'));
			if (mapping == null) {
				continue;
			}
			processClass(cls, mapping);
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
		// Method args & vars are handled in CodeMappingsVisitor
	}
}
