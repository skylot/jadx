package jadx.gui.ui.dock;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.jetbrains.annotations.Nullable;

import io.github.andrewauclair.moderndocking.Dockable;
import io.github.andrewauclair.moderndocking.app.Docking;
import io.github.andrewauclair.moderndocking.ui.DockingHeaderUI;
import io.github.andrewauclair.moderndocking.ui.HeaderController;
import io.github.andrewauclair.moderndocking.ui.HeaderModel;

public class DockPanel extends JPanel implements Dockable {
	private final String id;
	private final String title;
	private final boolean closable;
	private boolean headless;
	private @Nullable Icon icon;

	public DockPanel(String id, String title, boolean closable, Component content) {
		super(new BorderLayout());
		this.id = id;
		this.title = title;
		this.closable = closable;
		add(content, BorderLayout.CENTER);
	}

	public DockPanel register() {
		Docking.registerDockable(this);
		return this;
	}

	public void setIcon(@Nullable Icon icon) {
		this.icon = icon;
	}

	public void setHeadless(boolean headless) {
		this.headless = headless;
	}

	public void setContent(Component content) {
		removeAll();
		add(content, BorderLayout.CENTER);
		revalidate();
		repaint();
	}

	@Override
	public String getPersistentID() {
		return id;
	}

	@Override
	public String getTabText() {
		return title;
	}

	@Override
	public @Nullable Icon getIcon() {
		return icon;
	}

	@Override
	public boolean isClosable() {
		return closable;
	}

	@Override
	public boolean isWrappableInScrollpane() {
		return false;
	}

	@Override
	public boolean isFloatingAllowed() {
		return !headless;
	}

	@Override
	public DockingHeaderUI createHeaderUI(HeaderController headerController, HeaderModel headerModel) {
		if (headless) {
			return new NoHeader();
		}
		return Dockable.super.createHeaderUI(headerController, headerModel);
	}

	private static final class NoHeader extends JPanel implements DockingHeaderUI {
		NoHeader() {
			setPreferredSize(new Dimension(0, 0));
			setMinimumSize(new Dimension(0, 0));
			setMaximumSize(new Dimension(0, 0));
		}

		@Override
		public void update() {
		}

		@Override
		public void displaySettingsMenu(JButton button) {
		}

		@Override
		public void setBackgroundOverride(Color color) {
		}

		@Override
		public void setForegroundOverride(Color color) {
		}
	}
}
