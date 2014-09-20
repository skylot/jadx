package jadx.core.dex.visitors;

import jadx.core.codegen.TypeGen;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.nodes.EnumClassAttr;
import jadx.core.dex.attributes.nodes.EnumClassAttr.EnumField;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.exceptions.JadxException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnumVisitor extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(EnumVisitor.class);

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		if (!cls.isEnum()) {
			return true;
		}

		// collect enum fields, remove synthetic
		List<FieldNode> enumFields = new ArrayList<FieldNode>();
		for (FieldNode f : cls.getFields()) {
			if (f.getAccessFlags().isEnum()) {
				enumFields.add(f);
				f.add(AFlag.DONT_GENERATE);
			} else if (f.getAccessFlags().isSynthetic()) {
				f.add(AFlag.DONT_GENERATE);
			}
		}

		MethodNode staticMethod = null;

		ArgType clsType = cls.getClassInfo().getType();
		String enumConstructor = "<init>(Ljava/lang/String;I)V";
		String valuesOfMethod = "valueOf(Ljava/lang/String;)" + TypeGen.signature(clsType);
		String valuesMethod = "values()" + TypeGen.signature(ArgType.array(clsType));

		// remove synthetic methods
		for (Iterator<MethodNode> it = cls.getMethods().iterator(); it.hasNext(); ) {
			MethodNode mth = it.next();
			MethodInfo mi = mth.getMethodInfo();
			if (mi.isClassInit()) {
				staticMethod = mth;
			} else {
				String shortId = mi.getShortId();
				boolean isSynthetic = mth.getAccessFlags().isSynthetic();
				if (mi.isConstructor() && !isSynthetic) {
					if (shortId.equals(enumConstructor)) {
						it.remove();
					}
				} else if (isSynthetic
						|| shortId.equals(valuesMethod)
						|| shortId.equals(valuesOfMethod)) {
					it.remove();
				}
			}
		}

		EnumClassAttr attr = new EnumClassAttr(enumFields.size());
		cls.addAttr(attr);

		if (staticMethod == null) {
			ErrorsCounter.classError(cls, "Enum class init method not found");
			// for this broken enum puts found fields and mark as inconsistent
			for (FieldNode field : enumFields) {
				attr.getFields().add(new EnumField(field.getName(), 0));
			}
			return false;
		}
		attr.setStaticMethod(staticMethod);

		// move enum specific instruction from static method to separate list
		BlockNode staticBlock = staticMethod.getBasicBlocks().get(0);
		List<InsnNode> insns = new ArrayList<InsnNode>();
		List<InsnNode> list = staticBlock.getInstructions();
		int size = list.size();
		for (int i = 0; i < size; i++) {
			InsnNode insn = list.get(i);
			insns.add(insn);
			if (insn.getType() == InsnType.SPUT) {
				IndexInsnNode fp = (IndexInsnNode) insn;
				FieldInfo f = (FieldInfo) fp.getIndex();
				if (f.getName().equals("$VALUES")) {
					if (i == size - 1) {
						cls.getMethods().remove(staticMethod);
					} else {
						list.subList(0, i + 1).clear();
					}
					break;
				}
			}
		}

		for (InsnNode insn : insns) {
			if (insn.getType() == InsnType.CONSTRUCTOR) {
				ConstructorInsn co = (ConstructorInsn) insn;

				if (insn.getArgsCount() < 2) {
					continue;
				}

				ClassInfo clsInfo = co.getClassType();
				ClassNode constrCls = cls.dex().resolveClass(clsInfo);
				if (constrCls == null) {
					continue;
				}

				if (!clsInfo.equals(cls.getClassInfo()) && !constrCls.getAccessFlags().isEnum()) {
					continue;
				}

				RegisterArg nameArg = (RegisterArg) insn.getArg(0);
				// InsnArg pos = insn.getArg(1);
				// TODO add check: pos == j
				String name = (String) nameArg.getConstValue(cls.dex());
				if (name == null) {
					throw new JadxException("Unknown enum field name: " + cls);
				}

				EnumField field = new EnumField(name, insn.getArgsCount() - 2);
				attr.getFields().add(field);
				for (int i = 2; i < insn.getArgsCount(); i++) {
					InsnArg constrArg;
					InsnArg iArg = insn.getArg(i);
					if (iArg.isLiteral()) {
						constrArg = iArg;
					} else {
						constrArg = CodeShrinker.inlineArgument(staticMethod, (RegisterArg) iArg);
						if (constrArg == null) {
							throw new JadxException("Can't inline constructor arg in enum: " + cls);
						}
					}
					field.getArgs().add(constrArg);
				}

				if (co.getClassType() != cls.getClassInfo()) {
					// enum contains additional methods
					for (ClassNode innerCls : cls.getInnerClasses()) {
						if (innerCls.getClassInfo().equals(co.getClassType())) {
							// remove constructor, because it is anonymous class
							for (Iterator<?> mit = innerCls.getMethods().iterator(); mit.hasNext(); ) {
								MethodNode innerMth = (MethodNode) mit.next();
								if (innerMth.getAccessFlags().isConstructor()) {
									mit.remove();
								}
							}
							field.setCls(innerCls);
							innerCls.add(AFlag.DONT_GENERATE);
						}
					}
				}
			}
		}
		return false;
	}
}
