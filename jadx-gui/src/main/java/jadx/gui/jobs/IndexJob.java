package jadx.gui.jobs;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeWriter;
import jadx.api.JavaClass;
import jadx.gui.JadxWrapper;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.CodeLinesInfo;
import jadx.gui.utils.CodeUsageInfo;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.search.StringRef;
import jadx.gui.utils.search.TextSearchIndex;

public class IndexJob extends BackgroundJob {

	private static final Logger LOG = LoggerFactory.getLogger(IndexJob.class);

	private final CacheObject cache;

	public IndexJob(JadxWrapper wrapper, CacheObject cache, int threadsCount) {
		super(wrapper, threadsCount);
		this.cache = cache;
	}

	@Override
	protected void runJob() {
		TextSearchIndex index = cache.getTextIndex();
		addTask(index::indexResource);
		for (final JavaClass cls : wrapper.getIncludedClasses()) {
			addTask(() -> indexCls(cache, cls));
		}
	}

	private static void indexCls(CacheObject cache, JavaClass cls) {
		try {
			TextSearchIndex index = cache.getTextIndex();
			CodeUsageInfo usageInfo = cache.getUsageInfo();
			if (index == null || usageInfo == null) {
				return;
			}

			index.indexNames(cls);

			CodeLinesInfo linesInfo = new CodeLinesInfo(cls);
			List<StringRef> lines = splitLines(cls);

			usageInfo.processClass(cls, linesInfo, lines);
			if (UiUtils.isFreeMemoryAvailable()) {
				index.indexCode(cls, linesInfo, lines);
			} else {
				index.classCodeIndexSkipped(cls);
			}
		} catch (Exception e) {
			LOG.error("Index error in class: {}", cls.getFullName(), e);
		}
	}

	public static void refreshIndex(CacheObject cache, JavaClass cls) {
		TextSearchIndex index = cache.getTextIndex();
		CodeUsageInfo usageInfo = cache.getUsageInfo();
		if (index == null || usageInfo == null) {
			return;
		}
		index.remove(cls);
		usageInfo.remove(cls);
		indexCls(cache, cls);
	}

	@NotNull
	protected static List<StringRef> splitLines(JavaClass cls) {
		List<StringRef> lines = StringRef.split(cls.getCode(), ICodeWriter.NL);
		int size = lines.size();
		for (int i = 0; i < size; i++) {
			lines.set(i, lines.get(i).trim());
		}
		return lines;
	}

	@Override
	public String getInfoString() {
		return NLS.str("progress.index") + "… ";
	}
}
