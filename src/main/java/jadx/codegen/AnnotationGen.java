package jadx.codegen;

import jadx.Consts;
import jadx.dex.attributes.AttributeType;
import jadx.dex.attributes.IAttributeNode;
import jadx.dex.attributes.annotations.Annotation;
import jadx.dex.attributes.annotations.AnnotationsList;
import jadx.dex.attributes.annotations.MethodParameters;
import jadx.dex.info.FieldInfo;
import jadx.dex.instructions.args.ArgType;
import jadx.dex.nodes.ClassNode;
import jadx.dex.nodes.FieldNode;
import jadx.dex.nodes.MethodNode;
import jadx.utils.StringUtils;
import jadx.utils.exceptions.JadxRuntimeException;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class AnnotationGen {

	private final ClassNode cls;
	private final ClassGen classGen;

	public AnnotationGen(ClassNode cls, ClassGen classGen) {
		this.cls = cls;
		this.classGen = classGen;
	}

	public void addForClass(CodeWriter code) {
		add(cls, code);
	}

	public void addForMethod(CodeWriter code, MethodNode mth) {
		add(mth, code);
	}

	public void addForField(CodeWriter code, FieldNode field) {
		add(field, code);
	}

	public void addForParameter(CodeWriter code, MethodParameters paramsAnnotations, int n) {
		AnnotationsList aList = paramsAnnotations.getParamList().get(n);
		if (aList == null || aList.size() == 0)
			return;

		for (Annotation a : aList.getAll()) {
			code.add(formatAnnotation(a));
			code.add(' ');
		}
	}

	private void add(IAttributeNode node, CodeWriter code) {
		AnnotationsList aList = (AnnotationsList) node.getAttributes().get(AttributeType.ANNOTATION_LIST);
		if (aList == null || aList.size() == 0)
			return;

		for (Annotation a : aList.getAll()) {
			String aCls = a.getAnnotationClass();
			if (aCls.startsWith("dalvik.annotation.")) {
				// skip
				if (Consts.DEBUG) {
					code.startLine("// " + a);
				}
			} else {
				code.startLine();
				code.add(formatAnnotation(a));
			}
		}
	}

	private CodeWriter formatAnnotation(Annotation a) {
		CodeWriter code = new CodeWriter();
		code.add('@');
		code.add(classGen.useClass(a.getType()));
		Map<String, Object> vl = a.getValues();
		if (vl.size() != 0) {
			code.add('(');
			if (vl.size() == 1 && vl.containsKey("value")) {
				code.add(encValueToString(vl.get("value")));
			} else {
				for (Iterator<Entry<String, Object>> it = vl.entrySet().iterator(); it.hasNext();) {
					Entry<String, Object> e = it.next();
					code.add(e.getKey());
					code.add(" = ");
					code.add(encValueToString(e.getValue()));
					if (it.hasNext())
						code.add(", ");
				}
			}
			code.add(')');
		}
		return code;
	}

	@SuppressWarnings("unchecked")
	public void addThrows(MethodNode mth, CodeWriter code) {
		Annotation an = mth.getAttributes().getAnnotation("dalvik.annotation.Throws");
		if (an != null) {
			Object exs = an.getDefaultValue();
			code.add(" throws ");
			for (Iterator<ArgType> it = ((List<ArgType>) exs).iterator(); it.hasNext();) {
				ArgType ex = it.next();
				code.add(TypeGen.translate(classGen, ex));
				if (it.hasNext())
					code.add(", ");
			}
		}
	}

	public Object getAnnotationDefaultValue(String name) {
		Annotation an = cls.getAttributes().getAnnotation("dalvik.annotation.AnnotationDefault");
		if (an != null) {
			Annotation defAnnotation = (Annotation) an.getDefaultValue();
			return defAnnotation.getValues().get(name);
		}
		return null;
	}

	// TODO: refactor this boilerplate code
	@SuppressWarnings("unchecked")
	public String encValueToString(Object val) {
		if (val == null)
			return "null";

		if (val instanceof String)
			return StringUtils.unescapeString((String) val);
		if (val instanceof Integer)
			return TypeGen.formatInteger((Integer) val);
		if (val instanceof Character)
			return StringUtils.unescapeChar((Character) val);
		if (val instanceof Boolean)
			return Boolean.TRUE.equals(val) ? "true" : "false";
		if (val instanceof Float)
			return TypeGen.formatFloat((Float) val);
		if (val instanceof Double)
			return TypeGen.formatDouble((Double) val);
		if (val instanceof Long)
			return TypeGen.formatLong((Long) val);
		if (val instanceof Short)
			return TypeGen.formatShort((Short) val);
		if (val instanceof Byte)
			return TypeGen.formatByte((Byte) val);

		if (val instanceof ArgType)
			return TypeGen.translate(classGen, (ArgType) val) + ".class";

		if (val instanceof FieldInfo) {
			// must be a static field
			FieldInfo field = (FieldInfo) val;
			// FIXME: !!code from InsnGen.sfield
			String thisClass = cls.getFullName();
			if (field.getDeclClass().getFullName().equals(thisClass)) {
				return field.getName();
			} else {
				return classGen.useClass(field.getDeclClass()) + '.' + field.getName();
			}
		}

		if (val instanceof List) {
			StringBuilder str = new StringBuilder();
			str.append('{');
			List<Object> list = (List<Object>) val;
			for (Iterator<Object> it = list.iterator(); it.hasNext();) {
				Object obj = it.next();
				str.append(encValueToString(obj));
				if (it.hasNext())
					str.append(", ");
			}
			str.append('}');
			return str.toString();
		}

		if (val instanceof Annotation) {
			return formatAnnotation((Annotation) val).toString();
		}

		// TODO: also can be method values

		throw new JadxRuntimeException("Can't decode value: " + val + " (" + val.getClass() + ")");
	}
}
