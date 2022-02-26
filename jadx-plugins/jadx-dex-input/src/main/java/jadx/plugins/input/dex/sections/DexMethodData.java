package jadx.plugins.input.dex.sections;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.ICodeReader;
import jadx.api.plugins.input.data.IMethodData;
import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.api.plugins.input.data.attributes.types.AnnotationMethodParamsAttr;
import jadx.api.plugins.utils.Utils;
import jadx.plugins.input.dex.sections.annotations.AnnotationsParser;
import jadx.plugins.input.dex.smali.SmaliPrinter;

public class DexMethodData implements IMethodData {
	@Nullable
	private final AnnotationsParser annotationsParser;

	private DexMethodRef methodRef;

	private int accessFlags;
	private int annotationsOffset;
	private int paramAnnotationsOffset;

	@Nullable
	private DexCodeReader codeReader;

	public DexMethodData(@Nullable AnnotationsParser annotationsParser) {
		this.annotationsParser = annotationsParser;
	}

	@Override
	public DexMethodRef getMethodRef() {
		return methodRef;
	}

	public void setMethodRef(DexMethodRef methodRef) {
		this.methodRef = methodRef;
	}

	@Override
	public int getAccessFlags() {
		return accessFlags;
	}

	public void setAccessFlags(int accessFlags) {
		this.accessFlags = accessFlags;
	}

	@Nullable
	@Override
	public ICodeReader getCodeReader() {
		return codeReader;
	}

	public void setCodeReader(@Nullable DexCodeReader codeReader) {
		this.codeReader = codeReader;
	}

	@Override
	public String disassembleMethod() {
		return SmaliPrinter.printMethod(this);
	}

	public void setAnnotationsOffset(int annotationsOffset) {
		this.annotationsOffset = annotationsOffset;
	}

	public void setParamAnnotationsOffset(int paramAnnotationsOffset) {
		this.paramAnnotationsOffset = paramAnnotationsOffset;
	}

	private List<IAnnotation> getAnnotations() {
		return getAnnotationsParser().readAnnotationList(annotationsOffset);
	}

	private List<List<IAnnotation>> getParamsAnnotations() {
		return getAnnotationsParser().readAnnotationRefList(paramAnnotationsOffset);
	}

	@Override
	public List<IJadxAttribute> getAttributes() {
		List<IJadxAttribute> list = new ArrayList<>();
		DexAnnotationsConvert.forMethod(list, getAnnotations());
		Utils.addToList(list, AnnotationMethodParamsAttr.pack(getParamsAnnotations()));
		return list;
	}

	private AnnotationsParser getAnnotationsParser() {
		if (annotationsParser == null) {
			throw new NullPointerException("Annotation parser not initialized");
		}
		return annotationsParser;
	}

	@Override
	public String toString() {
		return getMethodRef().toString();
	}
}
