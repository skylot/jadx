package jadx.core.deobf.conditions;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;

public class ExcludeAndroidRClass extends AbstractDeobfCondition {

	@Override
	public Action check(ClassNode cls) {
		if (isR(cls.getTopParentClass())) {
			return Action.FORBID_RENAME;
		}
		return Action.NO_ACTION;
	}

	private static boolean isR(ClassNode cls) {
		if (cls.contains(AFlag.ANDROID_R_CLASS)) {
			return true;
		}
		if (!cls.getClassInfo().getShortName().equals("R")) {
			return false;
		}
		if (!cls.getMethods().isEmpty() || !cls.getFields().isEmpty()) {
			return false;
		}
		for (ClassNode inner : cls.getInnerClasses()) {
			for (MethodNode m : inner.getMethods()) {
				if (!m.getMethodInfo().isConstructor() && !m.getMethodInfo().isClassInit()) {
					return false;
				}
			}
			for (FieldNode field : cls.getFields()) {
				ArgType type = field.getType();
				if (type != ArgType.INT && (!type.isArray() || type.getArrayElement() != ArgType.INT)) {
					return false;
				}
			}
		}
		cls.add(AFlag.ANDROID_R_CLASS);
		return true;
	}
}
