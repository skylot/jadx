package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.FieldInitAttr;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.BaseInvokeNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxException;

public class DependencyCollector extends AbstractVisitor {

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		RootNode root = cls.root();
		Set<ClassNode> depSet = new HashSet<>();
		processClass(cls, root, depSet);
		for (ClassNode inner : cls.getInnerClasses()) {
			processClass(inner, root, depSet);
		}
		depSet.remove(cls);

		List<ClassNode> depList = new ArrayList<>(depSet);
		depList.sort(Comparator.comparing(c -> c.getClassInfo().getFullName()));
		cls.setDependencies(depList);
		return false;
	}

	private static void processClass(ClassNode cls, RootNode root, Set<ClassNode> depList) {
		addDep(root, depList, cls.getSuperClass());
		for (ArgType iType : cls.getInterfaces()) {
			addDep(root, depList, iType);
		}
		for (FieldNode fieldNode : cls.getFields()) {
			addDep(root, depList, fieldNode.getType());

			// process instructions from field init
			FieldInitAttr fieldInitAttr = fieldNode.get(AType.FIELD_INIT);
			if (fieldInitAttr != null && fieldInitAttr.getValueType() == FieldInitAttr.InitType.INSN) {
				processInsn(root, depList, fieldInitAttr.getInsn());
			}
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
		for (BlockNode block : methodNode.getBasicBlocks()) {
			for (InsnNode insnNode : block.getInstructions()) {
				processInsn(root, depList, insnNode);
			}
		}
	}

	// TODO: add custom instructions processing
	private static void processInsn(RootNode root, Set<ClassNode> depList, InsnNode insnNode) {
		RegisterArg result = insnNode.getResult();
		if (result != null) {
			addDep(root, depList, result.getType());
		}
		for (InsnArg arg : insnNode.getArguments()) {
			if (arg.isInsnWrap()) {
				processInsn(root, depList, ((InsnWrapArg) arg).getWrapInsn());
			} else {
				addDep(root, depList, arg.getType());
			}
		}
		processCustomInsn(root, depList, insnNode);
	}

	private static void processCustomInsn(RootNode root, Set<ClassNode> depList, InsnNode insn) {
		if (insn instanceof IndexInsnNode) {
			Object index = ((IndexInsnNode) insn).getIndex();
			if (index instanceof FieldInfo) {
				addDep(root, depList, ((FieldInfo) index).getDeclClass());
			} else if (index instanceof ArgType) {
				addDep(root, depList, (ArgType) index);
			}
		} else if (insn instanceof BaseInvokeNode) {
			ClassInfo declClass = ((BaseInvokeNode) insn).getCallMth().getDeclClass();
			addDep(root, depList, declClass);
		}
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
