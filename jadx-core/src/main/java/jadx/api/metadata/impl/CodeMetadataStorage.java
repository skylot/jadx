package jadx.api.metadata.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;

import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeAnnotation.AnnType;
import jadx.api.metadata.ICodeMetadata;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.core.utils.Utils;

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
	public @Nullable ICodeAnnotation getClosestUp(int position) {
		Map.Entry<Integer, ICodeAnnotation> entryBefore = navMap.higherEntry(position);
		return entryBefore != null ? entryBefore.getValue() : null;
	}

	@Override
	public @Nullable ICodeAnnotation searchUp(int position, AnnType annType) {
		for (ICodeAnnotation v : navMap.tailMap(position, true).values()) {
			if (v.getAnnType() == annType) {
				return v;
			}
		}
		return null;
	}

	@Override
	public @Nullable ICodeAnnotation searchUp(int position, int limitPos, AnnType annType) {
		for (ICodeAnnotation v : navMap.subMap(position, true, limitPos, true).values()) {
			if (v.getAnnType() == annType) {
				return v;
			}
		}
		return null;
	}

	@Override
	public <T> @Nullable T searchUp(int startPos, BiFunction<Integer, ICodeAnnotation, T> visitor) {
		for (Map.Entry<Integer, ICodeAnnotation> entry : navMap.tailMap(startPos, true).entrySet()) {
			T value = visitor.apply(entry.getKey(), entry.getValue());
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	@Override
	public <T> @Nullable T searchDown(int startPos, BiFunction<Integer, ICodeAnnotation, T> visitor) {
		NavigableMap<Integer, ICodeAnnotation> map = navMap.headMap(startPos, true).descendingMap();
		for (Map.Entry<Integer, ICodeAnnotation> entry : map.entrySet()) {
			T value = visitor.apply(entry.getKey(), entry.getValue());
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	@Override
	public ICodeNodeRef getNodeAt(int position) {
		int nesting = 0;
		for (ICodeAnnotation ann : navMap.tailMap(position, true).values()) {
			switch (ann.getAnnType()) {
				case END:
					nesting++;
					break;

				case DECLARATION:
					ICodeNodeRef node = ((NodeDeclareRef) ann).getNode();
					AnnType nodeType = node.getAnnType();
					if (nodeType == AnnType.CLASS || nodeType == AnnType.METHOD) {
						if (nesting == 0) {
							return node;
						}
						nesting--;
					}
					break;
			}
		}
		return null;
	}

	@Override
	public ICodeNodeRef getNodeBelow(int position) {
		for (ICodeAnnotation ann : navMap.headMap(position, true).descendingMap().values()) {
			if (ann.getAnnType() == AnnType.DECLARATION) {
				ICodeNodeRef node = ((NodeDeclareRef) ann).getNode();
				AnnType nodeType = node.getAnnType();
				if (nodeType == AnnType.CLASS || nodeType == AnnType.METHOD) {
					return node;
				}
			}
		}
		return null;
	}

	@Override
	public NavigableMap<Integer, ICodeAnnotation> getAsMap() {
		return navMap;
	}

	@Override
	public Map<Integer, Integer> getLineMapping() {
		return lines;
	}

	@Override
	public String toString() {
		return "CodeMetadata{\nlines=" + lines
				+ "\nannotations=\n " + Utils.listToString(navMap.descendingMap().entrySet(), "\n ") + "\n}";
	}
}
