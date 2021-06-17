package jadx.core.codegen;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeWriter;
import jadx.api.plugins.input.data.IFieldData;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.core.Consts;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.attributes.annotations.AnnotationsList;
import jadx.core.dex.attributes.annotations.MethodParameters;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.StringUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class AnnotationGen {

	private final ClassNode cls;
	private final ClassGen classGen;

	public AnnotationGen(ClassNode cls, ClassGen classGen) {
		this.cls = cls;
		this.classGen = classGen;
	}

	public void addForClass(ICodeWriter code) {
		add(cls, code);
	}

	public void addForMethod(ICodeWriter code, MethodNode mth) {
		add(mth, code);
	}

	public void addForField(ICodeWriter code, FieldNode field) {
		add(field, code);
	}

	public void addForParameter(ICodeWriter code, MethodParameters paramsAnnotations, int n) {
		List<AnnotationsList> paramList = paramsAnnotations.getParamList();
		if (n >= paramList.size()) {
			return;
		}
		AnnotationsList aList = paramList.get(n);
		if (aList == null || aList.isEmpty()) {
			return;
		}
		for (IAnnotation a : aList.getAll()) {
			formatAnnotation(code, a);
			code.add(' ');
		}
	}

	private void add(IAttributeNode node, ICodeWriter code) {
		AnnotationsList aList = node.get(AType.ANNOTATION_LIST);
		if (aList == null || aList.isEmpty()) {
			return;
		}
		for (IAnnotation a : aList.getAll()) {
			String aCls = a.getAnnotationClass();
			if (!aCls.startsWith(Consts.DALVIK_ANNOTATION_PKG) && !aCls.equals(Consts.OVERRIDE_ANNOTATION)) {
				code.startLine();
				formatAnnotation(code, a);
			}
		}
	}

	private void formatAnnotation(ICodeWriter code, IAnnotation a) {
		code.add('@');
		ClassNode annCls = cls.root().resolveClass(a.getAnnotationClass());
		if (annCls != null) {
			classGen.useClass(code, annCls);
		} else {
			classGen.useClass(code, a.getAnnotationClass());
		}

		Map<String, EncodedValue> vl = a.getValues();
		if (!vl.isEmpty()) {
			code.add('(');
			for (Iterator<Entry<String, EncodedValue>> it = vl.entrySet().iterator(); it.hasNext();) {
				Entry<String, EncodedValue> e = it.next();
				String paramName = getParamName(annCls, e.getKey());
				if (paramName.equals("value") && vl.size() == 1) {
					// don't add "value = " if no other parameters
				} else {
					code.add(paramName);
					code.add(" = ");
				}
				encodeValue(cls.root(), code, e.getValue());
				if (it.hasNext()) {
					code.add(", ");
				}
			}
			code.add(')');
		}
	}

	private String getParamName(@Nullable ClassNode annCls, String paramName) {
		if (annCls != null) {
			// TODO: save value type and search using signature
			MethodNode mth = annCls.searchMethodByShortName(paramName);
			if (mth != null) {
				return mth.getAlias();
			}
		}
		return paramName;
	}

	public void addThrows(MethodNode mth, ICodeWriter code) {
		List<ArgType> throwList = mth.getThrows();
		if (!throwList.isEmpty()) {
			code.add(" throws ");
			for (Iterator<ArgType> it = throwList.iterator(); it.hasNext();) {
				ArgType ex = it.next();
				classGen.useType(code, ex);
				if (it.hasNext()) {
					code.add(", ");
				}
			}
		}
	}

	public EncodedValue getAnnotationDefaultValue(String name) {
		IAnnotation an = cls.getAnnotation(Consts.DALVIK_ANNOTATION_DEFAULT);
		if (an != null) {
			EncodedValue defValue = an.getDefaultValue();
			if (defValue != null) {
				IAnnotation defAnnotation = (IAnnotation) defValue.getValue();
				return defAnnotation.getValues().get(name);
			}
		}
		return null;
	}

	// TODO: refactor this boilerplate code
	public void encodeValue(RootNode root, ICodeWriter code, EncodedValue encodedValue) {
		if (encodedValue == null) {
			code.add("null");
			return;
		}
		Object value = encodedValue.getValue();
		switch (encodedValue.getType()) {
			case ENCODED_NULL:
				code.add("null");
				break;
			case ENCODED_BOOLEAN:
				code.add(Boolean.TRUE.equals(value) ? "true" : "false");
				break;
			case ENCODED_BYTE:
				code.add(TypeGen.formatByte((Byte) value, false));
				break;
			case ENCODED_SHORT:
				code.add(TypeGen.formatShort((Short) value, false));
				break;
			case ENCODED_CHAR:
				code.add(getStringUtils().unescapeChar((Character) value));
				break;
			case ENCODED_INT:
				code.add(TypeGen.formatInteger((Integer) value, false));
				break;
			case ENCODED_LONG:
				code.add(TypeGen.formatLong((Long) value, false));
				break;
			case ENCODED_FLOAT:
				code.add(TypeGen.formatFloat((Float) value));
				break;
			case ENCODED_DOUBLE:
				code.add(TypeGen.formatDouble((Double) value));
				break;
			case ENCODED_STRING:
				code.add(getStringUtils().unescapeString((String) value));
				break;
			case ENCODED_TYPE:
				classGen.useType(code, ArgType.parse((String) value));
				code.add(".class");
				break;
			case ENCODED_ENUM:
			case ENCODED_FIELD:
				// must be a static field
				if (value instanceof IFieldData) {
					FieldInfo field = FieldInfo.fromData(root, (IFieldData) value);
					InsnGen.makeStaticFieldAccess(code, field, classGen);
				} else if (value instanceof FieldInfo) {
					InsnGen.makeStaticFieldAccess(code, (FieldInfo) value, classGen);
				} else {
					throw new JadxRuntimeException("Unexpected field type class: " + value.getClass());
				}
				break;
			case ENCODED_METHOD:
				// TODO
				break;
			case ENCODED_ARRAY:
				code.add('{');
				Iterator<?> it = ((Iterable<?>) value).iterator();
				while (it.hasNext()) {
					EncodedValue v = (EncodedValue) it.next();
					encodeValue(cls.root(), code, v);
					if (it.hasNext()) {
						code.add(", ");
					}
				}
				code.add('}');
				break;
			case ENCODED_ANNOTATION:
				formatAnnotation(code, (IAnnotation) value);
				break;

			default:
				throw new JadxRuntimeException("Can't decode value: " + encodedValue.getType() + " (" + encodedValue + ')');
		}
	}

	private StringUtils getStringUtils() {
		return cls.root().getStringUtils();
	}
}
