package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.StringUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.device.debugger.DbgUtils;
import jadx.gui.device.protocol.ADB;
import jadx.gui.device.protocol.ADBDevice;
import jadx.gui.device.protocol.ADBDeviceInfo;
import jadx.gui.treemodel.JClass;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.panel.IDebugController;
import jadx.gui.utils.NLS;
import jadx.gui.utils.SystemInfo;
import jadx.gui.utils.UiUtils;

import static jadx.gui.device.protocol.ADBDevice.ForwardResult;

public class ADBDialog extends JDialog implements ADB.DeviceStateListener, ADB.JDWPProcessListener {
	private static final Logger LOG = LoggerFactory.getLogger(ADBDialog.class);

	private static final long serialVersionUID = -1111111202102181630L;
	private static final ImageIcon ICON_DEVICE = UiUtils.openSvgIcon("adb/androidDevice");
	private static final ImageIcon ICON_PROCESS = UiUtils.openSvgIcon("adb/addToWatch");
	private static DebugSetting debugSetter = null;

	private final transient MainWindow mainWindow;
	private transient Label tipLabel;
	private transient JTextField pathTextField;
	private transient JTextField hostTextField;
	private transient JTextField portTextField;
	private transient DefaultTreeModel procTreeModel;
	private transient DefaultMutableTreeNode procTreeRoot;
	private transient JTree procTree;
	private Socket deviceSocket;
	private transient List<DeviceNode> deviceNodes = new ArrayList<>();

	public ADBDialog(MainWindow mainWindow) {
		super(mainWindow);
		this.mainWindow = mainWindow;
		if (debugSetter == null) {
			debugSetter = new DebugSetting();
		}
		initUI();
		pathTextField.setText(mainWindow.getSettings().getAdbDialogPath());
		hostTextField.setText(mainWindow.getSettings().getAdbDialogHost());
		portTextField.setText(mainWindow.getSettings().getAdbDialogPort());

		if (pathTextField.getText().equals("")) {
			detectADBPath();
		} else {
			pathTextField.setText("");
		}

		SwingUtilities.invokeLater(this::connectToADB);
		UiUtils.addEscapeShortCutToDispose(this);
	}

	private void initUI() {
		pathTextField = new JTextField();
		portTextField = new JTextField();
		hostTextField = new JTextField();

		JPanel adbPanel = new JPanel(new BorderLayout(5, 5));
		adbPanel.add(new JLabel(NLS.str("adb_dialog.path")), BorderLayout.WEST);
		adbPanel.add(pathTextField, BorderLayout.CENTER);

		JPanel portPanel = new JPanel(new BorderLayout(5, 0));
		portPanel.add(new JLabel(NLS.str("adb_dialog.port")), BorderLayout.WEST);
		portPanel.add(portTextField, BorderLayout.CENTER);

		JPanel hostPanel = new JPanel(new BorderLayout(5, 0));
		hostPanel.add(new JLabel(NLS.str("adb_dialog.addr")), BorderLayout.WEST);
		hostPanel.add(hostTextField, BorderLayout.CENTER);

		JPanel wrapperPanel = new JPanel(new GridLayout(1, 2, 5, 0));
		wrapperPanel.add(hostPanel);
		wrapperPanel.add(portPanel);
		adbPanel.add(wrapperPanel, BorderLayout.SOUTH);

		procTree = new JTree();
		JScrollPane scrollPane = new JScrollPane(procTree);
		scrollPane.setMinimumSize(new Dimension(100, 150));
		scrollPane.setBorder(BorderFactory.createLineBorder(Color.black));

		procTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		procTreeRoot = new DefaultMutableTreeNode(NLS.str("adb_dialog.device_node"));
		procTreeModel = new DefaultTreeModel(procTreeRoot);
		procTree.setModel(procTreeModel);
		procTree.setRowHeight(-1);
		Font font = mainWindow.getSettings().getFont();
		procTree.setFont(font.deriveFont(font.getSize() + 1.f));

		procTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					processSelected(e);
				}
			}
		});
		procTree.setCellRenderer(new DefaultTreeCellRenderer() {
			private static final long serialVersionUID = -1111111202103170735L;

			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf,
					int row, boolean hasFocus) {
				Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
				if (value instanceof DeviceTreeNode || value == procTreeRoot) {
					setIcon(ICON_DEVICE);
				} else {
					setIcon(ICON_PROCESS);
				}
				return c;
			}
		});

		JPanel btnPane = new JPanel();
		BoxLayout boxLayout = new BoxLayout(btnPane, BoxLayout.LINE_AXIS);
		btnPane.setLayout(boxLayout);
		tipLabel = new Label(NLS.str("adb_dialog.waiting"));
		btnPane.add(tipLabel);
		JButton refreshBtn = new JButton(NLS.str("adb_dialog.refresh"));
		JButton startServerBtn = new JButton(NLS.str("adb_dialog.start_server"));
		JButton launchAppBtn = new JButton(NLS.str("adb_dialog.launch_app"));
		btnPane.add(launchAppBtn);
		btnPane.add(startServerBtn);
		btnPane.add(refreshBtn);
		refreshBtn.addActionListener(e -> {
			clear();
			procTreeRoot.removeAllChildren();
			procTreeModel.reload(procTreeRoot);
			SwingUtilities.invokeLater(this::connectToADB);
		});

		startServerBtn.addActionListener(e -> startADBServer());
		launchAppBtn.addActionListener(e -> launchApp());

		JPanel mainPane = new JPanel(new BorderLayout(5, 5));
		mainPane.add(adbPanel, BorderLayout.NORTH);
		mainPane.add(scrollPane, BorderLayout.CENTER);
		mainPane.add(btnPane, BorderLayout.SOUTH);
		mainPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		getContentPane().add(mainPane);

		pack();
		setSize(800, 500);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.MODELESS);
	}

	private void clear() {
		if (deviceSocket != null) {
			try {
				deviceSocket.close();
			} catch (Exception e) {
				LOG.error("Failed to close device socket", e);
			}
			deviceSocket = null;
		}
		for (DeviceNode deviceNode : deviceNodes) {
			deviceNode.device.stopListenForJDWP();
		}
		deviceNodes.clear();
	}

	private void detectADBPath() {
		boolean isWinOS = SystemInfo.IS_WINDOWS;
		String slash = isWinOS ? "\\" : "/";
		String adbName = isWinOS ? "adb.exe" : "adb";
		String sdkPath = System.getenv("ANDROID_HOME");
		if (!StringUtils.isEmpty(sdkPath)) {
			if (!sdkPath.endsWith(slash)) {
				sdkPath += slash;
			}
			sdkPath += "platform-tools" + slash + adbName;
			if ((new File(sdkPath)).exists()) {
				pathTextField.setText(sdkPath);
				return;
			}
		}
		String envPath = System.getenv("PATH");
		String[] paths = envPath.split(isWinOS ? ";" : ":");
		for (String path : paths) {
			if (!path.endsWith(slash)) {
				path += slash;
			}
			path = path + adbName;
			if (new File(path).exists()) {
				pathTextField.setText(path);
				return;
			}
		}
	}

	private void startADBServer() {
		String path = pathTextField.getText();
		if (path.isEmpty()) {
			UiUtils.showMessageBox(mainWindow, NLS.str("adb_dialog.missing_path"));
			return;
		}
		String tip;
		try {
			if (ADB.startServer(path, Integer.parseInt(portTextField.getText()))) {
				tip = NLS.str("adb_dialog.start_okay", portTextField.getText());
			} else {
				tip = NLS.str("adb_dialog.start_fail", portTextField.getText());
			}
		} catch (Exception e) {
			LOG.error("Failed to start adb server", e);
			tip = e.getMessage();
		}
		UiUtils.showMessageBox(mainWindow, tip);
		tipLabel.setText(tip);
	}

	private void connectToADB() {
		String tip;
		try {
			String host = hostTextField.getText().trim();
			String port = portTextField.getText().trim();
			tipLabel.setText(NLS.str("adb_dialog.connecting", host, port));
			deviceSocket = ADB.listenForDeviceState(this, host, Integer.parseInt(port));
			if (deviceSocket != null) {
				tip = NLS.str("adb_dialog.connect_okay", host, port);
				this.setTitle(tip);
			} else {
				tip = NLS.str("adb_dialog.connect_fail");
			}
		} catch (Exception e) {
			LOG.error("Failed to connect to adb", e);
			tip = e.getMessage();
			UiUtils.showMessageBox(mainWindow, tip);
		}
		tipLabel.setText(tip);
	}

	@Override
	public void onDeviceStatusChange(List<ADBDeviceInfo> deviceInfoList) {
		List<DeviceNode> nodes = new ArrayList<>(deviceInfoList.size());
		info_loop: for (ADBDeviceInfo info : deviceInfoList) {
			for (DeviceNode deviceNode : deviceNodes) {
				if (deviceNode.device.updateDeviceInfo(info)) {
					deviceNode.refresh();
					nodes.add(deviceNode);
					continue info_loop;
				}
			}
			ADBDevice device = new ADBDevice(info);
			device.getAndroidReleaseVersion();
			nodes.add(new DeviceNode(device));
			listenJDWP(device);
		}
		deviceNodes = nodes;
		SwingUtilities.invokeLater(() -> {
			tipLabel.setText(NLS.str("adb_dialog.tip_devices", deviceNodes.size()));
			procTreeRoot.removeAllChildren();
			deviceNodes.forEach(n -> procTreeRoot.add(n.tNode));
			procTreeModel.reload(procTreeRoot);
			for (DeviceNode deviceNode : deviceNodes) {
				procTree.expandPath(new TreePath(deviceNode.tNode.getPath()));
			}
		});
	}

	private void processSelected(MouseEvent e) {
		TreePath path = procTree.getPathForLocation(e.getX(), e.getY());
		if (path != null) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
			String pid = getPid((String) node.getUserObject());
			if (StringUtils.isEmpty(pid)) {
				return;
			}
			if (mainWindow.getDebuggerPanel() != null && mainWindow.getDebuggerPanel().getDbgController().isDebugging()) {
				if (JOptionPane.showConfirmDialog(mainWindow,
						NLS.str("adb_dialog.restart_while_debugging_msg"),
						NLS.str("adb_dialog.restart_while_debugging_title"),
						JOptionPane.OK_CANCEL_OPTION) != JOptionPane.CANCEL_OPTION) {
					IDebugController ctrl = mainWindow.getDebuggerPanel().getDbgController();
					if (launchForDebugging(mainWindow, ctrl.getProcessName(), true)) {
						dispose();
					}
				}
				return;
			}
			DeviceNode deviceNode = getDeviceNode((DefaultMutableTreeNode) node.getParent());
			if (deviceNode == null) {
				return;
			}
			if (!setupArgs(deviceNode.device, pid, (String) node.getUserObject())) {
				return;
			}
			if (debugSetter.isBeingDebugged()) {
				if (JOptionPane.showConfirmDialog(mainWindow,
						NLS.str("adb_dialog.being_debugged_msg"),
						NLS.str("adb_dialog.being_debugged_title"),
						JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION) {
					return;
				}
			}
			tipLabel.setText(NLS.str("adb_dialog.starting_debugger"));
			if (!attachProcess(mainWindow)) {
				tipLabel.setText(NLS.str("adb_dialog.init_dbg_fail"));
			} else {
				dispose();
			}
		}
	}

	private static boolean attachProcess(MainWindow mainWindow) {
		if (debugSetter == null) {
			return false;
		}
		debugSetter.clearForward();
		String rst = debugSetter.forwardJDWP();
		if (!rst.isEmpty()) {
			UiUtils.showMessageBox(mainWindow, rst);
			return false;
		}
		try {
			return mainWindow.getDebuggerPanel().showDebugger(
					debugSetter.name,
					debugSetter.device.getDeviceInfo().getAdbHost(),
					debugSetter.forwardTcpPort,
					debugSetter.ver,
					debugSetter.device,
					debugSetter.pid);
		} catch (Exception e) {
			LOG.error("Failed to attach to process", e);
			return false;
		}
	}

	public static boolean launchForDebugging(MainWindow mainWindow, String fullAppPath, boolean autoAttach) {
		if (debugSetter != null) {
			debugSetter.autoAttachPkg = autoAttach;
			try {
				int pid = debugSetter.device.launchApp(fullAppPath);
				if (pid != -1) {
					debugSetter.setPid(String.valueOf(pid))
							.setName(fullAppPath);
					return attachProcess(mainWindow);
				}
			} catch (Exception e) {
				LOG.error("Failed to launch app", e);
			}
		}
		return false;
	}

	private String getPid(String nodeText) {
		if (nodeText.startsWith("[pid:")) {
			int pos = nodeText.indexOf("]", "[pid:".length());
			if (pos != -1) {
				return nodeText.substring("[pid:".length(), pos).trim();
			}
		}
		return null;
	}

	private DeviceNode getDeviceNode(DefaultMutableTreeNode node) {
		for (DeviceNode deviceNode : deviceNodes) {
			if (deviceNode.tNode == node) {
				return deviceNode;
			}
		}
		return null;
	}

	private DeviceNode getDeviceNode(ADBDevice device) {
		for (DeviceNode deviceNode : deviceNodes) {
			if (deviceNode.device.equals(device)) {
				return deviceNode;
			}
		}
		throw new JadxRuntimeException("Unexpected device: " + device);
	}

	private void listenJDWP(ADBDevice device) {
		try {
			device.listenForJDWP(this);
		} catch (Exception e) {
			LOG.error("Failed listen for JDWP", e);
		}
	}

	@Override
	public void dispose() {
		clear();
		super.dispose();
		boolean save = mainWindow.getSettings().getAdbDialogPath().equals(pathTextField.getText());
		boolean save1 = mainWindow.getSettings().getAdbDialogHost().equals(hostTextField.getText());
		boolean save2 = mainWindow.getSettings().getAdbDialogPort().equals(portTextField.getText());
		if (save || save1 || save2) {
			mainWindow.getSettings().sync();
		}
	}

	@Override
	public void adbDisconnected() {
		deviceSocket = null;
		SwingUtilities.invokeLater(() -> {
			tipLabel.setText(NLS.str("adb_dialog.disconnected"));
			this.setTitle("");
		});
	}

	@Override
	public void jdwpProcessOccurred(ADBDevice device, Set<String> id) {
		List<ADB.Process> procs;
		try {
			Thread.sleep(40); /*
								 * wait for a moment, let the new processes on remote be fully initialized,
								 * otherwise we may not get its real name but the <pre-initialized> state text.
								 */
			procs = device.getProcessList();
		} catch (Exception e) {
			LOG.error("Failed to get device process list", e);
			procs = Collections.emptyList();
		}
		List<String> procList = new ArrayList<>(id.size());
		if (procs.size() == 0) {
			procList.addAll(id);
		} else {
			for (ADB.Process proc : procs) {
				if (id.contains(proc.pid)) {
					procList.add(String.format("[pid: %-6s] %s", proc.pid, proc.name));
				}
			}
		}
		Collections.reverse(procList);
		DeviceNode node;
		try {
			node = getDeviceNode(device);
		} catch (Exception e) {
			LOG.error("Failed to find device", e);
			return;
		}

		SwingUtilities.invokeLater(() -> {
			node.tNode.removeAllChildren();
			DefaultMutableTreeNode foundNode = null;
			for (String procStr : procList) {
				DefaultMutableTreeNode pnode = new DefaultMutableTreeNode(procStr);
				node.tNode.add(pnode);
				if (!debugSetter.expectPkg.isEmpty() && procStr.endsWith(debugSetter.expectPkg)) {
					if (debugSetter.autoAttachPkg && debugSetter.device.equals(node.device)) {
						debugSetter.set(node.device, debugSetter.ver, getPid(procStr), procStr);
						if (attachProcess(mainWindow)) {
							dispose();
							return;
						}
					}
					foundNode = pnode;
				}
			}
			procTreeModel.reload(node.tNode);
			procTree.expandPath(new TreePath(node.tNode.getPath()));
			if (foundNode != null) {
				TreePath thePath = new TreePath(foundNode.getPath());
				procTree.scrollPathToVisible(thePath);
				procTree.setSelectionPath(thePath);
			}
		});
	}

	private void launchApp() {
		if (deviceNodes.size() == 0) {
			UiUtils.showMessageBox(mainWindow, NLS.str("adb_dialog.no_devices"));
			return;
		}
		JClass cls = DbgUtils.searchMainActivity(mainWindow);
		String pkg = DbgUtils.searchPackageName(mainWindow);
		if (pkg.isEmpty() || cls == null) {
			UiUtils.showMessageBox(mainWindow, NLS.str("adb_dialog.msg_read_mani_fail"));
			return;
		}
		if (scrollToProcNode(pkg)) {
			return;
		}
		String fullName = pkg + "/" + cls.getCls().getClassNode().getClassInfo().getFullName();
		ADBDevice device = deviceNodes.get(0).device; // TODO: if multiple devices presented should let user select the one they desire.
		if (device != null) {
			try {
				device.launchApp(fullName);
			} catch (Exception e) {
				LOG.error("Failed to launch app: {}", fullName, e);
				UiUtils.showMessageBox(mainWindow, e.getMessage());
			}
		}
	}

	private boolean scrollToProcNode(String pkg) {
		if (pkg.isEmpty()) {
			return false;
		}
		debugSetter.expectPkg = " " + pkg;
		for (int i = 0; i < procTreeRoot.getChildCount(); i++) {
			DefaultMutableTreeNode rn = (DefaultMutableTreeNode) procTreeRoot.getChildAt(i);
			for (int j = 0; j < rn.getChildCount(); j++) {
				DefaultMutableTreeNode n = (DefaultMutableTreeNode) rn.getChildAt(j);
				String pName = (String) n.getUserObject();
				if (pName.endsWith(debugSetter.expectPkg)) {
					TreePath path = new TreePath(n.getPath());
					procTree.scrollPathToVisible(path);
					procTree.setSelectionPath(path);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void jdwpListenerClosed(ADBDevice device) {

	}

	private static class DeviceTreeNode extends DefaultMutableTreeNode {
		private static final long serialVersionUID = -1111111202103131112L;
	}

	private static class DeviceNode {
		ADBDevice device;
		DeviceTreeNode tNode;

		DeviceNode(ADBDevice adbDevice) {
			this.device = adbDevice;
			tNode = new DeviceTreeNode();
			refresh();
		}

		void refresh() {
			ADBDeviceInfo info = device.getDeviceInfo();
			String text = info.getModel();
			if (text != null) {
				if (!text.equals(info.getSerial())) {
					text += String.format(" [serial: %s]", info.getSerial());
				}
				text += String.format(" [state: %s]", info.isOnline() ? "online" : "offline");
				tNode.setUserObject(text);
			}
		}
	}

	private boolean setupArgs(ADBDevice device, String pid, String name) {
		String ver = device.getAndroidReleaseVersion();
		if (StringUtils.isEmpty(ver)) {
			if (JOptionPane.showConfirmDialog(mainWindow,
					NLS.str("adb_dialog.unknown_android_ver"),
					"",
					JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION) {
				return false;
			}
			ver = "8";
		}
		ver = getMajorVer(ver);
		debugSetter.set(device, Integer.parseInt(ver), pid, name);
		return true;
	}

	private String getMajorVer(String ver) {
		int pos = ver.indexOf(".");
		if (pos != -1) {
			ver = ver.substring(0, pos);
		}
		return ver;
	}

	private class DebugSetting {
		private static final int FORWARD_TCP_PORT = 33233;
		private int ver;
		private String pid;
		private String name;
		private ADBDevice device;
		private int forwardTcpPort = FORWARD_TCP_PORT;
		private String expectPkg = "";
		private boolean autoAttachPkg = false;

		private void set(ADBDevice device, int ver, String pid, String name) {
			this.ver = ver;
			this.pid = pid;
			this.name = name;
			this.device = device;
			this.autoAttachPkg = false;
			this.expectPkg = "";
		}

		private DebugSetting setPid(String pid) {
			this.pid = pid;
			return this;
		}

		private DebugSetting setName(String name) {
			this.name = name;
			return this;
		}

		private String forwardJDWP() {
			int localPort = forwardTcpPort;
			String resultDesc = "";
			try {
				do {
					ForwardResult rst = device.forwardJDWP(localPort + "", pid);
					if (rst.state == 0) {
						forwardTcpPort = localPort;
						return "";
					}
					if (rst.state == 1) {
						if (rst.desc.contains("Only one usage of each socket address")) { // port is taken by other process
							if (localPort < 65536) {
								localPort++; // retry
								continue;
							}
						}
					}
					resultDesc = rst.desc;
					break;
				} while (true);
			} catch (Exception e) {
				LOG.error("JDWP forward error", e);
			}
			if (StringUtils.isEmpty(resultDesc)) {
				resultDesc = NLS.str("adb_dialog.forward_fail");
			}
			return resultDesc;
		}

		// we have to remove all ports that forwarding the jdwp:pid, otherwise our JDWP handshake may fail.
		private void clearForward() {
			String jdwpPid = " jdwp:" + pid;
			String tcpPort = " tcp:" + forwardTcpPort;
			try {
				List<String> list = ADB.listForward(device.getDeviceInfo().getAdbHost(),
						device.getDeviceInfo().getAdbPort());
				for (String s : list) {
					if (s.startsWith(device.getSerial()) && s.endsWith(jdwpPid) && !s.contains(tcpPort)) {
						String[] fields = s.split("\\s+");
						for (String field : fields) {
							if (field.startsWith("tcp:")) {
								try {
									device.removeForward(field.substring("tcp:".length()));
								} catch (Exception e) {
									LOG.error("JDWP remove forward error", e);
								}
							}
						}
					}
				}
			} catch (Exception e) {
				LOG.error("JDWP clear forward error", e);
			}
		}

		private boolean isBeingDebugged() {
			String jdwpPid = " jdwp:" + pid;
			String tcpPort = " tcp:" + forwardTcpPort;
			try {
				List<String> list = ADB.listForward(device.getDeviceInfo().getAdbHost(),
						device.getDeviceInfo().getAdbPort());
				for (String s : list) {
					if (s.startsWith(device.getSerial()) && s.endsWith(jdwpPid)) {
						return !s.contains(tcpPort);
					}
				}
			} catch (Exception e) {
				LOG.error("ADB list forward error", e);
			}
			return false;
		}
	}
}
