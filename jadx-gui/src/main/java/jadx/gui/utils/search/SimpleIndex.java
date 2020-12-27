package jadx.gui.utils.search;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

import jadx.api.JavaClass;
import jadx.gui.treemodel.JNode;

public class SimpleIndex {
	private static final Logger LOG = LoggerFactory.getLogger(SimpleIndex.class);
	private final Map<JNode, String> data = new ConcurrentHashMap<>();

	public void put(String str, JNode value) {
		data.put(value, str);
	}

	public void removeForCls(JavaClass cls) {
		data.entrySet().removeIf(e -> e.getKey().getJavaNode().getTopParentClass().equals(cls));
	}

	private boolean isMatched(String str, String searchStr, boolean caseInsensitive, boolean useRegex) {
		if (caseInsensitive && useRegex) {
			try {
				Pattern pattern = Pattern.compile(searchStr, Pattern.CASE_INSENSITIVE);
				Matcher matcher = pattern.matcher(str);
				return matcher.find();
			} catch (Exception e) {
				LOG.warn("Invalid Regex: {}", searchStr);
				return false;
			}
		} else if (useRegex) {
			try {
				Pattern pattern = Pattern.compile(searchStr);
				Matcher matcher = pattern.matcher(str);
				return matcher.find();
			} catch (Exception e) {
				LOG.warn("Invalid Regex: {}", searchStr);
				return false;
			}
		} else if (caseInsensitive) {
			return StringUtils.containsIgnoreCase(str, searchStr);
		} else {
			return str.contains(searchStr);
		}
	}

	public Flowable<JNode> search(final String searchStr, final boolean caseInsensitive, final boolean useRegex) {
		return Flowable.create(emitter -> {
			for (Map.Entry<JNode, String> entry : data.entrySet()) {
				if (isMatched(entry.getValue(), searchStr, caseInsensitive, useRegex)) {
					emitter.onNext(entry.getKey());
				}
				if (emitter.isCancelled()) {
					return;
				}
			}
			emitter.onComplete();
		}, BackpressureStrategy.LATEST);
	}

	public int size() {
		return data.size();
	}
}
