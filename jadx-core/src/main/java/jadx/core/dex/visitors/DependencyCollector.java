package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jadx.api.plugins.input.data.ICodeReader;
import jadx.api.plugins.input.data.IFieldData;
import jadx.api.plugins.input.data.IMethodData;
import jadx.api.plugins.input.insns.InsnData;
import jadx.api.plugins.input.insns.Opcode;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;

@JadxVisitor(
		name = "DependencyCollector",
		desc = "Scan class and methods and collect dependant classes",
		runAfter = {
				RenameVisitor.class // sort by alias name
		}
)
// TODO: store usage info for fields, methods and inner classes
public class DependencyCollector extends AbstractVisitor {

	@Override
	public void init(RootNode root) {
		List<ClassNode> clsList = root.getClassesWithoutInner();
		for (ClassNode cls : clsList) {
			collectClassDeps(cls);
		}
		buildUsageList(clsList);
	}

	private void buildUsageList(List<ClassNode> clsList) {
		clsList.forEach(cls -> cls.setUsedIn(new ArrayList<>()));
		for (ClassNode cls : clsList) {
			for (ClassNode depCls : cls.getDependencies()) {
				depCls.getUsedIn().add(cls);
			}
		}
		for (ClassNode cls : clsList) {
			List<ClassNode> usedIn = cls.getUsedIn();
			if (usedIn.isEmpty()) {
				cls.setUsedIn(Collections.emptyList());
			} else {
				Collections.sort(usedIn);
			}
		}
	}

	public void collectClassDeps(ClassNode cls) {
		RootNode root = cls.root();
		Set<ClassNode> depSet = new HashSet<>();
		processClass(cls, root, depSet);
		for (ClassNode inner : cls.getInnerClasses()) {
			processClass(inner, root, depSet);
		}
		depSet.remove(cls);

		if (depSet.isEmpty()) {
			cls.setDependencies(Collections.emptyList());
		} else {
			List<ClassNode> depList = new ArrayList<>(depSet);
			Collections.sort(depList);
			cls.setDependencies(depList);
		}
	}

	private static void processClass(ClassNode cls, RootNode root, Set<ClassNode> depList) {
		addDep(root, depList, cls.getSuperClass());
		for (ArgType iType : cls.getInterfaces()) {
			addDep(root, depList, iType);
		}
		for (FieldNode fieldNode : cls.getFields()) {
			addDep(root, depList, fieldNode.getType());
		}
		// TODO: process annotations and generics
		for (MethodNode methodNode : cls.getMethods()) {
			if (methodNode.isNoCode() || methodNode.contains(AType.JADX_ERROR)) {
				continue;
			}
			processMethod(root, depList, methodNode);
		}
	}

	private static void processMethod(RootNode root, Set<ClassNode> depList, MethodNode methodNode) {
		addDep(root, depList, methodNode.getParentClass());
		addDep(root, depList, methodNode.getReturnType());
		for (ArgType arg : methodNode.getMethodInfo().getArgumentsTypes()) {
			addDep(root, depList, arg);
		}
		try {
			processInstructions(methodNode, depList);
		} catch (Exception e) {
			methodNode.getCodeReader().visitInstructions(insnData -> {
				insnData.decode();
				System.out.println(insnData);
			});
			methodNode.addError("Dependency scan failed", e);
		}
	}

	private static void processInstructions(MethodNode mth, Set<ClassNode> deps) {
		ICodeReader codeReader = mth.getCodeReader();
		if (codeReader == null) {
			return;
		}
		RootNode root = mth.root();
		codeReader.visitInstructions(insnData -> {
			try {
				processInsn(root, insnData, deps);
			} catch (Exception e) {
				mth.addError("Dependency scan failed at insn: " + insnData, e);
			}
		});
	}

	private static void processInsn(RootNode root, InsnData insnData, Set<ClassNode> deps) {
		if (insnData.getOpcode() == Opcode.UNKNOWN) {
			return;
		}
		switch (insnData.getIndexType()) {
			case TYPE_REF:
				insnData.decode();
				resolveType(root, deps, insnData.getIndexAsType());
				break;
			case FIELD_REF:
				insnData.decode();
				resolveField(root, deps, insnData.getIndexAsField());
				break;
			case METHOD_REF:
				insnData.decode();
				resolveMethod(root, deps, insnData.getIndexAsMethod());
				break;
		}
	}

	private static void resolveType(RootNode root, Set<ClassNode> deps, String type) {
		addDep(root, deps, ArgType.parse(type));
	}

	private static void resolveMethod(RootNode root, Set<ClassNode> deps, IMethodData method) {
		resolveType(root, deps, method.getParentClassType());
	}

	private static void resolveField(RootNode root, Set<ClassNode> deps, IFieldData field) {
		resolveType(root, deps, field.getParentClassType());
	}

	private static void addDep(RootNode root, Set<ClassNode> depList, ArgType type) {
		if (type != null) {
			if (type.isObject() && !type.isGenericType()) {
				addDep(root, depList, ClassInfo.fromType(root, type));
				ArgType[] genericTypes = type.getGenericTypes();
				if (type.isGeneric() && genericTypes != null) {
					for (ArgType argType : genericTypes) {
						addDep(root, depList, argType);
					}
				}
			} else if (type.isArray()) {
				addDep(root, depList, type.getArrayRootElement());
			}
		}
	}

	private static void addDep(RootNode root, Set<ClassNode> depList, ClassInfo clsInfo) {
		if (clsInfo != null) {
			ClassNode node = root.resolveClass(clsInfo);
			addDep(root, depList, node);
		}
	}

	private static void addDep(RootNode root, Set<ClassNode> depList, ClassNode clsNode) {
		if (clsNode != null) {
			// add only top classes
			depList.add(clsNode.getTopParentClass());
		}
	}
}
