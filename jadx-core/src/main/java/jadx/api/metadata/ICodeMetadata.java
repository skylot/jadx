package jadx.api.metadata;

import java.util.Collections;
import java.util.Map;

import jadx.api.metadata.impl.CodeMetadataStorage;

public interface ICodeMetadata {

	ICodeMetadata EMPTY = new CodeMetadataStorage(Collections.emptyMap(), Collections.emptyMap());

	ICodeAnnotation getAt(int position);

	Map<Integer, ICodeAnnotation> getAsMap();

	Map<Integer, Integer> getLineMapping();
}
