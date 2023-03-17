package jadx.gui.ui.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Label;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.annotations.Nullable;

import jadx.core.utils.StringUtils;
import jadx.gui.device.debugger.DebugController;
import jadx.gui.device.protocol.ADBDevice;
import jadx.gui.treemodel.JClass;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.SmaliArea;
import jadx.gui.ui.dialog.ADBDialog;
import jadx.gui.ui.popupmenu.VarTreePopupMenu;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class JDebuggerPanel extends JPanel {
	private static final long serialVersionUID = -1111111202102181631L;
	private static final Logger LOG = LoggerFactory.getLogger(LogcatPanel.class);

	private static final ImageIcon ICON_RUN = UiUtils.openSvgIcon("debugger/execute");
	private static final ImageIcon ICON_RERUN = UiUtils.openSvgIcon("debugger/rerun");
	private static final ImageIcon ICON_PAUSE = UiUtils.openSvgIcon("debugger/threadFrozen");
	private static final ImageIcon ICON_STOP = UiUtils.openSvgIcon("debugger/suspend");
	private static final ImageIcon ICON_STOP_GRAY = UiUtils.openSvgIcon("debugger/suspendGray");
	private static final ImageIcon ICON_STEP_INTO = UiUtils.openSvgIcon("debugger/traceInto");
	private static final ImageIcon ICON_STEP_OVER = UiUtils.openSvgIcon("debugger/traceOver");
	private static final ImageIcon ICON_STEP_OUT = UiUtils.openSvgIcon("debugger/stepOut");

	private final transient MainWindow mainWindow;
	private final transient JList<IListElement> stackFrameList;
	private final transient JComboBox<IListElement> threadBox;
	private final transient JTextArea logger;
	private final transient JTree variableTree;
	private final transient DefaultTreeModel variableTreeModel;
	private final transient DefaultMutableTreeNode rootTreeNode;
	private final transient DefaultMutableTreeNode thisTreeNode;
	private final transient DefaultMutableTreeNode regTreeNode;

	private final transient JSplitPane rightSplitter;
	private final transient JSplitPane leftSplitter;
	private final transient IDebugController controller;
	private final LogcatPanel logcatPanel;

	private final transient VarTreePopupMenu varTreeMenu;
	private transient KeyEventDispatcher controllerShortCutDispatcher;

	public JDebuggerPanel(MainWindow mainWindow) {
		UiUtils.uiThreadGuard();
		this.mainWindow = mainWindow;
		controller = new DebugController();
		this.setLayout(new BorderLayout());
		this.setMinimumSize(new Dimension(100, 150));

		leftSplitter = new JSplitPane();
		rightSplitter = new JSplitPane();

		leftSplitter.setDividerLocation(mainWindow.getSettings().getDebuggerStackFrameSplitterLoc());
		rightSplitter.setDividerLocation(mainWindow.getSettings().getDebuggerVarTreeSplitterLoc());

		JPanel stackFramePanel = new JPanel(new BorderLayout());
		threadBox = new JComboBox<>();
		stackFrameList = new JList<>();
		threadBox.setModel(new DefaultComboBoxModel<>());
		stackFrameList.setModel(new DefaultListModel<>());

		stackFramePanel.add(threadBox, BorderLayout.NORTH);
		stackFramePanel.add(new JScrollPane(stackFrameList), BorderLayout.CENTER);

		JPanel variablePanel = new JPanel(new CardLayout());
		variableTree = new JTree();
		variablePanel.add(new JScrollPane(variableTree));

		rootTreeNode = new DefaultMutableTreeNode();
		thisTreeNode = new DefaultMutableTreeNode("this");
		regTreeNode = new DefaultMutableTreeNode("var");
		rootTreeNode.add(thisTreeNode);
		rootTreeNode.add(regTreeNode);
		variableTreeModel = new DefaultTreeModel(rootTreeNode);
		variableTree.setModel(variableTreeModel);
		variableTree.expandPath(new TreePath(rootTreeNode.getPath()));
		variableTree.setCellRenderer(new DefaultTreeCellRenderer() {
			private static final long serialVersionUID = -1111111202103170725L;

			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row,
					boolean hasFocus) {
				Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
				if (value instanceof ValueTreeNode) {
					if (((ValueTreeNode) value).isUpdated()) {
						setForeground(Color.RED);
					}
				}
				return c;
			}
		});

		varTreeMenu = new VarTreePopupMenu(mainWindow);

		JTabbedPane loggerPanel = new JTabbedPane();
		logger = new JTextArea();
		logger.setEditable(false);
		logger.setLineWrap(true);
		JScrollPane loggerScroll = new JScrollPane(logger);
		loggerPanel.addTab("Debugger Log", null, loggerScroll, null);
		this.logcatPanel = new LogcatPanel(this);
		loggerPanel.addTab(NLS.str("logcat.logcat"), null, logcatPanel, null);

		leftSplitter.setLeftComponent(stackFramePanel);
		leftSplitter.setRightComponent(rightSplitter);
		leftSplitter.setResizeWeight(MainWindow.SPLIT_PANE_RESIZE_WEIGHT);

		rightSplitter.setLeftComponent(variablePanel);
		rightSplitter.setRightComponent(loggerPanel);
		rightSplitter.setResizeWeight(MainWindow.SPLIT_PANE_RESIZE_WEIGHT);

		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.add(new Label(), BorderLayout.WEST);
		headerPanel.add(initToolBar(), BorderLayout.CENTER);
		JButton closeBtn = new JButton(UiUtils.openSvgIcon("ui/close"));
		closeBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (controller.isDebugging()) {
					int what = JOptionPane.showConfirmDialog(mainWindow,
							NLS.str("debugger.cfm_dialog_msg"),
							NLS.str("debugger.cfm_dialog_title"),
							JOptionPane.OK_CANCEL_OPTION);
					if (what == JOptionPane.OK_OPTION) {
						controller.exit();
						logcatPanel.exit();
					} else {
						return;
					}
				} else {
					mainWindow.destroyDebuggerPanel();
				}
				unregShortcuts();
			}
		});
		headerPanel.add(closeBtn, BorderLayout.EAST);

		this.add(headerPanel, BorderLayout.NORTH);
		this.add(leftSplitter, BorderLayout.CENTER);
		listenUIEvents();
	}

	public MainWindow getMainWindow() {
		return mainWindow;
	}

	private void listenUIEvents() {
		stackFrameList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() % 2 == 0) {
					stackFrameSelected(e.getPoint());
				}
			}
		});
		variableTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					treeNodeRightClicked(e);
				}
			}
		});
	}

	private JToolBar initToolBar() {
		AbstractAction stepOver = new AbstractAction(NLS.str("debugger.step_over"), ICON_STEP_OVER) {
			private static final long serialVersionUID = -1111111202103170726L;

			@Override
			public void actionPerformed(ActionEvent e) {
				controller.stepOver();
			}
		};
		stepOver.putValue(Action.SHORT_DESCRIPTION, NLS.str("debugger.step_over"));

		AbstractAction stepInto = new AbstractAction(NLS.str("debugger.step_into"), ICON_STEP_INTO) {
			private static final long serialVersionUID = -1111111202103170727L;

			@Override
			public void actionPerformed(ActionEvent e) {
				controller.stepInto();
			}
		};
		stepInto.putValue(Action.SHORT_DESCRIPTION, NLS.str("debugger.step_into"));

		AbstractAction stepOut = new AbstractAction(NLS.str("debugger.step_out"), ICON_STEP_OUT) {
			private static final long serialVersionUID = -1111111202103170728L;

			@Override
			public void actionPerformed(ActionEvent e) {
				controller.stepOut();
			}
		};
		stepOut.putValue(Action.SHORT_DESCRIPTION, NLS.str("debugger.step_out"));

		AbstractAction stop = new AbstractAction(NLS.str("debugger.stop"), ICON_STOP_GRAY) {
			private static final long serialVersionUID = -1111111202103170728L;

			@Override
			public void actionPerformed(ActionEvent e) {
				controller.stop();
			}
		};
		stop.putValue(Action.SHORT_DESCRIPTION, NLS.str("debugger.stop"));

		AbstractAction run = new AbstractAction(NLS.str("debugger.run"), ICON_RUN) {
			private static final long serialVersionUID = -1111111202103170728L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (controller.isDebugging()) {
					if (controller.isSuspended()) {
						controller.run();
					} else {
						controller.pause();
					}
				}
			}
		};
		run.putValue(Action.SHORT_DESCRIPTION, NLS.str("debugger.run"));

		AbstractAction rerun = new AbstractAction(NLS.str("debugger.rerun"), ICON_RERUN) {
			private static final long serialVersionUID = -1111111202103210433L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (controller.isDebugging()) {
					controller.stop();
				}
				String pkgName = controller.getProcessName();
				if (pkgName.isEmpty() || !ADBDialog.launchForDebugging(mainWindow, pkgName, true)) {
					(new ADBDialog(mainWindow)).setVisible(true);
				}
			}
		};
		rerun.putValue(Action.SHORT_DESCRIPTION, NLS.str("debugger.rerun"));

		controller.setStateListener(new DebugController.StateListener() {
			boolean isGray = true;

			@Override
			public void onStateChanged(boolean suspended, boolean stopped) {
				UiUtils.uiRun(() -> {
					if (!stopped) {
						if (isGray) {
							stop.putValue(Action.SMALL_ICON, ICON_STOP);
						}
					} else {
						stop.putValue(Action.SMALL_ICON, ICON_STOP_GRAY);
						run.putValue(Action.SMALL_ICON, ICON_RUN);
						run.putValue(Action.SHORT_DESCRIPTION, NLS.str("debugger.run"));
						isGray = true;
						return;
					}
					if (suspended) {
						run.putValue(Action.SMALL_ICON, ICON_RUN);
						run.putValue(Action.SHORT_DESCRIPTION, NLS.str("debugger.run"));
					} else {
						run.putValue(Action.SMALL_ICON, ICON_PAUSE);
						run.putValue(Action.SHORT_DESCRIPTION, NLS.str("debugger.pause"));
					}
				});
			}
		});

		JToolBar toolBar = new JToolBar();
		toolBar.add(new Label());
		toolBar.add(Box.createHorizontalGlue());
		toolBar.add(rerun);
		toolBar.add(Box.createRigidArea(new Dimension(5, 0)));
		toolBar.add(stop);
		toolBar.add(Box.createRigidArea(new Dimension(5, 0)));
		toolBar.add(run);
		toolBar.add(Box.createRigidArea(new Dimension(5, 0)));
		toolBar.add(stepOver);
		toolBar.add(Box.createRigidArea(new Dimension(5, 0)));
		toolBar.add(stepInto);
		toolBar.add(Box.createRigidArea(new Dimension(5, 0)));
		toolBar.add(stepOut);
		toolBar.add(Box.createHorizontalGlue());
		toolBar.add(new Label());
		regShortcuts();
		return toolBar;
	}

	private void unregShortcuts() {
		KeyboardFocusManager
				.getCurrentKeyboardFocusManager()
				.removeKeyEventDispatcher(controllerShortCutDispatcher);
	}

	private void regShortcuts() {
		controllerShortCutDispatcher = new KeyEventDispatcher() {
			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {
				if (e.getID() == KeyEvent.KEY_PRESSED
						&& mainWindow.getTabbedPane().getFocusedComp() instanceof SmaliArea) {
					if (e.getModifiersEx() == KeyEvent.SHIFT_DOWN_MASK
							&& e.getKeyCode() == KeyEvent.VK_F8) {
						controller.stepOut();
						return true;
					}
					switch (e.getKeyCode()) {
						case KeyEvent.VK_F7:
							controller.stepInto();
							return true;
						case KeyEvent.VK_F8:
							controller.stepOver();
							return true;
						case KeyEvent.VK_F9:
							controller.run();
							return true;
					}
				}
				return false;
			}
		};
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
				.addKeyEventDispatcher(controllerShortCutDispatcher);
	}

	private void treeNodeRightClicked(MouseEvent e) {
		TreePath path = variableTree.getPathForLocation(e.getX(), e.getY());
		if (path != null) {
			Object node = path.getLastPathComponent();
			if (node instanceof ValueTreeNode) {
				varTreeMenu.show((ValueTreeNode) node, e.getComponent(), e.getX(), e.getY());
			}
		}
	}

	private void stackFrameSelected(Point p) {
		int loc = stackFrameList.locationToIndex(p);
		if (loc > -1) {
			IListElement ele = stackFrameList.getModel().getElementAt(loc);
			if (ele != null) {
				ele.onSelected();
			}
		}
	}

	public boolean showDebugger(String procName, String host, int port, int androidVer, ADBDevice device, String pid) {
		boolean ok = controller.startDebugger(this, host, port, androidVer);
		if (ok) {
			UiUtils.uiRun(() -> {
				log(String.format("Attached %s %s:%d", procName, host, port));
				try {
					logcatPanel.init(device, pid);
				} catch (Exception e) {
					log(NLS.str("logcat.error_fail_start"));
					LOG.error("Logcat failed to start", e);
				}
				leftSplitter.setDividerLocation(mainWindow.getSettings().getDebuggerStackFrameSplitterLoc());
				rightSplitter.setDividerLocation(mainWindow.getSettings().getDebuggerVarTreeSplitterLoc());
				mainWindow.showDebuggerPanel();
			});
		}
		return ok;
	}

	public IDebugController getDbgController() {
		return controller;
	}

	public int getLeftSplitterLocation() {
		return leftSplitter.getDividerLocation();
	}

	public int getRightSplitterLocation() {
		return rightSplitter.getDividerLocation();
	}

	public void loadSettings() {
		UiUtils.uiThreadGuard();

		Font font = mainWindow.getSettings().getFont();
		variableTree.setFont(font.deriveFont(font.getSize() + 1.f));
		variableTree.setRowHeight(-1);
		stackFrameList.setFont(font);
		threadBox.setFont(font);
		logger.setFont(font);
	}

	public void resetUI() {
		UiUtils.uiThreadGuard();

		thisTreeNode.removeAllChildren();
		regTreeNode.removeAllChildren();

		clearFrameAndThreadList();

		threadBox.updateUI();
		stackFrameList.updateUI();
		variableTreeModel.reload(rootTreeNode);
		variableTree.expandPath(new TreePath(rootTreeNode.getPath()));
		logger.setText("");
	}

	public void scrollToSmaliLine(JClass cls, int pos, boolean debugMode) {
		SwingUtilities.invokeLater(() -> getMainWindow().getTabbedPane().smaliJump(cls, pos, debugMode));
	}

	public void resetAllDebuggingInfo() {
		clearFrameAndThreadList();
		resetRegTreeNodes();
		resetThisTreeNodes();
	}

	public void resetThisTreeNodes() {
		thisTreeNode.removeAllChildren();
		SwingUtilities.invokeLater(() -> variableTreeModel.reload(thisTreeNode));
	}

	public void resetRegTreeNodes() {
		regTreeNode.removeAllChildren();
		SwingUtilities.invokeLater(() -> variableTreeModel.reload(regTreeNode));
	}

	public void updateRegTreeNodes(List<? extends ValueTreeNode> nodes) {
		nodes.forEach(regTreeNode::add);
	}

	public void updateThisFieldNodes(List<? extends ValueTreeNode> nodes) {
		nodes.forEach(thisTreeNode::add);
	}

	public void refreshThreadBox(List<? extends IListElement> elements) {
		UiUtils.uiRun(() -> {
			if (!elements.isEmpty()) {
				DefaultComboBoxModel<IListElement> model = (DefaultComboBoxModel<IListElement>) threadBox.getModel();
				elements.forEach(model::addElement);
			}
			threadBox.updateUI();
			stackFrameList.setFont(mainWindow.getSettings().getFont());
		});
	}

	public void refreshStackFrameList(List<? extends IListElement> elements) {
		if (elements.size() > 0) {
			DefaultListModel<IListElement> model =
					(DefaultListModel<IListElement>) stackFrameList.getModel();
			elements.forEach(model::addElement);
			stackFrameList.setFont(mainWindow.getSettings().getFont());
		}
		SwingUtilities.invokeLater(stackFrameList::repaint);
	}

	public void refreshRegisterTree() {
		SwingUtilities.invokeLater(() -> {
			variableTreeModel.reload(regTreeNode);
			variableTree.expandPath(new TreePath(regTreeNode.getPath()));
		});
	}

	public void refreshThisFieldTree() {
		SwingUtilities.invokeLater(() -> {
			boolean expanded = variableTree.isExpanded(new TreePath(thisTreeNode.getPath()));
			variableTreeModel.reload(thisTreeNode);
			if (expanded) {
				variableTree.expandPath(new TreePath(regTreeNode.getPath()));
			}
		});
	}

	public void clearFrameAndThreadList() {
		((DefaultListModel<IListElement>) stackFrameList.getModel()).removeAllElements();
		((DefaultComboBoxModel<IListElement>) threadBox.getModel()).removeAllElements();
	}

	public void log(String msg) {
		StringBuilder sb = new StringBuilder();
		sb.append(" > ")
				.append(StringUtils.getDateText())
				.append(" ")
				.append(msg)
				.append("\n");
		SwingUtilities.invokeLater(() -> {
			logger.append(sb.toString());
		});
	}

	public void updateRegTree(ValueTreeNode node) {
		SwingUtilities.invokeLater(() -> {
			variableTreeModel.reload(regTreeNode);
			scrollToUpdatedNode(node);
		});
	}

	public void updateThisTree(ValueTreeNode node) {
		SwingUtilities.invokeLater(() -> {
			variableTreeModel.reload(thisTreeNode);
			scrollToUpdatedNode(node);
		});
	}

	public void scrollToUpdatedNode(ValueTreeNode node) {
		SwingUtilities.invokeLater(() -> {
			TreeNode[] path = node.getPath();
			variableTree.scrollPathToVisible(new TreePath(path));
		});
	}

	public abstract static class ValueTreeNode extends DefaultMutableTreeNode {
		private static final long serialVersionUID = -1111111202103122236L;

		private boolean updated;

		public void setUpdated(boolean updated) {
			this.updated = updated;
		}

		public boolean isUpdated() {
			return updated;
		}

		public abstract String getName();

		@Nullable
		public abstract String getValue();

		@Nullable
		public abstract String getType();

		public abstract long getTypeID();

		public abstract ValueTreeNode updateValue(String val);

		public abstract ValueTreeNode updateType(String val);

		public abstract ValueTreeNode updateTypeID(long id);

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(getName());
			String val = getValue();
			if (val != null) {
				sb.append(" val: ").append(val).append(",");
			}
			String type = getType();
			if (type != null) {
				sb.append(" type: ").append(getType());
				long id = getTypeID();
				if (id > 0) {
					sb.append("@").append(id);
				}
			}
			if (val == null && type == null) {
				sb.append(" undefined");
			}
			return sb.toString();
		}
	}

	public interface IListElement {
		void onSelected();
	}
}
