package jadx.plugins.input.dex.sections;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.IFieldData;
import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.plugins.input.dex.sections.annotations.AnnotationsParser;

public class DexFieldData implements IFieldData {
	@Nullable
	private final AnnotationsParser annotationsParser;

	private String parentClassType;
	private String type;
	private String name;
	private int accessFlags;
	private int annotationsOffset;

	public DexFieldData(@Nullable AnnotationsParser parser) {
		this.annotationsParser = parser;
	}

	@Override
	public String getParentClassType() {
		return parentClassType;
	}

	public void setParentClassType(String parentClassType) {
		this.parentClassType = parentClassType;
	}

	@Override
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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

	public void setAnnotationsOffset(int annotationsOffset) {
		this.annotationsOffset = annotationsOffset;
	}

	@Override
	public List<IAnnotation> getAnnotations() {
		if (annotationsParser == null) {
			throw new NullPointerException("Annotation parser not initialized");
		}
		return annotationsParser.readAnnotationList(annotationsOffset);
	}

	@Override
	public String toString() {
		return getParentClassType() + "->" + getName() + ":" + getType();
	}
}
