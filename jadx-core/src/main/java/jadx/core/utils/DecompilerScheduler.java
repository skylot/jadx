package jadx.core.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.IDecompileScheduler;
import jadx.api.JavaClass;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class DecompilerScheduler implements IDecompileScheduler {
	private static final Logger LOG = LoggerFactory.getLogger(DecompilerScheduler.class);

	private static final int MERGED_BATCH_SIZE = 16;
	private static final boolean DEBUG_BATCHES = false;

	@Override
	public List<List<JavaClass>> buildBatches(List<JavaClass> classes) {
		try {
			long start = System.currentTimeMillis();
			List<List<JavaClass>> result = internalBatches(classes);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Build decompilation batches in {}ms", System.currentTimeMillis() - start);
			}
			if (DEBUG_BATCHES) {
				check(result, classes);
			}
			return result;
		} catch (Throwable e) {
			LOG.warn("Build batches failed (continue with fallback)", e);
			return buildFallback(classes);
		}
	}

	/**
	 * Put classes with many dependencies at the end.
	 * Build batches for dependencies of single class to avoid locking from another thread.
	 */
	public List<List<JavaClass>> internalBatches(List<JavaClass> classes) {
		List<DepInfo> deps = sumDependencies(classes);
		Set<JavaClass> added = new HashSet<>(classes.size());
		Comparator<JavaClass> cmpDepSize = Comparator.comparingInt(JavaClass::getTotalDepsCount);
		List<List<JavaClass>> result = new ArrayList<>();
		List<JavaClass> mergedBatch = new ArrayList<>(MERGED_BATCH_SIZE);
		for (DepInfo depInfo : deps) {
			JavaClass cls = depInfo.getCls();
			if (!added.add(cls)) {
				continue;
			}
			int depsSize = cls.getTotalDepsCount();
			if (depsSize == 0) {
				// add classes without dependencies in merged batch
				mergedBatch.add(cls);
				if (mergedBatch.size() >= MERGED_BATCH_SIZE) {
					result.add(mergedBatch);
					mergedBatch = new ArrayList<>(MERGED_BATCH_SIZE);
				}
			} else {
				List<JavaClass> batch = new ArrayList<>(depsSize + 1);
				for (JavaClass dep : cls.getDependencies()) {
					JavaClass topDep = dep.getTopParentClass();
					if (!added.contains(topDep)) {
						batch.add(topDep);
						added.add(topDep);
					}
				}
				batch.sort(cmpDepSize);
				batch.add(cls);
				result.add(batch);
			}
		}
		if (mergedBatch.size() > 0) {
			result.add(mergedBatch);
		}
		if (DEBUG_BATCHES) {
			dumpBatchesStats(classes, result, deps);
		}
		return result;
	}

	private static List<DepInfo> sumDependencies(List<JavaClass> classes) {
		List<DepInfo> deps = new ArrayList<>(classes.size());
		for (JavaClass cls : classes) {
			int count = 0;
			for (JavaClass dep : cls.getDependencies()) {
				count += 1 + dep.getTotalDepsCount();
			}
			deps.add(new DepInfo(cls, count));
		}
		Collections.sort(deps);
		return deps;
	}

	private static final class DepInfo implements Comparable<DepInfo> {
		private final JavaClass cls;
		private final int depsCount;

		private DepInfo(JavaClass cls, int depsCount) {
			this.cls = cls;
			this.depsCount = depsCount;
		}

		public JavaClass getCls() {
			return cls;
		}

		public int getDepsCount() {
			return depsCount;
		}

		@Override
		public int compareTo(@NotNull DecompilerScheduler.DepInfo o) {
			int deps = Integer.compare(depsCount, o.depsCount);
			if (deps == 0) {
				return cls.getClassNode().compareTo(o.cls.getClassNode());
			}
			return deps;
		}

		@Override
		public String toString() {
			return cls + ":" + depsCount;
		}
	}

	private static List<List<JavaClass>> buildFallback(List<JavaClass> classes) {
		return classes.stream()
				.sorted(Comparator.comparingInt(c -> c.getClassNode().getTotalDepsCount()))
				.map(Collections::singletonList)
				.collect(Collectors.toList());
	}

	private void dumpBatchesStats(List<JavaClass> classes, List<List<JavaClass>> result, List<DepInfo> deps) {
		int clsInBatches = result.stream().mapToInt(List::size).sum();
		double avg = result.stream().mapToInt(List::size).average().orElse(-1);
		int maxSingleDeps = classes.stream().mapToInt(JavaClass::getTotalDepsCount).max().orElse(-1);
		int maxSubDeps = deps.stream().mapToInt(DepInfo::getDepsCount).max().orElse(-1);
		LOG.info("Batches stats:"
				+ "\n input classes: " + classes.size()
				+ ",\n classes in batches: " + clsInBatches
				+ ",\n batches: " + result.size()
				+ ",\n average batch size: " + String.format("%.2f", avg)
				+ ",\n max single deps count: " + maxSingleDeps
				+ ",\n max sub deps count: " + maxSubDeps);
	}

	private static void check(List<List<JavaClass>> result, List<JavaClass> classes) {
		int classInBatches = result.stream().mapToInt(List::size).sum();
		if (classes.size() != classInBatches) {
			throw new JadxRuntimeException(
					"Incorrect number of classes in result batch: " + classInBatches + ", expected: " + classes.size());
		}
	}
}
