package jadx.plugins.input.java.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.ICodeReader;
import jadx.api.plugins.input.data.IMethodData;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.api.plugins.utils.Utils;
import jadx.plugins.input.java.data.attributes.JavaAttrStorage;
import jadx.plugins.input.java.data.attributes.JavaAttrType;
import jadx.plugins.input.java.data.attributes.types.CodeAttr;
import jadx.plugins.input.java.data.attributes.types.JavaAnnotationDefaultAttr;
import jadx.plugins.input.java.data.attributes.types.JavaAnnotationsAttr;
import jadx.plugins.input.java.data.attributes.types.JavaParamAnnsAttr;
import jadx.plugins.input.java.data.code.JavaCodeReader;

public class JavaMethodData implements IMethodData {

	private final JavaClassData clsData;
	private final JavaMethodRef methodRef;
	private int accessFlags;
	private JavaAttrStorage attributes;

	public JavaMethodData(JavaClassData clsData, JavaMethodRef methodRef) {
		this.clsData = clsData;
		this.methodRef = methodRef;
	}

	public void setData(int accessFlags, JavaAttrStorage attributes) {
		this.accessFlags = accessFlags;
		this.attributes = attributes;
	}

	@Override
	public JavaMethodRef getMethodRef() {
		return methodRef;
	}

	@Override
	public int getAccessFlags() {
		return accessFlags;
	}

	@Override
	public @Nullable ICodeReader getCodeReader() {
		CodeAttr codeAttr = attributes.get(JavaAttrType.CODE);
		if (codeAttr == null) {
			return null;
		}
		return new JavaCodeReader(clsData, codeAttr.getOffset());
	}

	@Override
	public String disassembleMethod() {
		return "";
	}

	@Override
	public List<IJadxAttribute> getAttributes() {
		int size = attributes.size();
		if (size == 0) {
			return Collections.emptyList();
		}
		List<IJadxAttribute> list = new ArrayList<>(size);
		Utils.addToList(list, JavaAnnotationsAttr.merge(attributes));
		Utils.addToList(list, JavaParamAnnsAttr.merge(attributes));
		Utils.addToList(list, JavaAnnotationDefaultAttr.convert(attributes));
		Utils.addToList(list, attributes.get(JavaAttrType.SIGNATURE));
		Utils.addToList(list, attributes.get(JavaAttrType.EXCEPTIONS));
		Utils.addToList(list, attributes.get(JavaAttrType.METHOD_PARAMETERS));
		return list;
	}

	@Override
	public String toString() {
		return getMethodRef().toString();
	}
}
