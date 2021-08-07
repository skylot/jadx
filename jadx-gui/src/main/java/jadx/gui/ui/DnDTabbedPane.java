package jadx.gui.ui;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import javax.swing.*;
import javax.swing.plaf.metal.MetalTabbedPaneUI;

class DnDTabbedPane extends JTabbedPane {
	private static final int SCROLL_SIZE = 20; // Test
	private static final int BUTTON_SIZE = 30; // XXX 30 is magic number of scroll button size
	private static final int LINE_SIZE = 3;
	private static final Rectangle RECT_BACKWARD = new Rectangle();
	private static final Rectangle RECT_FORWARD = new Rectangle();
	protected static final Rectangle RECT_LINE = new Rectangle();
	// private final DropMode dropMode = DropMode.INSERT;
	protected int dragTabIndex = -1;
	private transient DnDTabbedPane.DropLocation dropLocation;

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
		//   return getClass().getName()
		//     + "[dropPoint=" + getDropPoint() + ","
		//     + "index=" + index + ","
		//     + "insert=" + isInsert + "]";
		// }
	}

	private void clickArrowButton(String actionKey) {
		JButton scrollForwardButton = null;
		JButton scrollBackwardButton = null;
		for (Component c: getComponents()) {
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
		//   .map(am -> am.get(actionKey))
		//   .filter(Action::isEnabled)
		//   .ifPresent(a -> a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null, 0, 0)));
		// // ActionMap map = getActionMap();
		// // if (Objects.nonNull(map)) {
		// //   Action action = map.get(actionKey);
		// //   if (Objects.nonNull(action) && action.isEnabled()) {
		// //     action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null, 0, 0));
		// //   }
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

	protected DnDTabbedPane() {
		super();
		Handler h = new Handler();
		addMouseListener(h);
		addMouseMotionListener(h);
		addPropertyChangeListener(h);
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
		//   case INSERT:
		//     for (int i = 0; i < getTabCount(); i++) {
		//       if (getBoundsAt(i).contains(p)) {
		//         return new DnDTabbedPane.DropLocation(p, i);
		//       }
		//     }
		//     if (getTabAreaBounds().contains(p)) {
		//       return new DnDTabbedPane.DropLocation(p, getTabCount());
		//     }
		//     break;
		//   case USE_SELECTION:
		//   case ON:
		//   case ON_OR_INSERT:
		//   default:
		//     assert false : "Unexpected drop mode";
		//     break;
		// }
		// return new DnDTabbedPane.DropLocation(p, -1);
	}

	public final DnDTabbedPane.DropLocation getDropLocation() {
		return dropLocation;
	}

	// // WARNING:
	// // The method DnDTabbedPane.setDropLocation(TransferHandler.DropLocation, Object, boolean)
	// // does not override the inherited method from JComponent since it is private to a different package
	// @Override Object setDropLocation(TransferHandler.DropLocation location, Object state, boolean forDrop) {
	//   DropLocation old = dropLocation;
	//   if (Objects.isNull(location) || !forDrop) {
	//     dropLocation = new DnDTabbedPane.DropLocation(new Point(), -1);
	//   } else if (location instanceof DropLocation) {
	//     dropLocation = (DropLocation) location;
	//   }
	//   firePropertyChange("dropLocation", old, dropLocation);
	//   return null;
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
		//System.out.println("exportTab");
		final Component cmp = getComponentAt(dragIndex);
		final String title = getTitleAt(dragIndex);
		final Icon icon = getIconAt(dragIndex);
		final String tip = getToolTipTextAt(dragIndex);
		final boolean isEnabled = isEnabledAt(dragIndex);
		final Component tab = getTabComponentAt(dragIndex);
		// // ButtonTabComponent
		// if (tab instanceof ButtonTabComponent) {
		//   tab = new ButtonTabComponent(target);
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
		//System.out.println("convertTab");
		// if (next < 0 || prev == next) {
		//   return;
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
		// I have a component in all tabs (JLabel with an X to close the tab) and when I move a tab the component disappear.
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
	//   Rectangle tabbedRect = getBounds();
	//   Component c = getSelectedComponent();
	//   if (Objects.isNull(c)) {
	//     return tabbedRect;
	//   }
	//   int xx = tabbedRect.x;
	//   int yy = tabbedRect.y;
	//   Rectangle compRect = getSelectedComponent().getBounds();
	//   int tabPlacement = getTabPlacement();
	//   if (tabPlacement == TOP) {
	//     tabbedRect.height = tabbedRect.height - compRect.height;
	//   } else if (tabPlacement == BOTTOM) {
	//     tabbedRect.y = tabbedRect.y + compRect.y + compRect.height;
	//     tabbedRect.height = tabbedRect.height - compRect.height;
	//   } else if (tabPlacement == LEFT) {
	//     tabbedRect.width = tabbedRect.width - compRect.width;
	//   } else { // if (tabPlacement == RIGHT) {
	//     tabbedRect.x = tabbedRect.x + compRect.x + compRect.width;
	//     tabbedRect.width = tabbedRect.width - compRect.width;
	//   }
	//   tabbedRect.translate(-xx, -yy);
	//   // tabbedRect.grow(2, 2);
	//   return tabbedRect;
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

	public static boolean isTopBottomTabPlacement(int tabPlacement) {
		return tabPlacement == SwingConstants.TOP || tabPlacement == SwingConstants.BOTTOM;
	}

	private class Handler extends MouseAdapter implements PropertyChangeListener { // , BeforeDrag
		private Point startPt;
		private final int gestureMotionThreshold = DragSource.getDragThreshold();
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
		@Override public void propertyChange(PropertyChangeEvent e) {
			String propertyName = e.getPropertyName();
			if ("dropLocation".equals(propertyName)) {
				// //System.out.println("propertyChange: dropLocation");
				repaintDropLocation();
			}
		}

		// MouseListener
		@Override public void mousePressed(MouseEvent e) {
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

		@Override public void mouseDragged(MouseEvent e) {
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

class TabDropTargetAdapter extends DropTargetAdapter {
	private void clearDropLocationPaint(Component c) {
		if (c instanceof DnDTabbedPane) {
			DnDTabbedPane t = (DnDTabbedPane) c;
			t.updateTabDropLocation(null, null, false);
			t.setCursor(Cursor.getDefaultCursor());
		}
	}

	@Override public void drop(DropTargetDropEvent dtde) {
		Component c = dtde.getDropTargetContext().getComponent();
		//System.out.println("DropTargetListener#drop: " + c.getName());
		clearDropLocationPaint(c);
	}

	@Override public void dragExit(DropTargetEvent dte) {
		Component c = dte.getDropTargetContext().getComponent();
		//System.out.println("DropTargetListener#dragExit: " + c.getName());
		clearDropLocationPaint(c);
	}

	@Override public void dragEnter(DropTargetDragEvent dtde) {
		Component c = dtde.getDropTargetContext().getComponent();
		//System.out.println("DropTargetListener#dragEnter: " + c.getName());
	}

	// @Override public void dragOver(DropTargetDragEvent dtde) {
	//   // //System.out.println("dragOver");
	// }

	// @Override public void dropActionChanged(DropTargetDragEvent dtde) {
	//   //System.out.println("dropActionChanged");
	// }
}

class DnDTabData {
	public final DnDTabbedPane tabbedPane;

	protected DnDTabData(DnDTabbedPane tabbedPane) {
		this.tabbedPane = tabbedPane;
	}
}

class TabTransferHandler extends TransferHandler {
	protected final DataFlavor localObjectFlavor = new DataFlavor(DnDTabData.class, "DnDTabData");
	protected DnDTabbedPane source;

	// protected TabTransferHandler() {
	//   super();
	//   //System.out.println("TabTransferHandler");
	//   localObjectFlavor = new ActivationDataFlavor(
	//       DnDTabbedPane.class, DataFlavor.javaJVMLocalObjectMimeType, "DnDTabbedPane");
	// }

	@Override protected Transferable createTransferable(JComponent c) {
		//System.out.println("createTransferable");
		if (c instanceof DnDTabbedPane) {
			source = (DnDTabbedPane) c;
		}
		// return new DataHandler(c, localObjectFlavor.getMimeType());
		return new Transferable() {
			@Override public DataFlavor[] getTransferDataFlavors() {
				return new DataFlavor[] {localObjectFlavor};
			}

			@Override public boolean isDataFlavorSupported(DataFlavor flavor) {
				return Objects.equals(localObjectFlavor, flavor);
			}

			@Override public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
				if (isDataFlavorSupported(flavor)) {
					return new DnDTabData(source);
				} else {
					throw new UnsupportedFlavorException(flavor);
				}
			}
		};
	}

	@Override public boolean canImport(TransferHandler.TransferSupport support) {
		// //System.out.println("canImport");
		if (!support.isDrop() || !support.isDataFlavorSupported(localObjectFlavor)) {
			//System.out.println("canImport:" + support.isDrop() + " " + support.isDataFlavorSupported(localObjectFlavor));
			return false;
		}
		support.setDropAction(TransferHandler.MOVE);
		TransferHandler.DropLocation tdl = support.getDropLocation();
		Point pt = tdl.getDropPoint();
		DnDTabbedPane target = (DnDTabbedPane) support.getComponent();
		target.autoScrollTest(pt);
		DnDTabbedPane.DropLocation dl = target.tabDropLocationForPoint(pt);
		int idx = dl.getIndex();

		// if (!isWebStart()) {
		//   // //System.out.println("local");
		//   try {
		//     source = (DnDTabbedPane) support.getTransferable().getTransferData(localObjectFlavor);
		//   } catch (Exception ex) {
		//     ex.printStackTrace();
		//   }
		// }

		boolean canDrop = false;
		boolean isAreaContains = target.getTabAreaBounds().contains(pt) && idx >= 0;
		if (target.equals(source)) {
			// //System.out.println("target == source");
			canDrop = isAreaContains && idx != target.dragTabIndex && idx != target.dragTabIndex + 1;
		} else {
			// System.out.format("target!=source%n target: %s%n source: %s", target.getName(), source.getName());
			canDrop = Optional.ofNullable(source).map(c -> !c.isAncestorOf(target)).orElse(false) && isAreaContains;
		}

		// [JDK-6700748] Cursor flickering during D&D when using CellRendererPane with validation - Java Bug System
		// https://bugs.openjdk.java.net/browse/JDK-6700748
		Cursor cursor = canDrop ? DragSource.DefaultMoveDrop : DragSource.DefaultMoveNoDrop;
		Component glassPane = target.getRootPane().getGlassPane();
		glassPane.setCursor(cursor);
		target.setCursor(cursor);

		support.setShowDropLocation(canDrop);
		target.updateTabDropLocation(dl, null, canDrop);
		return canDrop;
	}

	// private static boolean isWebStart() {
	//   try {
	//     ServiceManager.lookup("javax.jnlp.BasicService");
	//     return true;
	//   } catch (UnavailableServiceException ex) {
	//     return false;
	//   }
	// }

	private BufferedImage makeDragTabImage(DnDTabbedPane tabbedPane) {
		Rectangle rect = tabbedPane.getBoundsAt(tabbedPane.dragTabIndex);
		BufferedImage image = new BufferedImage(tabbedPane.getWidth(), tabbedPane.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics g = image.createGraphics();
		tabbedPane.paint(g);
		g.dispose();
		if (rect.x < 0) {
			rect.translate(-rect.x, 0);
		}
		if (rect.y < 0) {
			rect.translate(0, -rect.y);
		}
		if (rect.x + rect.width > image.getWidth()) {
			rect.width = image.getWidth() - rect.x;
		}
		if (rect.y + rect.height > image.getHeight()) {
			rect.height = image.getHeight() - rect.y;
		}
		return image.getSubimage(rect.x, rect.y, rect.width, rect.height);
	}

	@Override public int getSourceActions(JComponent c) {
		//System.out.println("getSourceActions");
		if (c instanceof DnDTabbedPane) {
			DnDTabbedPane src = (DnDTabbedPane) c;
			c.getRootPane().setGlassPane(new GhostGlassPane(src));
			if (src.dragTabIndex < 0) {
				return TransferHandler.NONE;
			}
			setDragImage(makeDragTabImage(src));
			c.getRootPane().getGlassPane().setVisible(true);
			return TransferHandler.MOVE;
		}
		return TransferHandler.NONE;
	}

	@Override public boolean importData(TransferHandler.TransferSupport support) {
		// //System.out.println("importData");
		DnDTabbedPane target = (DnDTabbedPane) support.getComponent();
		DnDTabbedPane.DropLocation dl = target.getDropLocation();
		try {
			DnDTabData data = (DnDTabData) support.getTransferable().getTransferData(localObjectFlavor);
			DnDTabbedPane src = data.tabbedPane;
			int index = dl.getIndex(); // boolean insert = dl.isInsert();
			if (target.equals(src)) {
				src.convertTab(src.dragTabIndex, index); // getTargetTabIndex(e.getLocation()));
			} else {
				src.exportTab(src.dragTabIndex, target, index);
			}
			return true;
		} catch (UnsupportedFlavorException | IOException ex) {
			return false;
		}
	}

	@Override protected void exportDone(JComponent c, Transferable data, int action) {
		//System.out.println("exportDone");
		DnDTabbedPane src = (DnDTabbedPane) c;
		src.getRootPane().getGlassPane().setVisible(false);
		src.updateTabDropLocation(null, null, false);
		src.repaint();
		src.setCursor(Cursor.getDefaultCursor());
	}
}

class GhostGlassPane extends JComponent {
	private DnDTabbedPane tabbedPane;

	protected GhostGlassPane(DnDTabbedPane tabbedPane) {
		super();
		this.tabbedPane = tabbedPane;
		setOpaque(false);
	}

	public void setTargetTabbedPane(DnDTabbedPane tab) {
		tabbedPane = tab;
	}

	@Override protected void paintComponent(Graphics g) {
		tabbedPane.getDropLineRect().ifPresent(rect -> {
			Graphics2D g2 = (Graphics2D) g.create();
			Rectangle r = SwingUtilities.convertRectangle(tabbedPane, rect, this);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f));
			g2.setPaint(Color.RED);
			g2.fill(r);
			g2.dispose();
		});
	}
}
