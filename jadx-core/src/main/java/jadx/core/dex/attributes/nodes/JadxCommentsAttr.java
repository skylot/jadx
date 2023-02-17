package jadx.core.dex.attributes.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jadx.api.CommentsLevel;
import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.utils.Utils;

public class JadxCommentsAttr implements IJadxAttribute {

	public static void add(IAttributeNode node, CommentsLevel level, String comment) {
		initFor(node).add(level, comment);
	}

	private static JadxCommentsAttr initFor(IAttributeNode node) {
		JadxCommentsAttr currentAttr = node.get(AType.JADX_COMMENTS);
		if (currentAttr != null) {
			return currentAttr;
		}
		JadxCommentsAttr newAttr = new JadxCommentsAttr();
		node.addAttr(newAttr);
		return newAttr;
	}

	private final Map<CommentsLevel, List<String>> comments = new EnumMap<>(CommentsLevel.class);

	public void add(CommentsLevel level, String comment) {
		comments.computeIfAbsent(level, (l) -> new ArrayList<>()).add(comment);
	}

	public List<String> formatAndFilter(CommentsLevel level) {
		if (level == CommentsLevel.NONE || level == CommentsLevel.USER_ONLY) {
			return Collections.emptyList();
		}
		return comments.entrySet().stream()
				.filter(e -> e.getKey().filter(level))
				.flatMap(e -> {
					String levelName = e.getKey().name();
					return e.getValue().stream()
							.map(v -> "JADX " + levelName + ": " + v);
				})
				.distinct()
				.sorted()
				.collect(Collectors.toList());
	}

	public Map<CommentsLevel, List<String>> getComments() {
		return comments;
	}

	@Override
	public IJadxAttrType<JadxCommentsAttr> getAttrType() {
		return AType.JADX_COMMENTS;
	}

	@Override
	public String toString() {
		return "JadxCommentsAttr{\n "
				+ Utils.listToString(comments.entrySet(), "\n ",
						e -> e.getKey() + ": \n -> " + Utils.listToString(e.getValue(), "\n -> "))
				+ '}';
	}
}
