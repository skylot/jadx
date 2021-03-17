package jadx.gui.device.debugger;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.*;

import io.reactivex.annotations.Nullable;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.utils.StringUtils;
import jadx.gui.device.debugger.SmaliDebugger.RuntimeField;
import jadx.gui.device.debugger.SmaliDebugger.RuntimeRegister;
import jadx.gui.device.debugger.SmaliDebugger.RuntimeValue;
import jadx.gui.device.debugger.smali.Smali;
import jadx.gui.device.debugger.smali.Smali.SmaliRegister;
import jadx.gui.treemodel.JClass;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.SmaliArea;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class JDebuggerPanel extends JPanel {
	private static final long serialVersionUID = -1111111202102181631L;

	private static final ImageIcon ICON_RUN = UiUtils.openIcon("run");
	private static final ImageIcon ICON_RERUN = UiUtils.openIcon("rerun");
	private static final ImageIcon ICON_PAUSE = UiUtils.openIcon("pause");
	private static final ImageIcon ICON_STOP = UiUtils.openIcon("stop");
	private static final ImageIcon ICON_STOP_GRAY = UiUtils.openIcon("stop_gray");
	private static final ImageIcon ICON_STEP_INTO = UiUtils.openIcon("step_into");
	private static final ImageIcon ICON_STEP_OVER = UiUtils.openIcon("step_over");
	private static final ImageIcon ICON_STEP_OUT = UiUtils.openIcon("step_out");

	private final transient MainWindow mainWindow;
	private final transient JList<StackFrameElement> stackFrameList;
	private final transient JComboBox<ThreadBoxElement> threadBox;
	private final transient JTextArea logger;
	private final transient JTree variableTree;
	private final transient DefaultTreeModel variableTreeModel;
	private final transient DefaultMutableTreeNode rootTreeNode;
	private final transient DefaultMutableTreeNode thisTreeNode;
	private final transient DefaultMutableTreeNode watchTreeNode;
	private final transient DefaultMutableTreeNode regTreeNode;

	private final transient JSplitPane rightSplitter;
	private final transient JSplitPane leftSplitter;
	private final transient DebugController controller;

	private final transient VarTreePopupMenu varTreeMenu;
	private transient KeyEventDispatcher controllerShortCutDispatcher;

	public JDebuggerPanel(MainWindow mainWindow) {
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
		watchTreeNode = new DefaultMutableTreeNode("watch");
		thisTreeNode = new DefaultMutableTreeNode("this");
		regTreeNode = new DefaultMutableTreeNode("var");
		rootTreeNode.add(watchTreeNode);
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
					if (sel) {
						setForeground(Color.WHITE);
					} else if (((ValueTreeNode) value).isUpdated()) {
						setForeground(Color.RED);
					} else {
						setForeground(Color.BLACK);
					}
				}
				return c;
			}
		});

		varTreeMenu = new VarTreePopupMenu(mainWindow);

		JPanel loggerPanel = new JPanel(new CardLayout());
		logger = new JTextArea();
		logger.setEditable(false);
		logger.setLineWrap(true);
		loggerPanel.add(new JScrollPane(logger));

		leftSplitter.setLeftComponent(stackFramePanel);
		leftSplitter.setRightComponent(rightSplitter);
		leftSplitter.setResizeWeight(MainWindow.SPLIT_PANE_RESIZE_WEIGHT);

		rightSplitter.setLeftComponent(variablePanel);
		rightSplitter.setRightComponent(loggerPanel);
		rightSplitter.setResizeWeight(MainWindow.SPLIT_PANE_RESIZE_WEIGHT);

		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.add(new Label(), BorderLayout.WEST);
		headerPanel.add(initToolBar(), BorderLayout.CENTER);
		JButton closeBtn = new JButton(UiUtils.openIcon("cross"));
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

		controller.setDebuggerStateListener(new DebugController.StateListener() {
			boolean isGray = true;

			@Override
			public void onStateChanged(boolean suspended, boolean stopped) {
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
			}
		});

		JToolBar toolBar = new JToolBar();
		toolBar.add(new Label());
		toolBar.add(Box.createHorizontalGlue());
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
			StackFrameElement ele = stackFrameList.getModel().getElementAt(loc);
			if (ele != null && ele.clsSig != null) {
				JClass cls = DbgUtils.getTopClassBySig(ele.clsSig, mainWindow);
				if (cls != null) {
					Smali smali = DbgUtils.getSmali(cls.getCls().getClassNode());
					if (smali != null) {
						int pos = smali.getInsnPosByCodeOffset(
								DbgUtils.classSigToRawFullName(ele.clsSig) + "." + ele.mthSig,
								ele.getCodeOffset());
						mainWindow.getTabbedPane().smaliJump(cls, Math.max(0, pos), false);
						return;
					}
				}
				log("Can't open smali panel for " + ele.clsSig + "->" + ele.mthSig);
			}
		}
	}

	public boolean showDebugger(String procName, String host, int port) {
		boolean ok = controller.setDebugger(this, host, port);
		if (ok) {
			log(String.format("Attached %s %s:%d", procName, host, port));
			leftSplitter.setDividerLocation(mainWindow.getSettings().getDebuggerStackFrameSplitterLoc());
			rightSplitter.setDividerLocation(mainWindow.getSettings().getDebuggerVarTreeSplitterLoc());
			mainWindow.showDebuggerPanel();
		}
		return ok;
	}

	public DebugController getDbgController() {
		return controller;
	}

	public JDebuggerPanel setLeftSplitterLocation(int loc) {
		leftSplitter.setDividerLocation(loc);
		return this;
	}

	public JDebuggerPanel setRightSplitterLocation(int loc) {
		rightSplitter.setDividerLocation(loc);
		return this;
	}

	public int getLeftSplitterLocation() {
		return leftSplitter.getDividerLocation();
	}

	public int getRightSplitterLocation() {
		return rightSplitter.getDividerLocation();
	}

	public void loadSettings() {
		Font font = mainWindow.getSettings().getFont();
		variableTree.setFont(font.deriveFont(font.getSize() + 1.f));
		variableTree.setRowHeight(-1);
		stackFrameList.setFont(font);
		threadBox.setFont(font);
		logger.setFont(font);
	}

	public void resetUI() {
		thisTreeNode.removeAllChildren();
		watchTreeNode.removeAllChildren();
		regTreeNode.removeAllChildren();

		clearFrameAndThreadList();

		threadBox.updateUI();
		stackFrameList.updateUI();
		variableTreeModel.reload(rootTreeNode);
		variableTree.expandPath(new TreePath(rootTreeNode.getPath()));
		logger.setText("");
	}

	protected void scrollToSmaliLine(JClass cls, int pos, boolean debugMode) {
		SwingUtilities.invokeLater(() -> getMainWindow().getTabbedPane().smaliJump(cls, pos, debugMode));
	}

	protected void resetAllDebuggingInfo() {
		clearFrameAndThreadList();
		resetAllRegAndFieldNodes();
	}

	protected void resetAllRegAndFieldNodes() {
		thisTreeNode.removeAllChildren();
		SwingUtilities.invokeLater(() -> variableTreeModel.reload(thisTreeNode));
		resetAllRegNodes();
	}

	protected void resetAllRegNodes() {
		regTreeNode.removeAllChildren();
		SwingUtilities.invokeLater(() -> variableTreeModel.reload(regTreeNode));
	}

	protected List<FieldTreeNode> getThisFieldNodes() {
		if (thisTreeNode.getChildCount() > 0) {
			List<FieldTreeNode> flds = new ArrayList<>(thisTreeNode.getChildCount());
			for (int i = 0; i < thisTreeNode.getChildCount(); i++) {
				flds.add((FieldTreeNode) thisTreeNode.getChildAt(i));
			}
			return flds;
		}
		return Collections.emptyList();
	}

	protected void updateRegTreeNodes(List<RegTreeNode> nodes) {
		nodes.forEach(regTreeNode::add);
	}

	protected void refreshThreadBox(List<ThreadBoxElement> elements) {
		if (elements.size() > 0) {
			DefaultComboBoxModel<ThreadBoxElement> model =
					(DefaultComboBoxModel<ThreadBoxElement>) threadBox.getModel();
			elements.forEach(model::addElement);
		}
		SwingUtilities.invokeLater(() -> {
			threadBox.updateUI();
			stackFrameList.setFont(mainWindow.getSettings().getFont());
		});
	}

	protected void refreshStackFrameList(List<StackFrameElement> elements) {
		if (elements.size() > 0) {
			DefaultListModel<StackFrameElement> model =
					(DefaultListModel<StackFrameElement>) stackFrameList.getModel();
			elements.forEach(model::addElement);
			stackFrameList.setFont(mainWindow.getSettings().getFont());
		}
		SwingUtilities.invokeLater(stackFrameList::repaint);
	}

	protected DefaultListModel<StackFrameElement> getStackFrameModel() {
		return (DefaultListModel<StackFrameElement>) stackFrameList.getModel();
	}

	protected void refreshRegisterTree() {
		SwingUtilities.invokeLater(() -> {
			variableTreeModel.reload(regTreeNode);
			variableTree.expandPath(new TreePath(regTreeNode.getPath()));
		});
	}

	protected void refreshThisFieldTree() {
		SwingUtilities.invokeLater(() -> {
			boolean expanded = variableTree.isExpanded(new TreePath(thisTreeNode.getPath()));
			variableTreeModel.reload(thisTreeNode);
			if (expanded) {
				variableTree.expandPath(new TreePath(regTreeNode.getPath()));
			}
		});
	}

	private void clearFrameAndThreadList() {
		((DefaultListModel<StackFrameElement>) stackFrameList.getModel()).removeAllElements();
		((DefaultComboBoxModel<ThreadBoxElement>) threadBox.getModel()).removeAllElements();
	}

	public void log(String msg) {
		StringBuilder sb = new StringBuilder();
		sb.append(" > ")
				.append(DbgUtils.getDateText())
				.append(" ")
				.append(msg)
				.append("\n");
		SwingUtilities.invokeLater(() -> {
			logger.append(sb.toString());
		});
	}

	protected void updateRegTree(RegTreeNode node) {
		SwingUtilities.invokeLater(() -> {
			variableTreeModel.reload(regTreeNode);
			scrollToUpdatedNode(node);
		});
	}

	protected void setThisFieldNodes(List<FieldTreeNode> nodes) {
		nodes.forEach(thisTreeNode::add);
		SwingUtilities.invokeLater(() -> variableTreeModel.reload(thisTreeNode));
	}

	protected void scrollToUpdatedNode(ValueTreeNode node) {
		SwingUtilities.invokeLater(() -> {
			TreeNode[] path = node.getPath();
			variableTree.scrollPathToVisible(new TreePath(path));
		});
	}

	protected FieldTreeNode buildFieldNode(RuntimeField rf) {
		return new FieldTreeNode(rf);
	}

	protected RegTreeNode buildRegNode(RuntimeRegister rr, SmaliRegister sr, long tid, long fid) {
		return new RegTreeNode(rr, sr, tid, fid);
	}

	public abstract static class ValueTreeNode extends DefaultMutableTreeNode {
		private static final long serialVersionUID = -1111111202103122236L;

		protected boolean updated;

		public void setUpdated(boolean updated) {
			this.updated = updated;
		}

		public boolean isUpdated() {
			return updated;
		}

		abstract String getName();

		abstract String getValue();

		abstract String getType();

		abstract RuntimeValue getRuntimeValue();

		abstract void updateValue(String val);

		abstract void updateType(String val);

		@Override
		public String toString() {
			String val = getValue();
			if (val != null) {
				return String.format("%s val: %s, type: %s", getName(), val, getType());
			} else {
				return String.format("%s type: %s", getName(), getType());
			}
		}
	}

	public class RegTreeNode extends ValueTreeNode {
		private static final long serialVersionUID = -1111111202103122234L;

		private RuntimeRegister runtimeReg;
		private SmaliRegister smaliReg;
		private final long threadID;
		private final long frameID;
		private String value;
		private String type;

		public RegTreeNode(RuntimeRegister register, SmaliRegister smaliReg, long threadID, long frameID) {
			this.runtimeReg = register;
			this.smaliReg = smaliReg;
			this.threadID = threadID;
			this.frameID = frameID;
		}

		public RuntimeRegister getRuntimeReg() {
			return runtimeReg;
		}

		public void updateReg(RuntimeRegister reg) {
			runtimeReg = reg;
		}

		@Override
		public void updateValue(String value) {
			updated = true;
			this.value = value;
			this.removeAllChildren();
		}

		@Override
		public void updateType(String type) {
			if (this.type == null || !this.type.equals(type)) {
				this.type = type;
				value = null;
				this.removeAllChildren();
				updated = true;
			}
		}

		@Override
		String getName() {
			String alias = smaliReg.getRegName(controller.getCodeOffset());
			if (!StringUtils.isEmpty(alias)) {
				return String.format("v%s (%s) ", getRegNum(), alias);
			}
			return String.format("v%-3s", getRegNum());
		}

		@Override
		@Nullable
		public String getValue() {
			return value;
		}

		public int getRuntimeRegNum() {
			return runtimeReg.getRegNum();
		}

		public int getRegNum() {
			return smaliReg.getRegNum();
		}

		@Override
		public String getType() {
			if (type != null) {
				return type;
			}
			return runtimeReg.getType().getDesc();
		}

		@Override
		RuntimeValue getRuntimeValue() {
			return getRuntimeReg();
		}

		public long getFrameID() {
			return frameID;
		}

		public long getThreadID() {
			return threadID;
		}
	}

	protected static class FieldTreeNode extends ValueTreeNode {
		private static final long serialVersionUID = -1111111202103122235L;

		private final RuntimeField field;
		private String value;
		private String alias;

		private FieldTreeNode(RuntimeField field) {
			this.field = field;
		}

		public RuntimeField getRuntimeField() {
			return this.field;
		}

		public void setAlias(String alias) {
			this.alias = alias;
		}

		@Override
		void updateValue(String val) {
			this.updated = true;
			value = val;
			this.removeAllChildren();
		}

		@Override
		void updateType(String val) {
		}

		@Override
		String getName() {
			if (StringUtils.isEmpty(alias) || alias.equals(field.getName())) {
				return field.getName();
			}
			return field.getName() + " (" + alias + ")";
		}

		@Override
		String getValue() {
			return value;
		}

		@Override
		String getType() {
			return ArgType.parse(field.getFieldType()).toString();
		}

		@Override
		RuntimeValue getRuntimeValue() {
			return field;
		}
	}

	public static class ThreadBoxElement {
		private long threadID;
		private String name;

		public ThreadBoxElement(long threadID) {
			this.threadID = threadID;
		}

		public void setName(String name) {
			this.name = name;
		}

		public long getThreadID() {
			return threadID;
		}

		@Override
		public String toString() {
			if (name == null) {
				return "thread id: " + threadID;
			}
			return "thread id: " + threadID + " name:" + name;
		}
	}

	public static class StackFrameElement {
		private final SmaliDebugger.Frame frame;
		private String clsSig;
		private String mthSig;
		private StringBuilder cache;
		private long codeOffset = -1;
		private List<RegTreeNode> regNodes;

		public StackFrameElement(SmaliDebugger.Frame frame) {
			cache = new StringBuilder(16);
			this.frame = frame;
			regNodes = Collections.emptyList();
		}

		public SmaliDebugger.Frame getFrame() {
			return frame;
		}

		public void setSignatures(String clsSig, String mthSig) {
			this.clsSig = clsSig;
			this.mthSig = mthSig;
			this.cache.delete(0, this.cache.length());
		}

		public String getClsSig() {
			return clsSig;
		}

		public String getMthSig() {
			return mthSig;
		}

		public void updateCodeOffset(long codeOffset) {
			this.codeOffset = codeOffset;
			if (this.codeOffset > -1) {
				this.cache.delete(0, this.cache.length());
			}
		}

		public long getCodeOffset() {
			return codeOffset == -1 ? frame.getCodeIndex() : codeOffset;
		}

		public void setRegNodes(List<RegTreeNode> regNodes) {
			this.regNodes = regNodes;
		}

		public List<RegTreeNode> getRegNodes() {
			return regNodes;
		}

		@Override
		public String toString() {
			if (cache.length() == 0) {
				long off = getCodeOffset();
				if (off < 0) {
					cache.append(String.format("index: %-4d ", off));
				} else {
					cache.append(String.format("index: %04x ", off));
				}
				if (clsSig == null) {
					cache.append("clsID: ").append(frame.getClassID());
				} else {
					cache.append(clsSig).append("->");
				}
				if (mthSig == null) {
					cache.append(" mthID: ").append(frame.getMethodID());
				} else {
					cache.append(mthSig);
				}
			}
			return cache.toString();
		}
	}
}
