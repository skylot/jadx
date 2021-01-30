package jadx.gui.utils.search;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

import jadx.api.JavaClass;
import jadx.gui.treemodel.JNode;

public class SimpleIndex {
	private final Map<JNode, String> data = new ConcurrentHashMap<>();

	public void put(String str, JNode value) {
		data.put(value, str);
	}

	public void removeForCls(JavaClass cls) {
		data.entrySet().removeIf(e -> e.getKey().getJavaNode().getTopParentClass().equals(cls));
	}

	private boolean isMatched(String str, SearchSettings searchSettings) {
		return searchSettings.isMatch(str);
	}

	public Flowable<JNode> search(final SearchSettings searchSettings) {
		return Flowable.create(emitter -> {
			for (Map.Entry<JNode, String> entry : data.entrySet()) {
				if (isMatched(entry.getValue(), searchSettings)) {
					emitter.onNext(entry.getKey());
				}
				if (emitter.isCancelled()) {
					return;
				}
			}
			emitter.onComplete();
		}, BackpressureStrategy.BUFFER);
	}

	public int size() {
		return data.size();
	}
}
