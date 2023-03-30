package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.AccessFlags;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.AnonymousClassAttr;
import jadx.core.dex.attributes.nodes.AnonymousClassAttr.InlineType;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.usage.UsageInfoVisitor;
import jadx.core.utils.ListUtils;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "ProcessAnonymous",
		desc = "Mark anonymous and lambda classes (for future inline)",
		runAfter = {
				UsageInfoVisitor.class
		}
)
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class ProcessAnonymous extends AbstractVisitor {

	private boolean inlineAnonymousClasses;

	@Override
	public void init(RootNode root) {
		inlineAnonymousClasses = root.getArgs().isInlineAnonymousClasses();
		if (!inlineAnonymousClasses) {
			return;
		}
		for (ClassNode cls : root.getClasses()) {
			markAnonymousClass(cls);
		}
		mergeAnonymousDeps(root);
	}

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		if (inlineAnonymousClasses && cls.contains(AFlag.CLASS_UNLOADED)) {
			// enter only on class reload
			visitClassAndInners(cls);
		}
		return false;
	}

	private void visitClassAndInners(ClassNode cls) {
		markAnonymousClass(cls);
		cls.getInnerClasses().forEach(this::visitClassAndInners);
	}

	private static void markAnonymousClass(ClassNode cls) {
		if (!canBeAnonymous(cls)) {
			return;
		}
		MethodNode anonymousConstructor = ListUtils.filterOnlyOne(cls.getMethods(), MethodNode::isConstructor);
		if (anonymousConstructor == null) {
			return;
		}
		InlineType inlineType = checkUsage(cls, anonymousConstructor);
		if (inlineType == null) {
			return;
		}
		ArgType baseType = getBaseType(cls);
		if (baseType == null) {
			return;
		}
		ClassNode outerCls;
		if (inlineType == InlineType.INSTANCE_FIELD) {
			outerCls = cls.getUseInMth().get(0).getParentClass();
		} else {
			outerCls = anonymousConstructor.getUseIn().get(0).getParentClass();
		}
		outerCls.addInlinedClass(cls);
		cls.addAttr(new AnonymousClassAttr(outerCls, baseType, inlineType));
		cls.add(AFlag.DONT_GENERATE);
		anonymousConstructor.add(AFlag.ANONYMOUS_CONSTRUCTOR);

		// force anonymous class to be processed before outer class,
		// actual usage of outer class will be removed at anonymous class process,
		// see ModVisitor.processAnonymousConstructor method
		ClassNode topOuterCls = outerCls.getTopParentClass();
		cls.removeDependency(topOuterCls);
		ListUtils.safeRemove(outerCls.getUseIn(), cls);

		// move dependency to codegen stage
		if (cls.isTopClass()) {
			topOuterCls.removeDependency(cls);
			topOuterCls.addCodegenDep(cls);
		}
	}

	private static void undoAnonymousMark(ClassNode cls) {
		AnonymousClassAttr attr = cls.get(AType.ANONYMOUS_CLASS);
		ClassNode outerCls = attr.getOuterCls();
		cls.setDependencies(ListUtils.safeAdd(cls.getDependencies(), outerCls.getTopParentClass()));
		outerCls.setUseIn(ListUtils.safeAdd(outerCls.getUseIn(), cls));

		cls.remove(AType.ANONYMOUS_CLASS);
		cls.remove(AFlag.DONT_GENERATE);
		for (MethodNode mth : cls.getMethods()) {
			if (mth.isConstructor()) {
				mth.remove(AFlag.ANONYMOUS_CONSTRUCTOR);
			}
		}
		cls.addDebugComment("Anonymous mark cleared");
	}

	private void mergeAnonymousDeps(RootNode root) {
		// Collect edges to build bidirectional tree:
		// inline edge: anonymous -> outer (one-to-one)
		// use edges: outer -> *anonymous (one-to-many)
		Map<ClassNode, ClassNode> inlineMap = new HashMap<>();
		Map<ClassNode, List<ClassNode>> useMap = new HashMap<>();
		for (ClassNode anonymousCls : root.getClasses()) {
			AnonymousClassAttr attr = anonymousCls.get(AType.ANONYMOUS_CLASS);
			if (attr != null) {
				ClassNode outerCls = attr.getOuterCls();
				List<ClassNode> list = useMap.get(outerCls);
				if (list == null || list.isEmpty()) {
					list = new ArrayList<>(2);
					useMap.put(outerCls, list);
				}
				list.add(anonymousCls);
				useMap.putIfAbsent(anonymousCls, Collections.emptyList()); // put leaf explicitly
				inlineMap.put(anonymousCls, outerCls);
			}
		}
		if (inlineMap.isEmpty()) {
			return;
		}
		// starting from leaf process deps in nodes up to root
		Set<ClassNode> added = new HashSet<>();
		useMap.forEach((key, list) -> {
			if (list.isEmpty()) {
				added.clear();
				updateDeps(key, inlineMap, added);
			}
		});
		for (ClassNode cls : root.getClasses()) {
			List<ClassNode> deps = cls.getCodegenDeps();
			if (deps.size() > 1) {
				// distinct sorted dep, reusing collections to reduce memory allocations :)
				added.clear();
				added.addAll(deps);
				deps.clear();
				deps.addAll(added);
				Collections.sort(deps);
			}
		}
	}

	private void updateDeps(ClassNode leafCls, Map<ClassNode, ClassNode> inlineMap, Set<ClassNode> added) {
		ClassNode topNode;
		ClassNode current = leafCls;
		while (true) {
			if (!added.add(current)) {
				current.addWarnComment("Loop in anonymous inline: " + current + ", path: " + added);
				added.forEach(ProcessAnonymous::undoAnonymousMark);
				return;
			}
			ClassNode next = inlineMap.get(current);
			if (next == null) {
				topNode = current.getTopParentClass();
				break;
			}
			current = next;
		}
		if (added.size() <= 2) {
			// first level deps already processed
			return;
		}
		List<ClassNode> deps = topNode.getCodegenDeps();
		if (deps.isEmpty()) {
			deps = new ArrayList<>(added.size());
			topNode.setCodegenDeps(deps);
		}
		for (ClassNode add : added) {
			deps.add(add.getTopParentClass());
		}
	}

	private static boolean canBeAnonymous(ClassNode cls) {
		if (cls.getAccessFlags().isSynthetic()) {
			return true;
		}
		String shortName = cls.getClassInfo().getShortName();
		if (shortName.contains("$") || Character.isDigit(shortName.charAt(0))) {
			return true;
		}
		if (cls.getUseIn().size() == 1 && cls.getUseInMth().size() == 1) {
			MethodNode useMth = cls.getUseInMth().get(0);
			// allow use in enum class init
			return useMth.getMethodInfo().isClassInit() && useMth.getParentClass().isEnum();
		}
		return false;
	}

	/**
	 * Checks:
	 * - class have only one constructor which used only once (allow common code for field init)
	 * - methods or fields not used outside (allow only nested inner classes with synthetic usage)
	 * - if constructor used only in class init check if possible inline by instance field
	 *
	 * @return decided inline type
	 */
	private static InlineType checkUsage(ClassNode cls, MethodNode ctr) {
		if (ctr.getUseIn().size() != 1) {
			// check if used in common field init in all constructors
			if (!checkForCommonFieldInit(ctr)) {
				return null;
			}
		}
		MethodNode ctrUseMth = ctr.getUseIn().get(0);
		ClassNode ctrUseCls = ctrUseMth.getParentClass();
		if (ctrUseCls.equals(cls)) {
			if (checkForInstanceFieldUsage(cls, ctr)) {
				return InlineType.INSTANCE_FIELD;
			}
			// exclude self usage
			return null;
		}
		if (ctrUseCls.getTopParentClass().equals(cls)) {
			// exclude usage inside inner classes
			return null;
		}
		if (!checkMethodsUsage(cls, ctr, ctrUseMth)) {
			return null;
		}
		for (FieldNode field : cls.getFields()) {
			for (MethodNode useMth : field.getUseIn()) {
				if (badMethodUsage(cls, useMth, field.getAccessFlags())) {
					return null;
				}
			}
		}
		return InlineType.CONSTRUCTOR;
	}

	private static boolean checkMethodsUsage(ClassNode cls, MethodNode ctr, MethodNode ctrUseMth) {
		for (MethodNode mth : cls.getMethods()) {
			if (mth == ctr) {
				continue;
			}
			for (MethodNode useMth : mth.getUseIn()) {
				if (useMth.equals(ctrUseMth)) {
					continue;
				}
				if (badMethodUsage(cls, useMth, mth.getAccessFlags())) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean checkForInstanceFieldUsage(ClassNode cls, MethodNode ctr) {
		MethodNode ctrUseMth = ctr.getUseIn().get(0);
		if (!ctrUseMth.getMethodInfo().isClassInit()) {
			return false;
		}
		FieldNode instFld = ListUtils.filterOnlyOne(cls.getFields(),
				f -> f.getAccessFlags().containsFlags(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL)
						&& f.getFieldInfo().getType().equals(cls.getClassInfo().getType()));
		if (instFld == null) {
			return false;
		}
		List<MethodNode> instFldUseIn = instFld.getUseIn();
		if (instFldUseIn.size() != 2
				|| !instFldUseIn.contains(ctrUseMth) // initialized in class init
				|| !instFldUseIn.containsAll(cls.getUseInMth()) // class used only with this field
		) {
			return false;
		}
		if (!checkMethodsUsage(cls, ctr, ctrUseMth)) {
			return false;
		}
		for (FieldNode field : cls.getFields()) {
			if (field == instFld) {
				continue;
			}
			for (MethodNode useMth : field.getUseIn()) {
				if (badMethodUsage(cls, useMth, field.getAccessFlags())) {
					return false;
				}
			}
		}
		instFld.add(AFlag.INLINE_INSTANCE_FIELD);
		return true;
	}

	private static boolean badMethodUsage(ClassNode cls, MethodNode useMth, AccessInfo accessFlags) {
		ClassNode useCls = useMth.getParentClass();
		if (useCls.equals(cls)) {
			return false;
		}
		if (accessFlags.isSynthetic()) {
			// allow synthetic usage in inner class
			return !useCls.getParentClass().equals(cls);
		}
		return true;
	}

	/**
	 * Checks:
	 * + all in constructors
	 * + all usage in one class
	 * - same field put (ignored: methods not loaded yet)
	 */
	private static boolean checkForCommonFieldInit(MethodNode ctrMth) {
		List<MethodNode> ctrUse = ctrMth.getUseIn();
		if (ctrUse.isEmpty()) {
			return false;
		}
		ClassNode firstUseCls = ctrUse.get(0).getParentClass();
		return ListUtils.allMatch(ctrUse, m -> m.isConstructor() && m.getParentClass().equals(firstUseCls));
	}

	@Nullable
	private static ArgType getBaseType(ClassNode cls) {
		int interfacesCount = cls.getInterfaces().size();
		if (interfacesCount > 1) {
			return null;
		}
		ArgType superCls = cls.getSuperClass();
		if (superCls == null || superCls.equals(ArgType.OBJECT)) {
			if (interfacesCount == 1) {
				return cls.getInterfaces().get(0);
			}
			return ArgType.OBJECT;
		}
		if (interfacesCount == 0) {
			return superCls;
		}
		// check if super class already implement that interface (weird case)
		ArgType interfaceType = cls.getInterfaces().get(0);
		if (cls.root().getClsp().isImplements(superCls.getObject(), interfaceType.getObject())) {
			return superCls;
		}
		if (cls.root().getArgs().isAllowInlineKotlinLambda()) {
			if (superCls.getObject().equals("kotlin.jvm.internal.Lambda")) {
				// Inline such class with have different semantic: missing 'arity' property.
				// For now, it is unclear how it may affect code execution.
				return interfaceType;
			}
		}
		return null;
	}

	@Override
	public String getName() {
		return "ProcessAnonymous";
	}
}
