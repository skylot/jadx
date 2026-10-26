package jadx.gui.plugins.apiscan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jadx.core.dex.nodes.MethodNode;

public final class ApiScanResult {
	private final List<ApiUsage> usages;
	private final Map<MethodNode, List<String>> comments;

	public ApiScanResult(List<ApiUsage> usages) {
		this.usages = Collections.unmodifiableList(usages);
		this.comments = buildComments(usages);
	}

	public List<ApiUsage> getUsages() {
		return usages;
	}

	public List<String> getComments(MethodNode mth) {
		return comments.getOrDefault(mth, Collections.emptyList());
	}

	private static Map<MethodNode, List<String>> buildComments(List<ApiUsage> usages) {
		Map<MethodNode, List<String>> map = new HashMap<>();
		for (ApiUsage usage : usages) {
			String comment = "Dangerous API: " + usage.getApi() + " (" + usage.getCategory().getTitle() + ')';
			map.computeIfAbsent(usage.getMth(), m -> new ArrayList<>()).add(comment);
		}
		return map;
	}
}
