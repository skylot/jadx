package jadx.gui.utils.ui;

import java.util.function.Consumer;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class DocumentUpdateListener implements DocumentListener {

	private final Consumer<DocumentEvent> listener;

	public DocumentUpdateListener(Consumer<DocumentEvent> listener) {
		this.listener = listener;
	}

	@Override
	public void insertUpdate(DocumentEvent event) {
		this.listener.accept(event);
	}

	@Override
	public void removeUpdate(DocumentEvent event) {
		this.listener.accept(event);
	}

	@Override
	public void changedUpdate(DocumentEvent event) {
		// ignore attributes change
	}
}
