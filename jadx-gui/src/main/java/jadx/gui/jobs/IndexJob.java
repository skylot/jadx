package jadx.gui.jobs;

import jadx.api.JavaClass;
import jadx.core.codegen.CodeWriter;
import jadx.gui.JadxWrapper;
import jadx.gui.settings.JadxSettings;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.CodeLinesInfo;
import jadx.gui.utils.CodeUsageInfo;
import jadx.gui.utils.TextSearchIndex;
import jadx.gui.utils.Utils;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexJob extends BackgroundJob {

	private static final Logger LOG = LoggerFactory.getLogger(IndexJob.class);
	private final CacheObject cache;
	private final boolean useFastSearch;

	public IndexJob(JadxWrapper wrapper, JadxSettings settings, CacheObject cache) {
		super(wrapper, settings.getThreadsCount());
		this.useFastSearch = settings.isUseFastSearch();
		this.cache = cache;
	}

	protected void runJob() {
		final TextSearchIndex index = new TextSearchIndex();
		final CodeUsageInfo usageInfo = new CodeUsageInfo();
		cache.setTextIndex(index);
		cache.setUsageInfo(usageInfo);
		for (final JavaClass cls : wrapper.getClasses()) {
			addTask(new Runnable() {
				@Override
				public void run() {
					try {
						index.indexNames(cls);

						CodeLinesInfo linesInfo = new CodeLinesInfo(cls);
						String[] lines = splitIntoLines(cls);

						usageInfo.processClass(cls, linesInfo, lines);
						if (useFastSearch && Utils.isFreeMemoryAvailable()) {
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
	protected String[] splitIntoLines(JavaClass cls) {
		String[] lines = cls.getCode().split(CodeWriter.NL);
		int count = lines.length;
		for (int i = 0; i < count; i++) {
			lines[i] = lines[i].trim();
		}
		return lines;
	}

	@Override
	public String getInfoString() {
		return "Indexing: ";
	}

	public boolean isUseFastSearch() {
		return useFastSearch;
	}
}
