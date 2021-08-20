package jadx.api.plugins.input.data.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import jadx.api.plugins.input.data.ISeqConsumer;

public class ListConsumer<T, R> implements ISeqConsumer<T> {
	private final Function<T, R> convert;
	private List<R> list;

	public ListConsumer(Function<T, R> convert) {
		this.convert = convert;
	}

	@Override
	public void init(int count) {
		list = count == 0 ? Collections.emptyList() : new ArrayList<>(count);
	}

	@Override
	public void accept(T t) {
		list.add(convert.apply(t));
	}

	public List<R> getResult() {
		if (list == null) {
			// init not called
			return Collections.emptyList();
		}
		return list;
	}
}
