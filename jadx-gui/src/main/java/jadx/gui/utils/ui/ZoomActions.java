package jadx.gui.utils.ui;

import java.awt.Container;
import java.awt.Font;
import java.awt.event.KeyEvent;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import jadx.gui.settings.JadxSettings;
import jadx.gui.utils.UiUtils;

public class ZoomActions {
	private final JComponent component;
	private final JadxSettings settings;
	private final Runnable update;

	public static void register(JComponent component, JadxSettings settings, Runnable update) {
		ZoomActions actions = new ZoomActions(component, settings, update);
		actions.register();
	}

	private ZoomActions(JComponent component, JadxSettings settings, Runnable update) {
		this.component = component;
		this.settings = settings;
		this.update = update;
	}

	private void register() {
		String zoomIn = "TextZoomIn";
		String zoomOut = "TextZoomOut";
		int ctrlButton = UiUtils.ctrlButton();
		InputMap inputMap = component.getInputMap();
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ctrlButton), zoomIn);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, ctrlButton), zoomIn);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, ctrlButton), zoomIn);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ctrlButton), zoomOut);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, ctrlButton), zoomOut);
		ActionMap actionMap = component.getActionMap();
		actionMap.put(zoomIn, new ActionHandler(e -> textZoom(1)));
		actionMap.put(zoomOut, new ActionHandler(e -> textZoom(-1)));

		component.addMouseWheelListener(e -> {
			if (e.getModifiersEx() == UiUtils.ctrlButton()) {
				textZoom(e.getWheelRotation() < 0 ? 1 : -1);
				e.consume();
			} else {
				// pass event to parent component, needed for scroll in JScrollPane
				Container parent = component.getParent();
				if (parent != null) {
					parent.dispatchEvent(e);
				}
			}
		});
	}

	private void textZoom(int change) {
		Font font = settings.getFont();
		if (component.getFont().equals(font)) {
			settings.setFont(changeFontSize(font, change));
		} else {
			settings.setSmaliFont(changeFontSize(settings.getSmaliFont(), change));
		}
		settings.sync();
		update.run();
	}

	private Font changeFontSize(Font font, int change) {
		float newSize = font.getSize() + change;
		if (newSize < 2) {
			// ignore change
			return font;
		}
		return font.deriveFont(newSize);
	}
}
