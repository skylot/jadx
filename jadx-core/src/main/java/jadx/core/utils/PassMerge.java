package jadx.core.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import jadx.api.plugins.pass.JadxPass;
import jadx.api.plugins.pass.JadxPassInfo;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class PassMerge {

	public static void run(List<IDexTreeVisitor> passes, List<JadxPass> customPasses, Function<JadxPass, IDexTreeVisitor> wrap) {
		if (Utils.isEmpty(customPasses)) {
			return;
		}
		for (JadxPass customPass : customPasses) {
			IDexTreeVisitor pass = wrap.apply(customPass);
			int pos = searchInsertPos(passes, customPass.getInfo());
			if (pos == -1) {
				passes.add(pass);
			} else {
				passes.add(pos, pass);
			}
		}
	}

	private static int searchInsertPos(List<IDexTreeVisitor> passes, JadxPassInfo info) {
		List<String> runAfter = info.runAfter();
		List<String> runBefore = info.runBefore();
		if (runAfter.isEmpty() && runBefore.isEmpty()) {
			return -1; // last
		}
		if (ListUtils.isSingleElement(runAfter, "start")) {
			return 0;
		}
		if (ListUtils.isSingleElement(runBefore, "end")) {
			return -1;
		}
		Map<String, Integer> namesMap = buildNamesMap(passes);
		int after = 0;
		for (String name : runAfter) {
			Integer pos = namesMap.get(name);
			if (pos != null) {
				after = Math.max(after, pos);
			}
		}
		int before = Integer.MAX_VALUE;
		for (String name : runBefore) {
			Integer pos = namesMap.get(name);
			if (pos != null) {
				before = Math.min(before, pos);
			}
		}
		if (before <= after) {
			throw new JadxRuntimeException("Conflict pass order requirements: " + info.getName()
					+ "\n run after: " + runAfter
					+ "\n run before: " + runBefore
					+ "\n passes: " + ListUtils.map(passes, PassMerge::getPassName));
		}
		if (after == 0) {
			return before;
		}
		int pos = after + 1;
		return pos >= passes.size() ? -1 : pos;
	}

	private static Map<String, Integer> buildNamesMap(List<IDexTreeVisitor> passes) {
		int size = passes.size();
		Map<String, Integer> namesMap = new HashMap<>(size);
		for (int i = 0; i < size; i++) {
			namesMap.put(getPassName(passes.get(i)), i);
		}
		return namesMap;
	}

	private static String getPassName(IDexTreeVisitor pass) {
		return pass.getClass().getSimpleName();
	}
}
