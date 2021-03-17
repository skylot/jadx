package jadx.gui.ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.*;

import jadx.core.utils.StringUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.device.protocol.ADB;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

import static jadx.gui.device.protocol.ADB.Device.*;

public class ADBDialog extends JDialog implements ADB.DeviceStateListener, ADB.JDWPProcessListener {
	private static final long serialVersionUID = -1111111202102181630L;
	private static final int FORWARD_TCP_PORT = 33233;
	private static final ImageIcon ICON_DEVICE = UiUtils.openIcon("device");
	private static final ImageIcon ICON_PROCESS = UiUtils.openIcon("process");

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
	private transient int forwardTcpPort = FORWARD_TCP_PORT;

	public ADBDialog(MainWindow mainWindow) {
		super(mainWindow);
		this.mainWindow = mainWindow;
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
		btnPane.add(startServerBtn);
		btnPane.add(refreshBtn);
		refreshBtn.addActionListener(e -> {
			clear();
			procTreeRoot.removeAllChildren();
			procTreeModel.reload(procTreeRoot);
			SwingUtilities.invokeLater(this::connectToADB);
		});

		startServerBtn.addActionListener(e -> startADBServer());

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
			} catch (IOException e) {
				e.printStackTrace();
			}
			deviceSocket = null;
		}
		for (DeviceNode deviceNode : deviceNodes) {
			deviceNode.device.stopListenForJDWP();
		}
		deviceNodes.clear();
	}

	private void detectADBPath() {
		boolean isWinOS;
		try {
			isWinOS = System.getProperty("os.name").startsWith("Windows");
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
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
		} catch (Exception except) {
			tip = except.getMessage();
			except.printStackTrace();
		}
		UiUtils.showMessageBox(mainWindow, tip);
		tipLabel.setText(tip);
	}

	private void connectToADB() {
		String tip;
		try {
			String host = hostTextField.getText();
			String port = portTextField.getText();
			tipLabel.setText(NLS.str("adb_dialog.connecting", host, port));
			deviceSocket = ADB.listenForDeviceState(this, host, Integer.parseInt(port));
			if (deviceSocket != null) {
				tip = NLS.str("adb_dialog.connect_okay", host, port);
				this.setTitle(tip);
			} else {
				tip = NLS.str("adb_dialog.connect_fail");
			}
		} catch (IOException e) {
			e.printStackTrace();
			tip = e.getMessage();
			UiUtils.showMessageBox(mainWindow, tip);
		}
		tipLabel.setText(tip);
	}

	@Override
	public void onDeviceStatusChange(List<ADB.DeviceInfo> deviceInfoList) {
		System.out.println(deviceInfoList.size());
		List<DeviceNode> nodes = new ArrayList<>(deviceInfoList.size());
		info_loop: for (ADB.DeviceInfo info : deviceInfoList) {
			System.out.println(info);
			for (DeviceNode deviceNode : deviceNodes) {
				if (deviceNode.device.updateDeviceInfo(info)) {
					deviceNode.refresh();
					nodes.add(deviceNode);
					continue info_loop;
				}
			}
			ADB.Device device = new ADB.Device(info);
			nodes.add(new DeviceNode(device, null));
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
			String pid = getPid(node);
			if (StringUtils.isEmpty(pid)) {
				return;
			}
			DeviceNode deviceNode = getDeviceNode((DefaultMutableTreeNode) node.getParent());
			if (deviceNode == null) {
				return;
			}
			if (isBeingDebugged(deviceNode, pid)) {
				if (JOptionPane.showConfirmDialog(mainWindow,
						NLS.str("adb_dialog.being_debugged_msg"),
						NLS.str("adb_dialog.being_debugged_title"),
						JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION) {
					return;
				}
			}
			clearForward(deviceNode, pid);
			String rst = forwardJDWP(deviceNode, pid);
			if (!rst.isEmpty()) {
				UiUtils.showMessageBox(mainWindow, rst);
				return;
			}
			System.out.printf("Forward: %s is ok.%n", pid);
			boolean ok = false;
			try {
				ok = mainWindow.getDebuggerPanel().showDebugger(
						(String) node.getUserObject(),
						deviceNode.device.getDeviceInfo().adbHost,
						forwardTcpPort);
			} catch (Exception except) {
				except.printStackTrace();
			}
			if (!ok) {
				tipLabel.setText(NLS.str("adb_dialog.init_dbg_fail"));
			} else {
				dispose();
			}
		}
	}

	private boolean isBeingDebugged(DeviceNode deviceNode, String pid) {
		String jdwpPid = " jdwp:" + pid;
		String tcpPort = " tcp:" + forwardTcpPort;
		try {
			List<String> list = ADB.listForward(hostTextField.getText(), Integer.parseInt(portTextField.getText()));
			for (String s : list) {
				if (s.startsWith(deviceNode.device.getSerial()) && s.endsWith(jdwpPid)) {
					return !s.contains(tcpPort);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	// we have to remove all ports that forwarding the jdwp:pid, otherwise our JDWP handshake may fail.
	private void clearForward(DeviceNode deviceNode, String pid) {
		String jdwpPid = " jdwp:" + pid;
		String tcpPort = " tcp:" + forwardTcpPort;
		try {
			List<String> list = ADB.listForward(hostTextField.getText(), Integer.parseInt(portTextField.getText()));
			for (String s : list) {
				if (s.startsWith(deviceNode.device.getSerial()) && s.endsWith(jdwpPid) && !s.contains(tcpPort)) {
					String[] fields = s.split("\\s+");
					for (String field : fields) {
						if (field.startsWith("tcp:")) {
							try {
								deviceNode.device.removeForward(field.substring("tcp:".length()));
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String forwardJDWP(DeviceNode deviceNode, String pid) {
		int localPort = forwardTcpPort;
		String resultDesc = "";
		try {
			do {
				ForwardResult rst = deviceNode.device.forwardJDWP(localPort + "", pid);
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
			} while (false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (StringUtils.isEmpty(resultDesc)) {
			resultDesc = NLS.str("adb_dialog.forward_fail");
		}
		return resultDesc;
	}

	private String getPid(DefaultMutableTreeNode node) {
		// DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
		String text = (String) node.getUserObject();
		if (text.startsWith("[pid:")) {
			int pos = text.indexOf("]", "[pid:".length());
			if (pos != -1) {
				return text.substring("[pid:".length(), pos).trim();
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

	private DeviceNode getDeviceNode(ADB.Device device) {
		for (DeviceNode deviceNode : deviceNodes) {
			if (deviceNode.device.equals(device)) {
				return deviceNode;
			}
		}
		throw new JadxRuntimeException("Unexpected device: " + device);
	}

	private void listenJDWP(ADB.Device device) {
		try {
			device.listenForJDWP(this);
		} catch (IOException e) {
			e.printStackTrace();
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
	public void jdwpProcessOccurred(ADB.Device device, Set<String> id) {
		List<ADB.Process> procs;
		try {
			Thread.sleep(40); /*
								 * wait for a moment, let the new processes on remote be fully initialized,
								 * otherwise we may not get its real name but the <pre-initialized> state text.
								 */
			procs = device.getProcessList();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			procs = Collections.emptyList();
		}
		List<String> procList = new ArrayList<>(id.size());
		if (procs.size() == 0) {
			procList.addAll(id);
		} else {
			String spaceAlign = "      "; // 6 spaces.
			for (ADB.Process proc : procs) {
				if (id.contains(proc.pid)) {
					if (proc.pid.length() < spaceAlign.length()) {
						proc.pid += spaceAlign.substring(proc.pid.length());
					}
					procList.add(String.format("[pid: %s] %s", proc.pid, proc.name));
				}
			}
		}
		Collections.reverse(procList);
		DeviceNode node;
		try {
			node = getDeviceNode(device);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		node.tNode.removeAllChildren();
		for (String s : procList) {
			node.tNode.add(new DefaultMutableTreeNode(s));
		}
		SwingUtilities.invokeLater(() -> {
			procTreeModel.reload(node.tNode);
			procTree.expandPath(new TreePath(node.tNode.getPath()));
		});
	}

	@Override
	public void jdwpListenerClosed(ADB.Device device) {

	}

	private static class DeviceTreeNode extends DefaultMutableTreeNode {
		private static final long serialVersionUID = -1111111202103131112L;
	}

	private static class DeviceNode {
		ADB.Device device;
		DeviceTreeNode tNode;

		DeviceNode(ADB.Device adbDevice, TreePath treePath) {
			this.device = adbDevice;
			tNode = new DeviceTreeNode();
			refresh();
		}

		void refresh() {
			ADB.DeviceInfo info = device.getDeviceInfo();
			String text = info.model;
			if (!text.equals(info.serial)) {
				text += String.format(" [serial: %s]", info.serial);
			}
			text += String.format(" [state: %s]", info.isOnline() ? "online" : "offline");
			tNode.setUserObject(text);
		}
	}
}
