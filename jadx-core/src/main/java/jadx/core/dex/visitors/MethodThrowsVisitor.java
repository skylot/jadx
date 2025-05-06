package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.types.ExceptionsAttr;
import jadx.api.plugins.input.data.attributes.types.MethodThrowsAttr;
import jadx.core.Consts;
import jadx.core.clsp.ClspClass;
import jadx.core.clsp.ClspMethod;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.visitors.regions.RegionMakerVisitor;
import jadx.core.dex.visitors.typeinference.TypeCompare;
import jadx.core.dex.visitors.typeinference.TypeCompareEnum;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "MethodThrowsVisitor",
		desc = "Scan methods to collect thrown exceptions",
		runAfter = {
				RegionMakerVisitor.class // Run after RegionMakerVisitor to ignore throw instructions of synchronized regions
		}
)
public class MethodThrowsVisitor extends AbstractVisitor {

	private static final Logger LOG = LoggerFactory.getLogger(MethodThrowsVisitor.class);

	enum ExceptionType {
		THROWS_REQUIRED, RUNTIME, UNKNOWN_TYPE, NO_EXCEPTION
	}

	private RootNode root;

	@Override
	public void init(RootNode root) throws JadxException {
		this.root = root;
	}

	@Override
	public void visit(MethodNode mth) throws JadxException {
		MethodThrowsAttr attr = mth.get(JadxAttrType.METHOD_THROWS);
		if (attr == null) {
			attr = new MethodThrowsAttr(new HashSet<>());
			mth.addAttr(attr);
		}
		if (!attr.isVisited()) {
			attr.setVisited(true);
			processInstructions(mth);
		}

		ExceptionsAttr exceptions = mth.get(JadxAttrType.EXCEPTIONS);
		if (exceptions != null && !exceptions.getList().isEmpty()) {
			for (String throwsAnnotation : exceptions.getList()) {
				if (throwsAnnotation.startsWith("L") && throwsAnnotation.endsWith(";")) {
					throwsAnnotation = throwsAnnotation.substring(1, throwsAnnotation.length() - 1).replace('/', '.');
				}
				if (validateException(throwsAnnotation) == ExceptionType.NO_EXCEPTION) {
					mth.addWarnComment("Byte code manipulation detected: skipped illegal throws declaration");
				} else {
					attr.getList().add(throwsAnnotation);
				}
			}
		}
		mergeExceptions(attr.getList());
	}

	private void mergeExceptions(Set<String> list) {
		if (list.contains("java.lang.Exception")) {
			list.removeIf(e -> !e.equals(Consts.CLASS_EXCEPTION));
			return;
		}
		if (list.contains("java.lang.Throwable")) {
			list.removeIf(e -> !e.equals(Consts.CLASS_THROWABLE));
			return;
		}
		List<String> toRemove = new ArrayList<>();
		for (String ex1 : list) {
			for (String ex2 : list) {
				if (ex1.equals(ex2)) {
					continue;
				}
				if (isBaseException(ex1, ex2, root)) {
					toRemove.add(ex1);
				}
			}
		}
		list.removeAll(toRemove);
	}

	/**
	 * @param exception
	 * @param possibleParent
	 * @param root
	 * @return is 'possibleParent' a exception class of 'exception'
	 */
	public static boolean isBaseException(String exception, String possibleParent, RootNode root) {
		ClassNode classNode = root.resolveClass(exception);
		if (classNode != null) {
			ArgType superClass = classNode.getSuperClass();
			if (superClass == null || superClass.getObject().equals(Consts.CLASS_THROWABLE)) {
				return false;
			}
			if (superClass.getObject().equals(possibleParent)) {
				return true;
			}
			return isBaseException(superClass.getObject(), possibleParent, root);
		}
		ArgType clspClass = ClassInfo.fromName(root, exception).getType();
		return isClspBaseExcecption(root, clspClass, possibleParent);
	}

	/**
	 * @param root
	 * @param clspClass
	 * @param possibleParent
	 * @return is 'possibleParent' a base exception of 'clspClass'
	 */
	private static boolean isClspBaseExcecption(RootNode root, ArgType clspClass, String possibleParent) {
		ClspClass clsDetails = root.getClsp().getClsDetails(clspClass);
		if (clsDetails != null) {
			for (ArgType parent : clsDetails.getParents()) {
				if (parent.getObject().equals(Consts.CLASS_OBJECT)) {
					continue;
				}
				if (parent.getObject().equals(Consts.CLASS_THROWABLE)) {
					return false;
				}
				if (parent.getObject().equals(possibleParent) || isClspBaseExcecption(root, parent, possibleParent)) {
					return true;
				}
			}
		}
		return false;
	}

	private void processInstructions(MethodNode mth) throws JadxException {
		if (mth.isNoCode() || mth.getBasicBlocks() == null) {
			return;
		}

		blocks: for (final BlockNode block : mth.getBasicBlocks()) {
			// Skip e.g. throw instructions of synchronized regions
			boolean skipExceptions = block.contains(AFlag.REMOVE) || block.contains(AFlag.DONT_GENERATE);
			Set<String> excludedExceptions = new HashSet<>();
			CatchAttr catchAttr = block.get(AType.EXC_CATCH);
			if (catchAttr != null) {
				for (ExceptionHandler handler : catchAttr.getHandlers()) {
					if (handler.isCatchAll()) {
						continue blocks;
					}
					excludedExceptions.add(handler.getArgType().toString());
				}
			}
			for (final InsnNode insn : block.getInstructions()) {
				checkInsn(mth, insn, excludedExceptions, skipExceptions);
			}
		}
	}

	private void checkInsn(MethodNode mth, InsnNode insn, Set<String> excludedExceptions, boolean skipExceptions) throws JadxException {
		if (!skipExceptions && insn.getType() == InsnType.THROW && !insn.contains(AFlag.DONT_GENERATE)) {
			if (insn.getArg(0) instanceof RegisterArg) {
				RegisterArg regArg = (RegisterArg) insn.getArg(0);
				ArgType exceptionType = regArg.getSVar().getAssign().getInitType();
				visitThrows(mth, exceptionType);
			} else {
				if (insn.getArg(0) instanceof InsnWrapArg) {
					InsnWrapArg insnWrapArg = (InsnWrapArg) insn.getArg(0);
					ArgType exceptionType = insnWrapArg.getType();
					visitThrows(mth, exceptionType);
				}
			}
			return;
		}

		if (insn.getType() == InsnType.INVOKE) {
			final InvokeNode invokeNode = (InvokeNode) insn;
			final MethodInfo callMth = invokeNode.getCallMth();

			String signature = callMth.makeSignature(true);
			final ClassInfo classInfo = callMth.getDeclClass();
			ArgType type = classInfo.getType();

			ClassNode classNode = root.resolveClass(type);
			if (classNode != null) {
				MethodNode cMth = searchOverriddenMethod(classNode, callMth, signature);
				if (cMth == null) {
					return;
				}
				visit(cMth);
				MethodThrowsAttr cAttr = cMth.get(JadxAttrType.METHOD_THROWS);
				MethodThrowsAttr attr = mth.get(JadxAttrType.METHOD_THROWS);
				if (attr != null && cAttr != null && !cAttr.getList().isEmpty()) {
					attr.getList().addAll(filterExceptions(cAttr.getList(), excludedExceptions));
				}
			} else {
				ClspClass clsDetails = root.getClsp().getClsDetails(type);
				if (clsDetails != null) {
					ClspMethod cMth = searchOverriddenMethod(clsDetails, signature);
					if (cMth != null && cMth.getThrows() != null && !cMth.getThrows().isEmpty()) {
						MethodThrowsAttr attr = mth.get(JadxAttrType.METHOD_THROWS);
						if (attr != null) {
							for (ArgType ex : cMth.getThrows()) {
								attr.getList().add(ex.getObject());
							}
						}
					}
				}
			}
		}
	}

	private void visitThrows(MethodNode mth, ArgType exceptionType) {
		if (isThrowsRequired(exceptionType)) {
			String ex = exceptionType.getObject();
			MethodThrowsAttr attr = mth.get(JadxAttrType.METHOD_THROWS);
			attr.getList().add(ex);
		}
	}

	private boolean isThrowsRequired(ArgType type) {
		ClassNode classNode = root.resolveClass(type);
		if (classNode != null) {
			return validateException(classNode) == ExceptionType.THROWS_REQUIRED;
		} else {
			if (type.isTypeKnown()) {
				ClspClass clsDetails = root.getClsp().getClsDetails(type);
				if (clsDetails == null) {
					LOG.warn("Thrown type has an unknown type hierarchy: {}", type.getObject());
					return true; // assume an exception
				}
				return validateException(clsDetails) == ExceptionType.THROWS_REQUIRED;
			}
		}
		return false;
	}

	private ExceptionType validateException(String exception) {
		ClassNode classNode = root.resolveClass(exception);
		if (classNode != null) {
			return validateException(classNode);
		}
		ArgType clspClass = ClassInfo.fromName(root, exception).getType();
		ClspClass clsDetails = root.getClsp().getClsDetails(clspClass);
		if (clsDetails == null) {
			return ExceptionType.UNKNOWN_TYPE;
		}
		return validateException(clsDetails);
	}

	private ExceptionType validateException(ClspClass clsDetails) {
		if (clsDetails.getName().equals(Consts.CLASS_THROWABLE) || clsDetails.getName().equals(Consts.CLASS_EXCEPTION)) {
			return ExceptionType.THROWS_REQUIRED;
		}
		if (clsDetails.getName().equals("java.lang.Error") || clsDetails.getName().equals("java.lang.RuntimeException")) {
			return ExceptionType.RUNTIME;
		}
		ArgType[] parents = clsDetails.getParents();
		for (ArgType type : parents) {
			ClspClass p = root.getClsp().getClsDetails(type);
			if (p != null) {
				ExceptionType errorType = validateException(p);
				if (errorType == ExceptionType.THROWS_REQUIRED
						|| errorType == ExceptionType.RUNTIME) {
					return errorType;
				}
			}
		}
		return ExceptionType.NO_EXCEPTION;
	}

	private ExceptionType validateException(ClassNode classNode) {
		ArgType superClass = classNode.getSuperClass();
		if (superClass == null || Consts.CLASS_OBJECT.equals(superClass.toString())) {
			return ExceptionType.NO_EXCEPTION;
		}
		if (Consts.CLASS_THROWABLE.equals(superClass.toString()) || Consts.CLASS_EXCEPTION.equals(superClass.toString())) {
			return ExceptionType.THROWS_REQUIRED;
		}
		if ("java.lang.Error".equals(superClass.toString()) || "java.lang.RuntimeException".equals(superClass.toString())) {
			return ExceptionType.RUNTIME;
		}
		ClassNode parentClassNode = root.resolveClass(superClass.toString());
		if (parentClassNode != null) {
			return validateException(parentClassNode);
		} else {
			ArgType type = ClassInfo.fromName(root, superClass.toString()).getType();
			ClspClass parentClass = root.getClsp().getClsDetails(type);
			if (parentClass != null) {
				return validateException(parentClass);
			}
			return ExceptionType.NO_EXCEPTION;
		}
	}

	private Collection<String> filterExceptions(Set<String> exceptions, Set<String> excludedExceptions) {
		Set<String> filteredExceptions = new HashSet<>();
		for (String exception : exceptions) {
			boolean filtered = false;
			for (String exlcuded : excludedExceptions) {
				filtered = exception.equals(exlcuded) || isBaseException(exception, exlcuded, this.root);
				if (filtered) {
					break;
				}
			}
			if (!filtered) {
				filteredExceptions.add(exception);
			}
		}
		return filteredExceptions;
	}

	@Nullable
	private MethodNode searchOverriddenMethod(ClassNode cls, MethodInfo mth, String signature) {
		// search by exact full signature (with return value) to fight obfuscation (see test
		// 'TestOverrideWithSameName')
		String shortId = mth.getShortId();
		for (MethodNode supMth : cls.getMethods()) {
			if (supMth.getMethodInfo().getShortId().equals(shortId)) {
				return supMth;
			}
		}
		// search by signature without return value and check if return value is wider type
		for (MethodNode supMth : cls.getMethods()) {
			if (supMth.getMethodInfo().getShortId().startsWith(signature) && !supMth.getAccessFlags().isStatic()) {
				TypeCompare typeCompare = cls.root().getTypeCompare();
				ArgType supRetType = supMth.getMethodInfo().getReturnType();
				ArgType mthRetType = mth.getReturnType();
				TypeCompareEnum res = typeCompare.compareTypes(supRetType, mthRetType);
				if (res.isWider()) {
					return supMth;
				}
			}
		}
		return null;
	}

	private ClspMethod searchOverriddenMethod(ClspClass clsDetails, String signature) {
		Map<String, ClspMethod> methodsMap = clsDetails.getMethodsMap();
		for (Map.Entry<String, ClspMethod> entry : methodsMap.entrySet()) {
			String mthShortId = entry.getKey();
			// do not check full signature, classpath methods can be trusted
			// i.e. doesn't contain methods with same signature in one class
			if (mthShortId.startsWith(signature)) {
				return entry.getValue();
			}
		}
		return null;
	}
}
