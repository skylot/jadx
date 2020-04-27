package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.dex.visitors.typeinference.TypeInferenceVisitor;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "ShadowFieldVisitor",
		desc = "Fix shadowed field access",
		runAfter = TypeInferenceVisitor.class,
		runBefore = CodeShrinkVisitor.class
)
public class ShadowFieldVisitor extends AbstractVisitor {
	private Map<String, FieldFixInfo> fixInfoMap;

	@Override
	public void init(RootNode root) {
		Map<String, FieldFixInfo> map = new HashMap<>();
		for (ClassNode cls : root.getClasses(true)) {
			Map<FieldInfo, FieldFixType> fieldFixMap = searchShadowedFields(cls);
			if (!fieldFixMap.isEmpty()) {
				FieldFixInfo fixInfo = new FieldFixInfo();
				fixInfo.fieldFixMap = fieldFixMap;
				map.put(cls.getRawName(), fixInfo);
			}
		}
		this.fixInfoMap = map;
	}

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		fixShadowFieldAccess(mth, fixInfoMap);
	}

	private static class FieldFixInfo {
		Map<FieldInfo, FieldFixType> fieldFixMap;
	}

	private enum FieldFixType {
		SUPER,
		CAST
	}

	private static Map<FieldInfo, FieldFixType> searchShadowedFields(ClassNode thisCls) {
		List<FieldNode> allFields = collectAllInstanceFields(thisCls);
		if (allFields.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, List<FieldNode>> mapByName = groupByName(allFields);
		mapByName.entrySet().removeIf(entry -> entry.getValue().size() == 1);
		if (mapByName.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<FieldInfo, FieldFixType> fixMap = new HashMap<>();
		for (List<FieldNode> fields : mapByName.values()) {
			boolean fromThisCls = fields.get(0).getParentClass() == thisCls;
			if (fromThisCls && fields.size() == 2) {
				// only one super class contains same field => can use super
				FieldNode otherField = fields.get(1);
				if (otherField.getParentClass() != thisCls) {
					fixMap.put(otherField.getFieldInfo(), FieldFixType.SUPER);
				}
			} else {
				// several super classes contains same field => can't use super, need cast to exact class
				for (FieldNode field : fields) {
					if (field.getParentClass() != thisCls) {
						fixMap.put(field.getFieldInfo(), FieldFixType.CAST);
					}
				}
			}
		}
		return fixMap;
	}

	private static Map<String, List<FieldNode>> groupByName(List<FieldNode> allFields) {
		Map<String, List<FieldNode>> groupByName = new HashMap<>(allFields.size());
		for (FieldNode field : allFields) {
			groupByName
					.computeIfAbsent(field.getName(), k -> new ArrayList<>())
					.add(field);
		}
		return groupByName;
	}

	private static List<FieldNode> collectAllInstanceFields(ClassNode cls) {
		List<FieldNode> fieldsList = new ArrayList<>();
		ClassNode currentClass = cls;
		while (currentClass != null) {
			for (FieldNode field : currentClass.getFields()) {
				if (!field.getAccessFlags().isStatic()) {
					fieldsList.add(field);
				}
			}
			ArgType superClass = currentClass.getSuperClass();
			if (superClass == null) {
				break;
			}
			currentClass = cls.root().resolveClass(superClass);
		}
		return fieldsList;
	}

	private static void fixShadowFieldAccess(MethodNode mth, Map<String, FieldFixInfo> fixInfoMap) {
		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				processInsn(mth, insn, fixInfoMap);
			}
		}
	}

	private static void processInsn(MethodNode mth, InsnNode insn, Map<String, FieldFixInfo> fixInfoMap) {
		FieldInfo fieldInfo = getFieldInfo(insn);
		if (fieldInfo == null) {
			return;
		}
		InsnArg arg = insn.getArg(insn.getArgsCount() - 1);
		ArgType type = arg.getType();
		if (!type.isTypeKnown() || !type.isObject()) {
			return;
		}
		FieldFixInfo fieldFixInfo = fixInfoMap.get(type.getObject());
		if (fieldFixInfo == null) {
			return;
		}
		FieldFixType fieldFixType = fieldFixInfo.fieldFixMap.get(fieldInfo);
		if (fieldFixType == null) {
			return;
		}
		fixFieldAccess(mth, fieldInfo, fieldFixType, arg);
	}

	@Nullable
	private static FieldInfo getFieldInfo(InsnNode insn) {
		switch (insn.getType()) {
			case IPUT:
			case IGET:
				return ((FieldInfo) ((IndexInsnNode) insn).getIndex());
			default:
				return null;
		}
	}

	private static void fixFieldAccess(MethodNode mth, FieldInfo fieldInfo, FieldFixType fieldFixType, InsnArg arg) {
		if (fieldFixType == FieldFixType.SUPER) {
			if (arg.isThis()) {
				// convert 'this' to 'super'
				arg.add(AFlag.SUPER);
				return;
			}
		}
		// apply cast
		InsnNode castInsn = new IndexInsnNode(InsnType.CAST, fieldInfo.getDeclClass().getType(), 1);
		castInsn.addArg(arg.duplicate());
		castInsn.add(AFlag.SYNTHETIC);
		castInsn.add(AFlag.EXPLICIT_CAST);
		arg.wrapInstruction(mth, castInsn, false);
	}
}
