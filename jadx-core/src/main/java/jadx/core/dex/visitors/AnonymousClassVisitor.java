package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.FieldReplaceAttr;
import jadx.core.dex.attributes.nodes.SkipMethodArgsAttr;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "AnonymousClassVisitor",
		desc = "Prepare anonymous class for inline",
		runBefore = {
				ModVisitor.class,
				CodeShrinkVisitor.class
		}
)
public class AnonymousClassVisitor extends AbstractVisitor {

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		if (cls.contains(AType.ANONYMOUS_CLASS)) {
			for (MethodNode mth : cls.getMethods()) {
				if (mth.contains(AFlag.ANONYMOUS_CONSTRUCTOR)) {
					processAnonymousConstructor(mth);
					break;
				}
			}
		}
		return true;
	}

	private static void processAnonymousConstructor(MethodNode mth) {
		ClassInfo outerCls = mth.getUseIn().get(0).getParentClass().getClassInfo();
		List<InsnNode> usedInsns = new ArrayList<>();
		Map<InsnArg, FieldNode> argsMap = getArgsToFieldsMapping(mth, usedInsns);
		if (argsMap.isEmpty()) {
			mth.add(AFlag.NO_SKIP_ARGS);
		} else {
			for (Map.Entry<InsnArg, FieldNode> entry : argsMap.entrySet()) {
				FieldNode field = entry.getValue();
				if (field == null) {
					continue;
				}
				InsnArg arg = entry.getKey();
				if (field.getType().equals(outerCls.getType())) {
					field.addAttr(new FieldReplaceAttr(outerCls));
				} else {
					field.addAttr(new FieldReplaceAttr(arg));
				}
				field.add(AFlag.DONT_GENERATE);
				if (arg.isRegister()) {
					arg.add(AFlag.SKIP_ARG);
					SkipMethodArgsAttr.skipArg(mth, (RegisterArg) arg);
				}
			}
		}
		for (InsnNode usedInsn : usedInsns) {
			usedInsn.add(AFlag.DONT_GENERATE);
		}
	}

	private static Map<InsnArg, FieldNode> getArgsToFieldsMapping(MethodNode mth, List<InsnNode> usedInsns) {
		ClassNode cls = mth.getParentClass();
		Map<InsnArg, FieldNode> map = new LinkedHashMap<>();
		for (RegisterArg arg : mth.getArgRegs()) {
			for (InsnNode useInsn : getParentInsnsSkipMove(arg)) {
				switch (useInsn.getType()) {
					case IPUT:
						FieldNode fieldNode = cls.searchField((FieldInfo) ((IndexInsnNode) useInsn).getIndex());
						if (fieldNode == null || !fieldNode.getAccessFlags().isSynthetic()) {
							// ignore normal fields
						} else {
							map.put(arg, fieldNode);
							usedInsns.add(useInsn);
						}
						break;

					case CONSTRUCTOR:
						ConstructorInsn superCtr = (ConstructorInsn) useInsn;
						if (superCtr.isSuper()) {
							usedInsns.add(useInsn);
						}
						break;
				}
			}
		}
		return map;
	}

	private static List<InsnNode> getParentInsnsSkipMove(RegisterArg arg) {
		SSAVar sVar = arg.getSVar();
		if (sVar.getUseCount() == 0) {
			return Collections.emptyList();
		}
		List<InsnNode> usedInsns = new ArrayList<>(sVar.getUseCount());
		for (RegisterArg useArg : sVar.getUseList()) {
			InsnNode parentInsn = useArg.getParentInsn();
			if (parentInsn == null) {
				continue;
			}
			if (parentInsn.getType() == InsnType.MOVE) {
				usedInsns.addAll(getParentInsnsSkipMove(parentInsn.getResult()));
			} else {
				usedInsns.add(parentInsn);
			}
		}
		return usedInsns;
	}
}
