package jadx.core.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jadx.api.plugins.pass.JadxPass;
import jadx.api.plugins.pass.JadxPassInfo;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class PassMerge {

	private final List<IDexTreeVisitor> visitors;

	private Set<String> mergePassesNames;
	private Map<IDexTreeVisitor, String> namesMap;

	public PassMerge(List<IDexTreeVisitor> visitors) {
		this.visitors = visitors;
	}

	public void merge(List<JadxPass> customPasses, Function<JadxPass, IDexTreeVisitor> wrap) {
		if (Utils.isEmpty(customPasses)) {
			return;
		}
		List<MergePass> mergePasses = ListUtils.map(customPasses, p -> new MergePass(p, wrap.apply(p), p.getInfo()));
		linkDeps(mergePasses);
		mergePasses.sort(new ExtDepsComparator(visitors).thenComparing(InvertedDepsComparator.INSTANCE));

		namesMap = new IdentityHashMap<>();
		visitors.forEach(p -> namesMap.put(p, p.getName()));
		mergePasses.forEach(p -> namesMap.put(p.getVisitor(), p.getName()));

		mergePassesNames = mergePasses.stream().map(MergePass::getName).collect(Collectors.toSet());

		for (MergePass mergePass : mergePasses) {
			int pos = searchInsertPos(mergePass);
			if (pos == -1) {
				visitors.add(mergePass.getVisitor());
			} else {
				visitors.add(pos, mergePass.getVisitor());
			}
		}
	}

	private int searchInsertPos(MergePass pass) {
		List<String> runAfter = pass.after();
		List<String> runBefore = pass.before();
		if (runAfter.isEmpty() && runBefore.isEmpty()) {
			return -1; // last
		}
		if (ListUtils.isSingleElement(runAfter, JadxPassInfo.START)) {
			return 0;
		}
		if (ListUtils.isSingleElement(runBefore, JadxPassInfo.END)) {
			return -1;
		}
		int visitorsCount = visitors.size();
		Map<String, Integer> namePosMap = new HashMap<>(visitorsCount);
		for (int i = 0; i < visitorsCount; i++) {
			namePosMap.put(namesMap.get(visitors.get(i)), i);
		}
		int after = -1;
		for (String name : runAfter) {
			Integer pos = namePosMap.get(name);
			if (pos != null) {
				after = Math.max(after, pos);
			} else {
				if (mergePassesNames.contains(name)) {
					// ignore known passes
					continue;
				}
				throw new JadxRuntimeException("Ordering pass not found: " + name
						+ ", listed in 'runAfter' of pass: " + pass
						+ "\n all passes: " + ListUtils.map(visitors, namesMap::get));
			}
		}
		int before = Integer.MAX_VALUE;
		for (String name : runBefore) {
			Integer pos = namePosMap.get(name);
			if (pos != null) {
				before = Math.min(before, pos);
			} else {
				if (mergePassesNames.contains(name)) {
					// ignore known passes
					continue;
				}
				throw new JadxRuntimeException("Ordering pass not found: " + name
						+ ", listed in 'runBefore' of pass: " + pass
						+ "\n all passes: " + ListUtils.map(visitors, namesMap::get));
			}
		}
		if (before <= after) {
			throw new JadxRuntimeException("Conflict order requirements for pass: " + pass
					+ "\n run after: " + runAfter
					+ "\n run before: " + runBefore
					+ "\n passes: " + ListUtils.map(visitors, namesMap::get));
		}
		if (after == -1) {
			if (before == Integer.MAX_VALUE) {
				// not ordered, put at last
				return -1;
			}
			return before;
		}
		int pos = after + 1;
		return pos >= visitorsCount ? -1 : pos;
	}

	private static final class MergePass {
		private final JadxPass pass;
		private final IDexTreeVisitor visitor;
		private final JadxPassInfo info;
		// copy dep lists for future modifications
		private final List<String> before;
		private final List<String> after;

		private MergePass(JadxPass pass, IDexTreeVisitor visitor, JadxPassInfo info) {
			this.pass = pass;
			this.visitor = visitor;
			this.info = info;
			this.before = new ArrayList<>(info.runBefore());
			this.after = new ArrayList<>(info.runAfter());
		}

		public JadxPass getPass() {
			return pass;
		}

		public IDexTreeVisitor getVisitor() {
			return visitor;
		}

		public String getName() {
			return info.getName();
		}

		public JadxPassInfo getInfo() {
			return info;
		}

		public List<String> before() {
			return before;
		}

		public List<String> after() {
			return after;
		}

		@Override
		public String toString() {
			return info.getName();
		}
	}

	/**
	 * Make deps double linked
	 */
	private static void linkDeps(List<MergePass> mergePasses) {
		Map<String, MergePass> map = mergePasses.stream().collect(Collectors.toMap(MergePass::getName, p -> p));
		for (MergePass pass : mergePasses) {
			for (String after : pass.getInfo().runAfter()) {
				MergePass beforePass = map.get(after);
				if (beforePass != null) {
					beforePass.before().add(pass.getName());
				}
			}
			for (String before : pass.getInfo().runBefore()) {
				MergePass afterPass = map.get(before);
				if (afterPass != null) {
					afterPass.after().add(pass.getName());
				}
			}
		}
	}

	/**
	 * Place passes with visitors dependencies before others.
	 */
	private static class ExtDepsComparator implements Comparator<MergePass> {
		private final Set<String> names;

		public ExtDepsComparator(List<IDexTreeVisitor> visitors) {
			this.names = visitors.stream()
					.map(IDexTreeVisitor::getName)
					.collect(Collectors.toSet());
		}

		@Override
		public int compare(MergePass first, MergePass second) {
			boolean isFirst = containsVisitor(first.before()) || containsVisitor(first.after());
			boolean isSecond = containsVisitor(second.before()) || containsVisitor(second.after());
			return -Boolean.compare(isFirst, isSecond);
		}

		private boolean containsVisitor(List<String> deps) {
			for (String dep : deps) {
				if (names.contains(dep)) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Sort to get inverted dependencies i.e. if pass depends on another place it before.
	 */
	private static class InvertedDepsComparator implements Comparator<MergePass> {
		public static final InvertedDepsComparator INSTANCE = new InvertedDepsComparator();

		@Override
		public int compare(MergePass first, MergePass second) {
			if (first.before().contains(second.getName())
					|| first.after().contains(second.getName())) {
				return 1;
			}
			if (second.before().contains(first.getName())
					|| second.after().contains(first.getName())) {
				return -1;
			}
			return 0;
		}
	}
}
