package jadx.api.impl;

import java.util.Map;

import jadx.api.ICodeInfo;
import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeMetadata;
import jadx.api.metadata.impl.CodeMetadataStorage;

public class AnnotatedCodeInfo implements ICodeInfo {

	private final String code;
	private final ICodeMetadata metadata;

	public AnnotatedCodeInfo(String code, Map<Integer, Integer> lineMapping, Map<Integer, ICodeAnnotation> annotations) {
		this.code = code;
		this.metadata = CodeMetadataStorage.build(lineMapping, annotations);
	}

	@Override
	public String getCodeStr() {
		return code;
	}

	@Override
	public ICodeMetadata getCodeMetadata() {
		return metadata;
	}

	@Override
	public boolean hasMetadata() {
		return metadata != ICodeMetadata.EMPTY;
	}

	@Override
	public String toString() {
		return code;
	}
}
