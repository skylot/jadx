package jadx.core.dex.visitors;

import jadx.core.dex.attributes.AttributeFlag;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.InstructionRemover;
import jadx.core.utils.exceptions.JadxException;

import java.util.Iterator;
import java.util.List;

public class ClassModifier extends AbstractVisitor {

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		for (ClassNode inner : cls.getInnerClasses()) {
			visit(inner);
		}
		if (cls.getAccessFlags().isSynthetic()
				&& cls.getFields().isEmpty()
				&& cls.getMethods().isEmpty()) {
			cls.getAttributes().add(AttributeFlag.DONT_GENERATE);
			return false;
		}
		removeSyntheticFields(cls);
		removeSyntheticMethods(cls);
		removeEmptyMethods(cls);
		return false;
	}

	private static void removeSyntheticFields(ClassNode cls) {
		if (!cls.getClassInfo().isInner() || cls.getAccessFlags().isStatic()) {
			return;
		}
		// remove fields if it is synthetic and type is a outer class
		for (FieldNode field : cls.getFields()) {
			if (field.getAccessFlags().isSynthetic() && field.getType().isObject()) {
				ClassNode fieldsCls = cls.dex().resolveClass(ClassInfo.fromType(field.getType()));
				if (fieldsCls != null
						&& cls.getClassInfo().getParentClass().equals(fieldsCls.getClassInfo())) {
					int found = 0;
					for (MethodNode mth : cls.getMethods()) {
						if (removeFieldUsage(field, fieldsCls, mth)) {
							found++;
						}
					}
					if (found != 0) {
						// TODO: make new flag for skip field generation and usage
						field.getAttributes().add(AttributeFlag.DONT_GENERATE);
					}
				}
			}
		}
	}

	private static boolean removeFieldUsage(FieldNode field, ClassNode fieldsCls, MethodNode mth) {
		if (!mth.getAccessFlags().isConstructor()) {
			return false;
		}
		List<RegisterArg> args = mth.getArguments(false);
		if (args.isEmpty()) {
			return false;
		}
		RegisterArg arg = args.get(0);
		if (!arg.getType().equals(fieldsCls.getClassInfo().getType())) {
			return false;
		}
		BlockNode block = mth.getBasicBlocks().get(0);
		List<InsnNode> instructions = block.getInstructions();
		if (instructions.isEmpty()) {
			return false;
		}
		InsnNode insn = instructions.get(0);
		if (insn.getType() != InsnType.IPUT) {
			return false;
		}
		IndexInsnNode putInsn = (IndexInsnNode) insn;
		FieldInfo fieldInfo = (FieldInfo) putInsn.getIndex();
		if (!fieldInfo.equals(field.getFieldInfo()) || !putInsn.getArg(0).equals(arg)) {
			return false;
		}
		mth.removeFirstArgument();
		InstructionRemover.remove(block, insn);
		// other arg usage -> wrap with IGET insn
		List<InsnArg> useList = arg.getTypedVar().getUseList();
		if (useList.size() > 1) {
			InsnNode iget = new IndexInsnNode(InsnType.IGET, fieldInfo, 1);
			iget.addArg(insn.getArg(1));
			for (InsnArg insnArg : useList) {
				insnArg.wrapInstruction(iget);
			}
		}
		return true;
	}

	private static void removeSyntheticMethods(ClassNode cls) {
		for (Iterator<MethodNode> it = cls.getMethods().iterator(); it.hasNext(); ) {
			MethodNode mth = it.next();
			AccessInfo af = mth.getAccessFlags();

			// remove bridge methods
			if (af.isBridge() && af.isSynthetic() && !isMethodUniq(cls, mth)) {
				// TODO add more checks before method deletion
				it.remove();
			}

			// remove synthetic constructor for inner non-static classes
			if (af.isSynthetic() && af.isConstructor() && mth.getBasicBlocks().size() == 2) {
				List<InsnNode> insns = mth.getBasicBlocks().get(0).getInstructions();
				if (insns.size() == 1 && insns.get(0).getType() == InsnType.CONSTRUCTOR) {
					ConstructorInsn constr = (ConstructorInsn) insns.get(0);
					if (constr.isThis() && mth.getArguments(false).size() >= 1) {
						mth.removeFirstArgument();
						mth.getAttributes().add(AttributeFlag.DONT_GENERATE);
					}
				}
			}
		}
	}

	private static boolean isMethodUniq(ClassNode cls, MethodNode mth) {
		MethodInfo mi = mth.getMethodInfo();
		for (MethodNode otherMth : cls.getMethods()) {
			if (otherMth != mth) {
				MethodInfo omi = otherMth.getMethodInfo();
				if (omi.getName().equals(mi.getName())
						&& omi.getArgumentsTypes().size() == mi.getArgumentsTypes().size()) {
					// TODO: check objects types
					return false;
				}
			}
		}
		return true;
	}

	private static void removeEmptyMethods(ClassNode cls) {
		for (MethodNode mth : cls.getMethods()) {
			AccessInfo af = mth.getAccessFlags();

			// remove public empty constructors
			if (af.isConstructor()
					&& af.isPublic()
					&& mth.getArguments(false).isEmpty()) {
				List<BlockNode> bb = mth.getBasicBlocks();
				if (bb.isEmpty() || allBlocksEmpty(bb)) {
					mth.getAttributes().add(AttributeFlag.DONT_GENERATE);
				}
			}
		}
	}

	private static boolean allBlocksEmpty(List<BlockNode> blocks) {
		for (BlockNode block : blocks) {
			if (block.getInstructions().size() != 0) {
				return false;
			}
		}
		return true;
	}
}
