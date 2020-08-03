package jadx.gui.utils.search;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

import jadx.api.JavaClass;
import jadx.gui.treemodel.CodeNode;
import jadx.gui.utils.UiUtils;

public class CodeIndex {

	private static final Logger LOG = LoggerFactory.getLogger(CodeIndex.class);

	private final List<CodeNode> values = new ArrayList<>();

	public synchronized void put(CodeNode value) {
		values.add(value);
	}

	public void removeForCls(JavaClass cls) {
		values.removeIf(v -> v.getJavaNode().getTopParentClass().equals(cls));
	}

	private boolean isMatched(StringRef key, String str, boolean caseInsensitive) {
		return key.indexOf(str, caseInsensitive) != -1;
	}

	public Flowable<CodeNode> search(final String searchStr, final boolean caseInsensitive) {
		return Flowable.create(emitter -> {
			LOG.debug("Code search started: {} ...", searchStr);
			for (CodeNode node : values) {
				if (isMatched(node.getLineStr(), searchStr, caseInsensitive)) {
					emitter.onNext(node);
				}
				if (emitter.isCancelled()) {
					LOG.debug("Code search canceled: {}", searchStr);
					return;
				}
			}
			LOG.debug("Code search complete: {}, memory usage: {}", searchStr, UiUtils.memoryInfo());
			emitter.onComplete();
		}, BackpressureStrategy.LATEST);
	}

	public int size() {
		return values.size();
	}
}
