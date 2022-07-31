package jadx.core.deobf;

import java.util.HashSet;
import java.util.Set;

import jadx.api.JadxArgs;
import jadx.api.deobf.IRenameCondition;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;

public class DeobfCondition implements IRenameCondition {

	private int minLength;
	private int maxLength;

	private final Set<String> avoidClsNames = new HashSet<>();

	@Override
	public void init(RootNode root) {
		JadxArgs args = root.getArgs();
		this.minLength = args.getDeobfuscationMinLength();
		this.maxLength = args.getDeobfuscationMaxLength();

		for (PackageNode pkg : root.getPackages()) {
			avoidClsNames.add(pkg.getPkgInfo().getName());
		}
	}

	@Override
	public boolean shouldRename(PackageNode pkg) {
		String name = pkg.getAliasPkgInfo().getName();
		return shouldRename(name)
				&& !pkg.hasAlias()
				&& !TldHelper.contains(name);
	}

	@Override
	public boolean shouldRename(ClassNode cls) {
		if (cls.contains(AFlag.DONT_RENAME)
				|| cls.getClassInfo().hasAlias()
				|| isR(cls.getTopParentClass())) {
			return false;
		}
		String name = cls.getAlias();
		if (avoidClsNames.contains(name)) {
			return true;
		}
		return shouldRename(name);
	}

	@Override
	public boolean shouldRename(FieldNode fld) {
		return shouldRename(fld.getAlias())
				&& !fld.contains(AFlag.DONT_RENAME)
				&& !fld.getFieldInfo().hasAlias()
				&& !isR(fld.getTopParentClass());
	}

	@Override
	public boolean shouldRename(MethodNode mth) {
		return shouldRename(mth.getAlias())
				&& !mth.contains(AFlag.DONT_RENAME)
				&& !mth.getMethodInfo().hasAlias()
				&& !mth.isConstructor();
	}

	private boolean shouldRename(String s) {
		int len = s.length();
		return len < minLength || len > maxLength;
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
