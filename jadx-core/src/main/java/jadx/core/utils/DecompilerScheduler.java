package jadx.core.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.IDecompileScheduler;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class DecompilerScheduler implements IDecompileScheduler {
	private static final Logger LOG = LoggerFactory.getLogger(DecompilerScheduler.class);

	private static final int MERGED_BATCH_SIZE = 16;
	private static final boolean DEBUG_BATCHES = false;

	private final JadxDecompiler decompiler;

	public DecompilerScheduler(JadxDecompiler decompiler) {
		this.decompiler = decompiler;
	}

	@Override
	public List<List<JavaClass>> buildBatches(List<JavaClass> classes) {
		long start = System.currentTimeMillis();
		List<List<ClassNode>> batches = internalBatches(Utils.collectionMap(classes, JavaClass::getClassNode));
		List<List<JavaClass>> result = Utils.collectionMap(batches, l -> Utils.collectionMapNoNull(l, decompiler::getJavaClassByNode));
		if (LOG.isDebugEnabled()) {
			LOG.debug("Build decompilation batches in {}ms", System.currentTimeMillis() - start);
		}
		if (DEBUG_BATCHES) {
			check(result, classes);
		}
		return result;
	}

	/**
	 * Put classes with many dependencies at the end.
	 * Build batches for dependencies of single class to avoid locking from another thread.
	 */
	public List<List<ClassNode>> internalBatches(List<ClassNode> classes) {
		Map<ClassNode, DepInfo> depsMap = new HashMap<>(classes.size());
		Set<ClassNode> visited = new HashSet<>();
		for (ClassNode classNode : classes) {
			visited.clear();
			sumDeps(classNode, depsMap, visited);
		}
		List<DepInfo> deps = new ArrayList<>(depsMap.values());
		Collections.sort(deps);

		Set<ClassNode> added = new HashSet<>(classes.size());
		Comparator<ClassNode> cmpDepSize = Comparator.comparingInt(c -> c.getDependencies().size());
		List<List<ClassNode>> result = new ArrayList<>();
		List<ClassNode> mergedBatch = new ArrayList<>(MERGED_BATCH_SIZE);
		for (DepInfo depInfo : deps) {
			ClassNode cls = depInfo.getCls();
			if (!added.add(cls)) {
				continue;
			}
			int depsSize = cls.getDependencies().size();
			if (depsSize == 0) {
				// add classes without dependencies in merged batch
				mergedBatch.add(cls);
				if (mergedBatch.size() >= MERGED_BATCH_SIZE) {
					result.add(mergedBatch);
					mergedBatch = new ArrayList<>(MERGED_BATCH_SIZE);
				}
			} else {
				List<ClassNode> batch = new ArrayList<>(depsSize + 1);
				for (ClassNode dep : cls.getDependencies()) {
					ClassNode topDep = dep.getTopParentClass();
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

	public int sumDeps(ClassNode cls, Map<ClassNode, DepInfo> depsMap, Set<ClassNode> visited) {
		visited.add(cls);
		DepInfo depInfo = depsMap.get(cls);
		if (depInfo != null) {
			return depInfo.getDepsCount();
		}
		List<ClassNode> deps = cls.getDependencies();
		int count = deps.size();
		for (ClassNode dep : deps) {
			if (!visited.contains(dep)) {
				count += sumDeps(dep, depsMap, visited);
			}
		}
		depsMap.put(cls, new DepInfo(cls, count));
		return count;
	}

	private static final class DepInfo implements Comparable<DepInfo> {
		private final ClassNode cls;
		private final int depsCount;

		private DepInfo(ClassNode cls, int depsCount) {
			this.cls = cls;
			this.depsCount = depsCount;
		}

		public ClassNode getCls() {
			return cls;
		}

		public int getDepsCount() {
			return depsCount;
		}

		@Override
		public int compareTo(@NotNull DecompilerScheduler.DepInfo o) {
			return Integer.compare(depsCount, o.depsCount);
		}
	}

	private void dumpBatchesStats(List<ClassNode> classes, List<List<ClassNode>> result, List<DepInfo> deps) {
		double avg = result.stream().mapToInt(List::size).average().orElse(-1);
		int maxSingleDeps = classes.stream().mapToInt(c -> c.getDependencies().size()).max().orElse(-1);
		int maxRecursiveDeps = deps.stream().mapToInt(DepInfo::getDepsCount).max().orElse(-1);
		LOG.info("Batches stats:"
				+ "\n input classes: " + classes.size()
				+ ",\n batches: " + result.size()
				+ ",\n average batch size: " + String.format("%.2f", avg)
				+ ",\n max single deps count: " + maxSingleDeps
				+ ",\n max recursive deps count: " + maxRecursiveDeps);
	}

	private static void check(List<List<JavaClass>> result, List<JavaClass> classes) {
		int classInBatches = result.stream().mapToInt(List::size).sum();
		if (classes.size() != classInBatches) {
			throw new JadxRuntimeException(
					"Incorrect number of classes in result batch: " + classInBatches + ", expected: " + classes.size());
		}
	}
}
