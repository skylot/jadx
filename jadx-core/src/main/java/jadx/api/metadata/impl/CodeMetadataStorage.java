package jadx.api.metadata.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeMetadata;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;

public class CodeMetadataStorage implements ICodeMetadata {

	public static ICodeMetadata build(Map<Integer, Integer> lines, Map<Integer, ICodeAnnotation> map) {
		if (map.isEmpty() && lines.isEmpty()) {
			return ICodeMetadata.EMPTY;
		}
		Comparator<Integer> reverseCmp = Comparator.comparingInt(Integer::intValue).reversed();
		NavigableMap<Integer, ICodeAnnotation> navMap = new TreeMap<>(reverseCmp);
		navMap.putAll(map);
		return new CodeMetadataStorage(lines, navMap);
	}

	public static ICodeMetadata empty() {
		return new CodeMetadataStorage(Collections.emptyMap(), Collections.emptyNavigableMap());
	}

	private final Map<Integer, Integer> lines;

	private final NavigableMap<Integer, ICodeAnnotation> navMap;

	private CodeMetadataStorage(Map<Integer, Integer> lines, NavigableMap<Integer, ICodeAnnotation> navMap) {
		this.lines = lines;
		this.navMap = navMap;
	}

	@Override
	public ICodeAnnotation getAt(int position) {
		return navMap.get(position);
	}

	@Override
	public ICodeAnnotation getClosestUp(int position) {
		Map.Entry<Integer, ICodeAnnotation> entryBefore = navMap.higherEntry(position);
		return entryBefore != null ? entryBefore.getValue() : null;
	}

	@Override
	public ICodeNodeRef getNodeAt(int position) {
		return navMap.tailMap(position, true).values().stream()
				.flatMap(CodeMetadataStorage::mapEnclosingNode)
				.findFirst().orElse(null);
	}

	@Override
	public ICodeNodeRef getNodeBelow(int position) {
		return navMap.headMap(position)
				.entrySet().stream()
				.sorted(Comparator.comparingInt(Map.Entry::getKey)) // reverse order to normal
				.map(Map.Entry::getValue)
				.flatMap(CodeMetadataStorage::mapEnclosingNode)
				.findFirst().orElse(null);
	}

	private static Stream<ICodeNodeRef> mapEnclosingNode(ICodeAnnotation ann) {
		if (ann instanceof NodeDeclareRef) {
			ICodeNodeRef node = ((NodeDeclareRef) ann).getNode();
			if (node instanceof ClassNode || node instanceof MethodNode) {
				return Stream.of(node);
			}
		}
		return Stream.empty();
	}

	@Override
	public Map<Integer, ICodeAnnotation> getAsMap() {
		return navMap;
	}

	@Override
	public Map<Integer, Integer> getLineMapping() {
		return lines;
	}
}
