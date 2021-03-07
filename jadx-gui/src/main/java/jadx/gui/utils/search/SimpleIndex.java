package jadx.gui.utils.search;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

import jadx.api.JavaClass;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;

public class SimpleIndex {
	private final Map<JNode, String> data = new ConcurrentHashMap<>();

	public void put(String str, JNode value) {
		data.put(value, str);
	}

	public void removeForCls(JavaClass cls) {
		data.entrySet().removeIf(e -> e.getKey().getJavaNode().getTopParentClass().equals(cls));
	}

	private boolean isMatched(String str, JNode node, SearchSettings searchSettings) {
		if (searchSettings.isMatch(str)) {
			JClass activeCls = searchSettings.getActiveCls();
			if (activeCls == null) {
				return true;
			}
			return Objects.equals(node.getRootClass(), activeCls);
		}
		return false;
	}

	public Flowable<JNode> search(final SearchSettings searchSettings) {
		return Flowable.create(emitter -> {
			for (Map.Entry<JNode, String> entry : data.entrySet()) {
				JNode node = entry.getKey();
				if (isMatched(entry.getValue(), node, searchSettings)) {
					emitter.onNext(node);
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
