package jadx.api.impl;

import java.util.Map;

import jadx.api.CodePosition;
import jadx.api.ICodeInfo;
import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeMetadata;
import jadx.api.metadata.impl.CodeMetadataStorage;

public class AnnotatedCodeInfo implements ICodeInfo {

	private final String code;
	private final Map<Integer, Integer> lineMapping;
	private final Map<CodePosition, ICodeAnnotation> annotations;

	public AnnotatedCodeInfo(String code, Map<Integer, Integer> lineMapping, Map<CodePosition, ICodeAnnotation> annotations) {
		this.code = code;
		this.lineMapping = lineMapping;
		this.annotations = annotations;
	}

	@Override
	public String getCodeStr() {
		return code;
	}

	@Override
	public Map<Integer, Integer> getLineMapping() {
		return lineMapping;
	}

	@Override
	public Map<CodePosition, Object> getAnnotations() {
		return (Map) annotations;
	}

	@Override
	public ICodeMetadata getCodeMetadata() {
		return CodeMetadataStorage.convert(lineMapping, (Map) annotations);
	}

	@Override
	public boolean hasMetadata() {
		return !annotations.isEmpty();
	}

	@Override
	public String toString() {
		return code;
	}
}
