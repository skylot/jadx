package jadx.plugins.input.dex.sections;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.ICodeReader;
import jadx.api.plugins.input.data.IMethodData;
import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.plugins.input.dex.sections.annotations.AnnotationsParser;
import jadx.plugins.input.dex.smali.SmaliPrinter;
import jadx.plugins.input.dex.utils.Utils;

public class DexMethodData implements IMethodData {
	@Nullable
	private final AnnotationsParser annotationsParser;

	private String parentClassType;
	private String returnType;
	private List<String> argTypes;
	private String name;
	private int accessFlags;
	private boolean isDirect;
	private int annotationsOffset;
	private int paramAnnotationsOffset;

	@Nullable
	private DexCodeReader codeReader;

	public DexMethodData(@Nullable AnnotationsParser annotationsParser) {
		this.annotationsParser = annotationsParser;
	}

	@Override
	public String getParentClassType() {
		return parentClassType;
	}

	public void setParentClassType(String parentClassType) {
		this.parentClassType = parentClassType;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int getAccessFlags() {
		return accessFlags;
	}

	public void setAccessFlags(int accessFlags) {
		this.accessFlags = accessFlags;
	}

	@Override
	public String getReturnType() {
		return returnType;
	}

	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}

	@Override
	public List<String> getArgTypes() {
		return argTypes;
	}

	public void setArgTypes(List<String> argTypes) {
		this.argTypes = argTypes;
	}

	@Override
	public boolean isDirect() {
		return isDirect;
	}

	public void setDirect(boolean direct) {
		isDirect = direct;
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

	@Override
	public List<IAnnotation> getAnnotations() {
		return getAnnotationsParser().readAnnotationList(annotationsOffset);
	}

	@Override
	public List<List<IAnnotation>> getParamsAnnotations() {
		return getAnnotationsParser().readAnnotationRefList(paramAnnotationsOffset);
	}

	private AnnotationsParser getAnnotationsParser() {
		if (annotationsParser == null) {
			throw new NullPointerException("Annotation parser not initialized");
		}
		return annotationsParser;
	}

	@Override
	public String toString() {
		return getParentClassType() + "->" + getName()
				+ '(' + Utils.listToStr(getArgTypes()) + ")" + getReturnType();
	}
}
