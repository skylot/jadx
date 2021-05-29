package jadx.gui.jobs;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeWriter;
import jadx.api.JavaClass;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.CodeLinesInfo;
import jadx.gui.utils.CodeUsageInfo;
import jadx.gui.utils.search.StringRef;
import jadx.gui.utils.search.TextSearchIndex;

public class IndexService {

	private static final Logger LOG = LoggerFactory.getLogger(IndexService.class);

	private final CacheObject cache;
	private boolean indexComplete;

	public IndexService(CacheObject cache) {
		this.cache = cache;
	}

	public void indexCls(JavaClass cls) {
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
			index.indexCode(cls, linesInfo, lines);
		} catch (Exception e) {
			LOG.error("Index error in class: {}", cls.getFullName(), e);
		}
	}

	public void indexResources() {
		TextSearchIndex index = cache.getTextIndex();
		index.indexResource();
	}

	public void refreshIndex(JavaClass cls) {
		TextSearchIndex index = cache.getTextIndex();
		CodeUsageInfo usageInfo = cache.getUsageInfo();
		if (index == null || usageInfo == null) {
			return;
		}
		index.remove(cls);
		usageInfo.remove(cls);
		indexCls(cls);
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

	public boolean isComplete() {
		return indexComplete;
	}

	public void setComplete(boolean indexComplete) {
		this.indexComplete = indexComplete;
	}
}
