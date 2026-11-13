package jadx.gui.ui.tab;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.*;

import org.jetbrains.annotations.Nullable;

import jadx.api.gui.code.JadxContentPanel;
import jadx.gui.code.HtmlContentPanel;
import jadx.gui.ui.codearea.AbstractCodeContentPanel;
import jadx.gui.ui.codearea.ClassCodeContentPanel;
import jadx.gui.ui.panel.FontPanel;
import jadx.gui.ui.panel.HtmlPanel;
import jadx.gui.ui.panel.ImagePanel;

class TabsFocusManager implements FocusListener {
	private static final TabsFocusManager INSTANCE = new TabsFocusManager();
	private static @Nullable Component focusedComp;

	public static boolean isActive() {
		return focusedComp != null;
	}

	public static void reset() {
		focusedComp = null;
	}

	public static Component getFocusedComp() {
		return focusedComp;
	}

	@Override
	public void focusGained(FocusEvent e) {
		focusedComp = (Component) e.getSource();
	}

	@Override
	public void focusLost(FocusEvent e) {
		focusedComp = null;
	}

	static void listen(JadxContentPanel pane) {
		if (pane instanceof ClassCodeContentPanel) {
			((ClassCodeContentPanel) pane).getCodeArea().addFocusListener(INSTANCE);
			((ClassCodeContentPanel) pane).getSmaliCodeArea().addFocusListener(INSTANCE);
			return;
		}
		if (pane instanceof AbstractCodeContentPanel) {
			((AbstractCodeContentPanel) pane).getChildrenComponent().addFocusListener(INSTANCE);
			return;
		}
		if (pane instanceof HtmlPanel) {
			((HtmlPanel) pane).getHtmlArea().addFocusListener(INSTANCE);
			return;
		}
		if (pane instanceof HtmlContentPanel) {
			((HtmlContentPanel) pane).getHtmlArea().addFocusListener(INSTANCE);
			return;
		}
		if (pane instanceof ImagePanel || pane instanceof FontPanel) {
			pane.addFocusListener(INSTANCE);
		}
	}

	static void focusOnCodePanel(JadxContentPanel pane) {
		if (pane instanceof ClassCodeContentPanel) {
			SwingUtilities.invokeLater(() -> ((ClassCodeContentPanel) pane).getCurrentCodeArea().requestFocus());
			return;
		}
		if (pane instanceof AbstractCodeContentPanel) {
			SwingUtilities.invokeLater(() -> ((AbstractCodeContentPanel) pane).getChildrenComponent().requestFocus());
			return;
		}
		if (pane instanceof HtmlPanel) {
			SwingUtilities.invokeLater(() -> ((HtmlPanel) pane).getHtmlArea().requestFocusInWindow());
			return;
		}
		if (pane instanceof HtmlContentPanel) {
			SwingUtilities.invokeLater(() -> ((HtmlContentPanel) pane).getHtmlArea().requestFocusInWindow());
			return;
		}
		if (pane instanceof ImagePanel || pane instanceof FontPanel) {
			SwingUtilities.invokeLater(pane::requestFocusInWindow);
		}
	}
}
