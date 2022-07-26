package jadx.core.dex.visitors.rename;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.FieldMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodMapping;

import jadx.core.codegen.TypeGen;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.MethodOverrideAttr;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.JadxVisitor;

@JadxVisitor(
		name = "MappingsVisitor",
		desc = "Apply mappings to classes, fields and methods",
		runAfter = {
				RenameVisitor.class
		}
)
public class MappingsVisitor extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(MappingsVisitor.class);

	@Override
	public void init(RootNode root) {
		List<File> inputFiles = root.getArgs().getInputFiles();
		if (inputFiles.isEmpty()) {
			return;
		}

		MappingTree tree = root.getMappingTree();
		if (tree == null) {
			return;
		}

		for (ClassNode cls : root.getClasses(true)) {
			ClassMapping mapping = tree.getClass(cls.getClassInfo().makeRawFullName().replace('.', '/'));
			if (mapping == null) {
				continue;
			}
			processClass(cls, mapping);
		}
	}

	private static void processClass(ClassNode cls, ClassMapping classMapping) {
		if (classMapping.getDstName(0) != null) {
			cls.getClassInfo().changeShortName(classMapping.getDstName(0));
		}
		if (classMapping.getComment() != null) {
			cls.addInfoComment(classMapping.getComment());
		}

		// Fields
		for (FieldNode field : cls.getFields()) {
			FieldMapping fieldMapping =
					classMapping.getField(field.getFieldInfo().getName(), TypeGen.signature(field.getFieldInfo().getType()));

			if (fieldMapping == null) {
				continue;
			}
			if (fieldMapping.getDstName(0) != null) {
				field.getFieldInfo().setAlias(fieldMapping.getDstName(0));
			}
			if (fieldMapping.getComment() != null) {
				field.addInfoComment(fieldMapping.getComment());
			}
		}
		// Methods
		String methodName;
		String methodDesc;
		for (MethodNode method : cls.getMethods()) {
			methodName = method.getMethodInfo().getName();
			methodDesc = method.getMethodInfo().getShortId().substring(methodName.length());
			MethodMapping methodMapping = classMapping.getMethod(methodName, methodDesc);

			if (methodMapping == null) {
				continue;
			}
			processMethod(method, methodMapping);
		}
	}

	private static void processMethod(MethodNode method, MethodMapping methodMapping) {
		MethodOverrideAttr overrideAttr = method.get(AType.METHOD_OVERRIDE);
		if (methodMapping.getDstName(0) != null) {
			if (overrideAttr == null) {
				method.getMethodInfo().setAlias(methodMapping.getDstName(0));
			} else {
				for (MethodNode relatedMth : overrideAttr.getRelatedMthNodes()) {
					method.getMethodInfo().setAlias(methodMapping.getDstName(0));
				}
			}
		}
		if (methodMapping.getComment() != null) {
			method.addInfoComment(methodMapping.getComment());
		}
		// Method args & vars are handled in CodeMappingsVisitor
	}
}
