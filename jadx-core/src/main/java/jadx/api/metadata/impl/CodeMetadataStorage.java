package jadx.api.metadata.impl;

import java.util.HashMap;
import java.util.Map;

import jadx.api.CodePosition;
import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeMetadata;

public class CodeMetadataStorage implements ICodeMetadata {

	public static CodeMetadataStorage convert(Map<Integer, Integer> lines, Map<CodePosition, Object> codePosMap) {
		if (codePosMap.isEmpty()) {
			return (CodeMetadataStorage) ICodeMetadata.EMPTY;
		}
		Map<Integer, ICodeAnnotation> map = new HashMap<>(codePosMap.size());
		for (Map.Entry<CodePosition, Object> entry : codePosMap.entrySet()) {
			map.put(entry.getKey().getPos(), (ICodeAnnotation) entry.getValue());
		}
		return new CodeMetadataStorage(lines, map);
	}

	private final Map<Integer, Integer> lines;
	private final Map<Integer, ICodeAnnotation> map;

	public CodeMetadataStorage(Map<Integer, Integer> lines, Map<Integer, ICodeAnnotation> map) {
		this.lines = lines;
		this.map = map;
	}

	@Override
	public ICodeAnnotation getAt(int position) {
		return map.get(position);
	}

	@Override
	public Map<Integer, ICodeAnnotation> getAsMap() {
		return map;
	}

	@Override
	public Map<Integer, Integer> getLineMapping() {
		return lines;
	}
}
