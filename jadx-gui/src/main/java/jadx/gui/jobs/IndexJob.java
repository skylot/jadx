package jadx.gui.jobs;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JavaClass;
import jadx.core.codegen.CodeWriter;
import jadx.gui.JadxWrapper;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.CodeLinesInfo;
import jadx.gui.utils.CodeUsageInfo;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.Utils;
import jadx.gui.utils.search.StringRef;
import jadx.gui.utils.search.TextSearchIndex;

public class IndexJob extends BackgroundJob {

	private static final Logger LOG = LoggerFactory.getLogger(IndexJob.class);
	private final CacheObject cache;

	public IndexJob(JadxWrapper wrapper, CacheObject cache, int threadsCount) {
		super(wrapper, threadsCount);
		this.cache = cache;
	}

	protected void runJob() {
		JNodeCache nodeCache = cache.getNodeCache();
		final TextSearchIndex index = new TextSearchIndex(nodeCache);
		final CodeUsageInfo usageInfo = new CodeUsageInfo(nodeCache);
		cache.setTextIndex(index);
		cache.setUsageInfo(usageInfo);
		for (final JavaClass cls : wrapper.getClasses()) {
			addTask(new Runnable() {
				@Override
				public void run() {
					try {
						index.indexNames(cls);

						CodeLinesInfo linesInfo = new CodeLinesInfo(cls);
						List<StringRef> lines = splitLines(cls);

						usageInfo.processClass(cls, linesInfo, lines);
						if (Utils.isFreeMemoryAvailable()) {
							index.indexCode(cls, linesInfo, lines);
						} else {
							index.classCodeIndexSkipped(cls);
						}
					} catch (Exception e) {
						LOG.error("Index error in class: {}", cls.getFullName(), e);
					}
				}
			});
		}
	}

	@NotNull
	protected List<StringRef> splitLines(JavaClass cls) {
		List<StringRef> lines = StringRef.split(cls.getCode(), CodeWriter.NL);
		int size = lines.size();
		for (int i = 0; i < size; i++) {
			lines.set(i, lines.get(i).trim());
		}
		return lines;
	}

	@Override
	public String getInfoString() {
		return "Indexing: ";
	}
}
