package jadx.core.dex.visitors.usage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;

import static jadx.core.utils.Utils.notEmpty;

public class UsageInfo {
	private final RootNode root;

	private final UseSet<ClassNode, ClassNode> clsDeps = new UseSet<>();
	private final UseSet<ClassNode, ClassNode> clsUsage = new UseSet<>();
	private final UseSet<ClassNode, MethodNode> clsUseInMth = new UseSet<>();
	private final UseSet<FieldNode, MethodNode> fieldUsage = new UseSet<>();
	private final UseSet<MethodNode, MethodNode> mthUsage = new UseSet<>();

	public UsageInfo(RootNode root) {
		this.root = root;
	}

	public void apply() {
		clsDeps.visit((cls, deps) -> cls.setDependencies(sortedList(deps)));
		clsUsage.visit((cls, deps) -> cls.setUseIn(sortedList(deps)));
		clsUseInMth.visit((cls, methods) -> cls.setUseInMth(sortedList(methods)));
		fieldUsage.visit((field, methods) -> field.setUseIn(sortedList(methods)));
		mthUsage.visit((mth, methods) -> mth.setUseIn(sortedList(methods)));
	}

	public void clsUse(ClassNode cls, ArgType useType) {
		processType(useType, depCls -> clsUse(cls, depCls));
	}

	public void clsUse(MethodNode mth, ArgType useType) {
		processType(useType, depCls -> clsUse(mth, depCls));
	}

	public void clsUse(MethodNode mth, ClassNode useCls) {
		ClassNode parentClass = mth.getParentClass();
		clsUse(parentClass, useCls);
		if (parentClass != useCls) {
			// exclude class usage in self methods
			clsUseInMth.add(useCls, mth);
		}
	}

	public void clsUse(ClassNode cls, ClassNode depCls) {
		ClassNode topParentClass = cls.getTopParentClass();
		clsDeps.add(topParentClass, depCls.getTopParentClass());

		clsUsage.add(depCls, cls);
		clsUsage.add(depCls, topParentClass);
	}

	/**
	 * Add method usage: {@code useMth} occurrence found in {@code mth} code
	 */
	public void methodUse(MethodNode mth, MethodNode useMth) {
		clsUse(mth, useMth.getParentClass());
		mthUsage.add(useMth, mth);
		// implicit usage
		clsUse(mth, useMth.getReturnType());
		useMth.getMethodInfo().getArgumentsTypes().forEach(argType -> clsUse(mth, argType));
	}

	public void fieldUse(MethodNode mth, FieldNode useFld) {
		clsUse(mth, useFld.getParentClass());
		fieldUsage.add(useFld, mth);
		// implicit usage
		clsUse(mth, useFld.getType());
	}

	private void processType(ArgType type, Consumer<ClassNode> consumer) {
		if (type == null) {
			return;
		}
		if (type.isArray()) {
			processType(type.getArrayRootElement(), consumer);
			return;
		}
		if (type.isObject() && !type.isGenericType()) {
			ClassNode clsNode = root.resolveClass(type);
			if (clsNode != null) {
				consumer.accept(clsNode);
			}
			List<ArgType> genericTypes = type.getGenericTypes();
			if (type.isGeneric() && notEmpty(genericTypes)) {
				for (ArgType argType : genericTypes) {
					processType(argType, consumer);
				}
			}
		}
	}

	private static <T extends Comparable<T>> List<T> sortedList(Set<T> deps) {
		List<T> list = new ArrayList<>(deps);
		Collections.sort(list);
		return list;
	}
}
