package jadx.gui.ui;

import java.awt.*;
import java.awt.dnd.DragSource;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.Optional;

import javax.swing.*;
import javax.swing.plaf.metal.MetalTabbedPaneUI;

class DnDTabbedPane extends JTabbedPane {
	protected static final Rectangle RECT_LINE = new Rectangle();
	private static final int SCROLL_SIZE = 20; // Test
	private static final int BUTTON_SIZE = 30; // XXX 30 is magic number of scroll button size
	private static final int LINE_SIZE = 3;
	private static final Rectangle RECT_BACKWARD = new Rectangle();
	private static final Rectangle RECT_FORWARD = new Rectangle();
	// private final DropMode dropMode = DropMode.INSERT;
	protected int dragTabIndex = -1;
	private transient DnDTabbedPane.DropLocation dropLocation;

	protected DnDTabbedPane() {
		super();
		Handler h = new Handler();
		addMouseListener(h);
		addMouseMotionListener(h);
		addPropertyChangeListener(h);
	}

	public static boolean isTopBottomTabPlacement(int tabPlacement) {
		return tabPlacement == SwingConstants.TOP || tabPlacement == SwingConstants.BOTTOM;
	}

	private void clickArrowButton(String actionKey) {
		JButton scrollForwardButton = null;
		JButton scrollBackwardButton = null;
		for (Component c : getComponents()) {
			if (c instanceof JButton) {
				if (Objects.isNull(scrollForwardButton)) {
					scrollForwardButton = (JButton) c;
				} else if (Objects.isNull(scrollBackwardButton)) {
					scrollBackwardButton = (JButton) c;
				}
			}
		}
		JButton button = "scrollTabsForwardAction".equals(actionKey) ? scrollForwardButton : scrollBackwardButton;
		Optional.ofNullable(button).filter(JButton::isEnabled).ifPresent(JButton::doClick);

		// // ArrayIndexOutOfBoundsException
		// Optional.ofNullable(getActionMap())
		// .map(am -> am.get(actionKey))
		// .filter(Action::isEnabled)
		// .ifPresent(a -> a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null, 0,
		// 0)));
		// // ActionMap map = getActionMap();
		// // if (Objects.nonNull(map)) {
		// // Action action = map.get(actionKey);
		// // if (Objects.nonNull(action) && action.isEnabled()) {
		// // action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null, 0, 0));
		// // }
		// // }
	}

	public void autoScrollTest(Point pt) {
		Rectangle r = getTabAreaBounds();
		// int tabPlacement = getTabPlacement();
		// if (tabPlacement == TOP || tabPlacement == BOTTOM) {
		if (isTopBottomTabPlacement(getTabPlacement())) {
			RECT_BACKWARD.setBounds(r.x, r.y, SCROLL_SIZE, r.height);
			RECT_FORWARD.setBounds(r.x + r.width - SCROLL_SIZE - BUTTON_SIZE, r.y, SCROLL_SIZE + BUTTON_SIZE, r.height);
		} else { // if (tabPlacement == LEFT || tabPlacement == RIGHT) {
			RECT_BACKWARD.setBounds(r.x, r.y, r.width, SCROLL_SIZE);
			RECT_FORWARD.setBounds(r.x, r.y + r.height - SCROLL_SIZE - BUTTON_SIZE, r.width, SCROLL_SIZE + BUTTON_SIZE);
		}
		if (RECT_BACKWARD.contains(pt)) {
			clickArrowButton("scrollTabsBackwardAction");
		} else if (RECT_FORWARD.contains(pt)) {
			clickArrowButton("scrollTabsForwardAction");
		}
	}

	// @Override TransferHandler.DropLocation dropLocationForPoint(Point p) {
	public DnDTabbedPane.DropLocation tabDropLocationForPoint(Point p) {
		// assert dropMode == DropMode.INSERT : "Unexpected drop mode";
		for (int i = 0; i < getTabCount(); i++) {
			if (getBoundsAt(i).contains(p)) {
				return new DnDTabbedPane.DropLocation(p, i);
			}
		}
		if (getTabAreaBounds().contains(p)) {
			return new DnDTabbedPane.DropLocation(p, getTabCount());
		}
		return new DnDTabbedPane.DropLocation(p, -1);
		// switch (dropMode) {
		// case INSERT:
		// for (int i = 0; i < getTabCount(); i++) {
		// if (getBoundsAt(i).contains(p)) {
		// return new DnDTabbedPane.DropLocation(p, i);
		// }
		// }
		// if (getTabAreaBounds().contains(p)) {
		// return new DnDTabbedPane.DropLocation(p, getTabCount());
		// }
		// break;
		// case USE_SELECTION:
		// case ON:
		// case ON_OR_INSERT:
		// default:
		// assert false : "Unexpected drop mode";
		// break;
		// }
		// return new DnDTabbedPane.DropLocation(p, -1);
	}

	public final DnDTabbedPane.DropLocation getDropLocation() {
		return dropLocation;
	}

	// // WARNING:
	// // The method DnDTabbedPane.setDropLocation(TransferHandler.DropLocation, Object, boolean)
	// // does not override the inherited method from JComponent since it is private to a different
	// package
	// @Override Object setDropLocation(TransferHandler.DropLocation location, Object state, boolean
	// forDrop) {
	// DropLocation old = dropLocation;
	// if (Objects.isNull(location) || !forDrop) {
	// dropLocation = new DnDTabbedPane.DropLocation(new Point(), -1);
	// } else if (location instanceof DropLocation) {
	// dropLocation = (DropLocation) location;
	// }
	// firePropertyChange("dropLocation", old, dropLocation);
	// return null;
	// }

	public Object updateTabDropLocation(DnDTabbedPane.DropLocation location, Object state, boolean forDrop) {
		DnDTabbedPane.DropLocation old = dropLocation;
		if (Objects.isNull(location) || !forDrop) {
			dropLocation = new DnDTabbedPane.DropLocation(new Point(), -1);
		} else {
			dropLocation = location;
		}
		firePropertyChange("dropLocation", old, dropLocation);
		return state;
	}

	public void exportTab(int dragIndex, JTabbedPane target, int targetIndex) {
		// System.out.println("exportTab");
		final Component cmp = getComponentAt(dragIndex);
		final String title = getTitleAt(dragIndex);
		final Icon icon = getIconAt(dragIndex);
		final String tip = getToolTipTextAt(dragIndex);
		final boolean isEnabled = isEnabledAt(dragIndex);
		final Component tab = getTabComponentAt(dragIndex);
		// // ButtonTabComponent
		// if (tab instanceof ButtonTabComponent) {
		// tab = new ButtonTabComponent(target);
		// }

		remove(dragIndex);
		target.insertTab(title, icon, cmp, tip, targetIndex);
		target.setEnabledAt(targetIndex, isEnabled);
		target.setTabComponentAt(targetIndex, tab);
		target.setSelectedIndex(targetIndex);
		if (tab instanceof JComponent) {
			((JComponent) tab).scrollRectToVisible(tab.getBounds());
		}
	}

	public void convertTab(int prev, int next) {
		// System.out.println("convertTab");
		// if (next < 0 || prev == next) {
		// return;
		// }
		final Component cmp = getComponentAt(prev);
		final Component tab = getTabComponentAt(prev);
		final String title = getTitleAt(prev);
		final Icon icon = getIconAt(prev);
		final String tip = getToolTipTextAt(prev);
		final boolean isEnabled = isEnabledAt(prev);
		int tgtIndex = prev > next ? next : next - 1;
		remove(prev);
		insertTab(title, icon, cmp, tip, tgtIndex);
		setEnabledAt(tgtIndex, isEnabled);
		// When you drag'n'drop a disabled tab, it finishes enabled and selected.
		// pointed out by dlorde
		if (isEnabled) {
			setSelectedIndex(tgtIndex);
		}
		// I have a component in all tabs (JLabel with an X to close the tab) and when I move a tab the
		// component disappear.
		// pointed out by Daniel Dario Morales Salas
		setTabComponentAt(tgtIndex, tab);
	}

	public Optional<Rectangle> getDropLineRect() {
		int index = Optional.ofNullable(getDropLocation())
				// .filter(DnDTabbedPane.DropLocation::canDrop)
				.map(DnDTabbedPane.DropLocation::getIndex)
				.orElse(-1);
		if (index < 0) {
			RECT_LINE.setBounds(0, 0, 0, 0);
			return Optional.empty();
		}
		int a = Math.min(index, 1); // index == 0 ? 0 : 1;
		Rectangle r = getBoundsAt(a * (index - 1));
		if (isTopBottomTabPlacement(getTabPlacement())) {
			RECT_LINE.setBounds(r.x - LINE_SIZE / 2 + r.width * a, r.y, LINE_SIZE, r.height);
		} else {
			RECT_LINE.setBounds(r.x, r.y - LINE_SIZE / 2 + r.height * a, r.width, LINE_SIZE);
		}
		return Optional.of(RECT_LINE);
	}

	// public Rectangle getTabAreaBounds() {
	// Rectangle tabbedRect = getBounds();
	// Component c = getSelectedComponent();
	// if (Objects.isNull(c)) {
	// return tabbedRect;
	// }
	// int xx = tabbedRect.x;
	// int yy = tabbedRect.y;
	// Rectangle compRect = getSelectedComponent().getBounds();
	// int tabPlacement = getTabPlacement();
	// if (tabPlacement == TOP) {
	// tabbedRect.height = tabbedRect.height - compRect.height;
	// } else if (tabPlacement == BOTTOM) {
	// tabbedRect.y = tabbedRect.y + compRect.y + compRect.height;
	// tabbedRect.height = tabbedRect.height - compRect.height;
	// } else if (tabPlacement == LEFT) {
	// tabbedRect.width = tabbedRect.width - compRect.width;
	// } else { // if (tabPlacement == RIGHT) {
	// tabbedRect.x = tabbedRect.x + compRect.x + compRect.width;
	// tabbedRect.width = tabbedRect.width - compRect.width;
	// }
	// tabbedRect.translate(-xx, -yy);
	// // tabbedRect.grow(2, 2);
	// return tabbedRect;
	// }

	public Rectangle getTabAreaBounds() {
		Rectangle tabbedRect = getBounds();
		int xx = tabbedRect.x;
		int yy = tabbedRect.y;
		Rectangle compRect = Optional.ofNullable(getSelectedComponent())
				.map(Component::getBounds)
				.orElseGet(Rectangle::new);
		int tabPlacement = getTabPlacement();
		if (isTopBottomTabPlacement(tabPlacement)) {
			tabbedRect.height = tabbedRect.height - compRect.height;
			if (tabPlacement == BOTTOM) {
				tabbedRect.y += compRect.y + compRect.height;
			}
		} else {
			tabbedRect.width = tabbedRect.width - compRect.width;
			if (tabPlacement == RIGHT) {
				tabbedRect.x += compRect.x + compRect.width;
			}
		}
		tabbedRect.translate(-xx, -yy);
		// tabbedRect.grow(2, 2);
		return tabbedRect;
	}

	public static final class DropLocation extends TransferHandler.DropLocation {
		private final int index;
		// public boolean canDrop = true; // index >= 0;

		protected DropLocation(Point p, int index) {
			super(p);
			this.index = index;
		}

		public int getIndex() {
			return index;
		}

		// @Override public String toString() {
		// return getClass().getName()
		// + "[dropPoint=" + getDropPoint() + ","
		// + "index=" + index + ","
		// + "insert=" + isInsert + "]";
		// }
	}

	private class Handler extends MouseAdapter implements PropertyChangeListener { // , BeforeDrag
		private final int gestureMotionThreshold = DragSource.getDragThreshold();
		private Point startPt;
		// Toolkit tk = Toolkit.getDefaultToolkit();
		// Integer gestureMotionThreshold = (Integer) tk.getDesktopProperty("DnD.gestureMotionThreshold");

		private void repaintDropLocation() {
			Component c = getRootPane().getGlassPane();
			if (c instanceof GhostGlassPane) {
				GhostGlassPane glassPane = (GhostGlassPane) c;
				glassPane.setTargetTabbedPane(DnDTabbedPane.this);
				glassPane.repaint();
			}
		}

		// PropertyChangeListener
		@Override
		public void propertyChange(PropertyChangeEvent e) {
			String propertyName = e.getPropertyName();
			if ("dropLocation".equals(propertyName)) {
				// //System.out.println("propertyChange: dropLocation");
				repaintDropLocation();
			}
		}

		// MouseListener
		@Override
		public void mousePressed(MouseEvent e) {
			DnDTabbedPane src = (DnDTabbedPane) e.getComponent();
			boolean isOnlyOneTab = src.getTabCount() <= 1;
			if (isOnlyOneTab) {
				startPt = null;
				return;
			}
			Point tabPt = e.getPoint(); // e.getDragOrigin();
			int idx = src.indexAtLocation(tabPt.x, tabPt.y);
			// disabled tab, null component problem.
			// pointed out by daryl. NullPointerException: i.e. addTab("Tab", null)
			boolean flag = idx < 0 || !src.isEnabledAt(idx) || Objects.isNull(src.getComponentAt(idx));
			startPt = flag ? null : tabPt;
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			Point tabPt = e.getPoint(); // e.getDragOrigin();
			if (Objects.nonNull(startPt) && startPt.distance(tabPt) > gestureMotionThreshold) {
				DnDTabbedPane src = (DnDTabbedPane) e.getComponent();
				TransferHandler th = src.getTransferHandler();
				// When a tab runs rotation occurs, a tab that is not the target is dragged.
				// pointed out by Arjen
				int idx = src.indexAtLocation(tabPt.x, tabPt.y);
				int selIdx = src.getSelectedIndex();
				boolean isRotateTabRuns = !(src.getUI() instanceof MetalTabbedPaneUI)
						&& src.getTabLayoutPolicy() == JTabbedPane.WRAP_TAB_LAYOUT
						&& idx != selIdx;
				dragTabIndex = isRotateTabRuns ? selIdx : idx;
				th.exportAsDrag(src, e, TransferHandler.MOVE);
				RECT_LINE.setBounds(0, 0, 0, 0);
				src.getRootPane().getGlassPane().setVisible(true);
				src.updateTabDropLocation(new DnDTabbedPane.DropLocation(tabPt, -1), null, true);
				startPt = null;
			}
		}
	}
}
