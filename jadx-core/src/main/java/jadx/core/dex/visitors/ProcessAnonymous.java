package jadx.core.dex.visitors;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.nodes.AnonymousClassBaseAttr;
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
public class ProcessAnonymous extends AbstractVisitor {

	private boolean inlineAnonymous;

	@Override
	public void init(RootNode root) {
		inlineAnonymous = root.getArgs().isInlineAnonymousClasses();
	}

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		if (!inlineAnonymous) {
			return false;
		}
		markAnonymousClass(cls);
		return true;
	}

	private static void markAnonymousClass(ClassNode cls) {
		boolean synthetic = cls.getAccessFlags().isSynthetic()
				|| cls.getClassInfo().getShortName().contains("$")
				|| Character.isDigit(cls.getClassInfo().getShortName().charAt(0));
		if (!synthetic) {
			return;
		}
		MethodNode anonymousConstructor = checkUsage(cls);
		if (anonymousConstructor == null) {
			return;
		}
		ArgType baseType = getBaseType(cls);
		if (baseType == null) {
			return;
		}

		cls.add(AFlag.ANONYMOUS_CLASS);
		cls.addAttr(new AnonymousClassBaseAttr(baseType));
		cls.add(AFlag.DONT_GENERATE);

		anonymousConstructor.add(AFlag.ANONYMOUS_CONSTRUCTOR);
		// force anonymous class to be processed before outer class,
		// actual usage of outer class will be removed at anonymous class process,
		// see ModVisitor.processAnonymousConstructor method
		ClassNode outerCls = anonymousConstructor.getUseIn().get(0).getParentClass();
		ListUtils.safeRemove(cls.getDependencies(), outerCls);
		ListUtils.safeRemove(outerCls.getUseIn(), cls);
	}

	/**
	 * Checks:
	 * - class have only one constructor which used only once (allow common code for field init)
	 * - methods or fields not used outside (allow only nested inner classes with synthetic usage)
	 *
	 * @return anonymous constructor method
	 */
	private static MethodNode checkUsage(ClassNode cls) {
		MethodNode ctr = ListUtils.filterOnlyOne(cls.getMethods(), MethodNode::isConstructor);
		if (ctr == null) {
			return null;
		}
		if (ctr.getUseIn().size() != 1) {
			// check if used in common field init in all constructors
			if (!checkForCommonFieldInit(ctr)) {
				return null;
			}
		}
		MethodNode ctrUseMth = ctr.getUseIn().get(0);
		ClassNode ctrUseCls = ctrUseMth.getParentClass();
		if (ctrUseCls.equals(cls)) {
			// exclude self usage
			return null;
		}
		for (MethodNode mth : cls.getMethods()) {
			if (mth == ctr) {
				continue;
			}
			for (MethodNode useMth : mth.getUseIn()) {
				if (useMth.equals(ctrUseMth)) {
					continue;
				}
				if (badMethodUsage(cls, useMth, mth.getAccessFlags())) {
					return null;
				}
			}
		}
		for (FieldNode field : cls.getFields()) {
			for (MethodNode useMth : field.getUseIn()) {
				if (badMethodUsage(cls, useMth, field.getAccessFlags())) {
					return null;
				}
			}
		}
		return ctr;
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
		return null;
	}
}
