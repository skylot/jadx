package jadx.gui.utils.ui;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.swing.event.CaretListener;
import javax.swing.text.JTextComponent;

/**
 * Track attached UI listeners and remove on request
 */
public class ListenersHelper<C, L> {

	public static ListenersHelper<JTextComponent, CaretListener> buildForCaretListener() {
		return new ListenersHelper<>(JTextComponent::addCaretListener, JTextComponent::removeCaretListener);
	}

	private final Map<C, List<L>> listenerMap = new IdentityHashMap<>();

	private final BiConsumer<C, L> addMth;
	private final BiConsumer<C, L> removeMth;

	private ListenersHelper(BiConsumer<C, L> add, BiConsumer<C, L> remove) {
		this.addMth = add;
		this.removeMth = remove;
	}

	public synchronized void add(C component, L listener) {
		addMth.accept(component, listener);
		listenerMap.computeIfAbsent(component, c -> new ArrayList<>()).add(listener);
	}

	public synchronized void removeAll() {
		listenerMap.forEach((comp, list) -> {
			for (L l : list) {
				remove(comp, l);
			}
		});
		listenerMap.clear();
	}

	public synchronized void removeFor(C component) {
		List<L> list = listenerMap.get(component);
		if (list != null) {
			list.forEach(l -> remove(component, l));
			listenerMap.remove(component);
		}
	}

	private void remove(C component, L listener) {
		removeMth.accept(component, listener);
	}

}
