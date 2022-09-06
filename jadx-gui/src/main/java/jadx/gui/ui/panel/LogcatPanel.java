package jadx.gui.ui.panel;

import jadx.gui.device.debugger.LogcatController;
import jadx.gui.device.protocol.ADB;
import jadx.gui.device.protocol.ADBDevice;
import jadx.gui.utils.NLS;

import jadx.gui.utils.UiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class LogcatPanel extends JPanel{
	private static final Logger LOG = LoggerFactory.getLogger(LogcatPanel.class);


	StyleContext sc = StyleContext.getDefaultStyleContext();
	private final AttributeSet defaultAset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.decode("#6c71c4"));
	private final AttributeSet verboseAset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.decode("#2aa198"));
	private final AttributeSet debugAset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.decode("#859900"));
	private final AttributeSet infoAset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.decode("#586e75"));
	private final AttributeSet warningAset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.decode("#b58900"));
	private final AttributeSet errorAset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.decode("#dc322f"));
	private final AttributeSet fatalAset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.decode("#d33682"));
	private final AttributeSet silentAset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.decode("#002b36"));

	private static final ImageIcon ICON_PAUSE = UiUtils.openSvgIcon("debugger/threadFrozen");
	private static final ImageIcon ICON_RUN = UiUtils.openSvgIcon("debugger/execute");;


	private transient JTextPane logcatPane;
	private final transient JDebuggerPanel debugPanel;
	private LogcatController logcatController;
	private boolean ready = false;
	private List<ADB.Process> procs;
	private transient JToolBar menuPanel;
	public LogcatPanel(JDebuggerPanel debugPanel){
		this.debugPanel = debugPanel;
	}
	private JScrollPane logcatScroll;
	private int pid;
	private ArrayList<String> pkgs;
	private ArrayList<Integer> pids;

	private AbstractAction pauseButton = new AbstractAction(NLS.str("debugger.pause"), ICON_PAUSE) {
		@Override
		public void actionPerformed(ActionEvent e) {
			toggleLogcat();
		}
	};

	public boolean showLogcat() {
		pkgs = new ArrayList<String>();
		pids = new ArrayList<Integer>();
		JPanel procBox;
		for( ADB.Process proc : procs.subList(1, procs.size() )) {  //skipping first element because it contains the column label
			pkgs.add(String.format("[pid: %-6s] %s", proc.pid, proc.name));
			pids.add(Integer.valueOf(proc.pid));
			logcatController.getFilter().addPid(Integer.valueOf(proc.pid));
		};

		String msgTypes[] = {
				NLS.str( "logcat.default" ),
				NLS.str( "logcat.verbose" ),
				NLS.str( "logcat.debug" ),
				NLS.str( "logcat.info" ),
				NLS.str( "logcat.warn" ),
				NLS.str( "logcat.error" ),
				NLS.str( "logcat.fatal" ),
				NLS.str( "logcat.silent" )};
		Integer msgIndex[] = {1,2,3,4,5,6,7,8};

		this.setLayout(new BorderLayout());
		logcatPane = new JTextPane();
		logcatPane.setEditable(false);
		logcatScroll = new JScrollPane(logcatPane);
		menuPanel = new JToolBar();

		procBox = new CheckCombo(NLS.str( "logcat.process" ) , 1, pids.toArray(new Integer[0]), pkgs.toArray(new String[0])).getContent();
		JPanel msgTypeBox = new CheckCombo(NLS.str( "logcat.level" ), 2, msgIndex,msgTypes).getContent();

		menuPanel.add(procBox);
		menuPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		menuPanel.add(msgTypeBox);
		menuPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		menuPanel.add(pauseButton);

		this.add(menuPanel,BorderLayout.NORTH);
		this.add(logcatScroll,BorderLayout.CENTER);

		return true;
	}

	public boolean clearLogcatArea() {
		logcatPane.setText("");
		return true;
	}

	public boolean init(ADBDevice device, String pid) {
		this.pid = Integer.valueOf(pid);
		try {
			this.logcatController = new LogcatController(this, device);
			this.procs = device.getProcessList();
			if(this.showLogcat() == false) {
				debugPanel.log(NLS.str( "logcat.error_fail_start" ));
			}

		} catch (Exception e) {
			this.ready = false;
			LOG.error("Failed to start logcat", e);
			return false;
		}
		this.ready = true;
		return true;
	}

	private void toggleLogcat() {
		if(this.logcatController.getStatus() == "running") {
			this.debugPanel.log("STOPPING");
			this.logcatController.stopLogcat();
			this.pauseButton.putValue(Action.SMALL_ICON,ICON_RUN);
		} else if(this.logcatController.getStatus() == "stopped") {
			this.debugPanel.log("STARTING");
			this.logcatController.startLogcat();
			this.pauseButton.putValue(Action.SMALL_ICON,ICON_PAUSE);
		}
	}

	public JDebuggerPanel getDebugPanel() {
		return debugPanel;
	}

	public boolean isReady() {
		return this.ready;
	}

	private boolean isAtBottom(JScrollBar scrollbar) {
		BoundedRangeModel model = scrollbar.getModel();
		return (model.getExtent() + model.getValue()) == model.getMaximum();
	}

	public void log(LogcatController.logcatInfo logcatInfo) {
		boolean atBottom = false;

		int len = logcatPane.getDocument().getLength();
		JScrollBar scrollbar = logcatScroll.getVerticalScrollBar();
		if(isAtBottom(scrollbar)) {
			atBottom = true;
		}

		StringBuilder sb = new StringBuilder();
		sb.append(" > ")
				.append(logcatInfo.getTimestamp())
				.append(" [pid: ")
				.append(String.valueOf(logcatInfo.getPid()))
				.append("] ")
				.append(logcatInfo.getMsgTypeString())
				.append(": ")
				.append(logcatInfo.getMsg())
				.append("\n");

		try {
			switch(logcatInfo.getMsgType()) {
				case 0: //Unknown
					break;
				case 1: //Default
					logcatPane.getDocument().insertString(len, sb.toString(), defaultAset);
					break;
				case 2: //Verbose
					logcatPane.getDocument().insertString(len, sb.toString(), verboseAset);
					break;
				case 3: //Debug
					logcatPane.getDocument().insertString(len, sb.toString(), debugAset);
					break;
				case 4: //Info
					logcatPane.getDocument().insertString(len, sb.toString(), infoAset);
					break;
				case 5: //Warn
					logcatPane.getDocument().insertString(len, sb.toString(), warningAset);
					break;
				case 6: //Error
					logcatPane.getDocument().insertString(len, sb.toString(), errorAset);
					break;
				case 7: //Fatal
					logcatPane.getDocument().insertString(len, sb.toString(), fatalAset);
					break;
				case 8: //Silent
					logcatPane.getDocument().insertString(len, sb.toString(), silentAset);
					break;
				default:
					logcatPane.getDocument().insertString(len, sb.toString(), null);
					break;
			}
		} catch(Exception e) {
			LOG.error("Failed to write logcat message", e);
		}

		if(atBottom) {
			EventQueue.invokeLater(new Runnable()
			{
				public void run() {
					scrollbar.setValue(scrollbar.getMaximum());
				}
			});
		}
	}

	public void exit() {
		logcatController.exit();
		clearLogcatArea();
	}

	class CheckCombo implements ActionListener {
		private String[] ids;
		private int type;
		private String label;
		private Integer[] index;
		private JComboBox combo;
		public CheckCombo(String label, int type, Integer[] index, String[] ids) {
			this.ids = ids;
			this.type = type;
			this.label = label;
			this.index = index;

		}

		public void actionPerformed(ActionEvent e) {
			JComboBox cb = (JComboBox) e.getSource();
			CheckComboStore store = (CheckComboStore) cb.getSelectedItem();
			CheckComboRenderer ccr = (CheckComboRenderer) cb.getRenderer();
			ccr.checkBox.setSelected((store.state = !store.state));

			switch(this.type) {
				case 1: //process
					logcatController.getFilter().togglePid(store.index, store.state);
					logcatController.reload();
				case 2: //label
					logcatController.getFilter().toggleMsgType((byte)store.index, store.state);
					logcatController.reload();

			}

		}

		public JPanel getContent() {
			JLabel label = new JLabel(this.label + ": ");
			CheckComboStore[] stores = new CheckComboStore[ids.length];
			for (int j = 0; j < ids.length; j++)
				stores[j] = new CheckComboStore(index[j], ids[j], Boolean.TRUE);
			combo = new JComboBox(stores);
			combo.setRenderer(new CheckComboRenderer());
			JPanel panel = new JPanel();
			panel.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.weightx = 0;
			c.gridwidth = 1;
			c.insets = new Insets(0,1,0,1);
			panel.add(label,c);
			c.weightx = 1;
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.anchor = GridBagConstraints.WEST;
			c.insets = new Insets(0,1,0,1);
			panel.add(combo,c);
			combo.addActionListener(this);
			combo.addMouseListener(new FilterClickListener(this));
			return panel;
		}

		public void toggleAll(boolean checked) {
			for(int i = 0; i < combo.getItemCount(); i++) {
				CheckComboStore ccs = (CheckComboStore) combo.getItemAt(i);
				ccs.state = checked;
				switch (type) {
					case 1: //process
						logcatController.getFilter().togglePid(ccs.index, checked);
					case 2: //level
						logcatController.getFilter().toggleMsgType((byte) ccs.index, checked);
				}
			}
			logcatController.reload();
		}

		public void selectAllBut(int ind) {
			for(int i = 0; i < combo.getItemCount(); i++) {
				CheckComboStore ccs = (CheckComboStore) combo.getItemAt(i);
				if(i != ind) {
					ccs.state = false;
				} else {
					ccs.state = true;
				}
				switch (type) {
					case 1: //process
						logcatController.getFilter().togglePid(ccs.index, ccs.state);
					case 2: //level
						logcatController.getFilter().toggleMsgType((byte) ccs.index, ccs.state);
				}
			}
			logcatController.reload();
		}
	}

	class CheckComboRenderer implements ListCellRenderer {
		JCheckBox checkBox;
		ArrayList<JCheckBox> boxes = new ArrayList<JCheckBox>();

		public CheckComboRenderer() {
			checkBox = new JCheckBox();
		}

		public Component getListCellRendererComponent(JList list, Object value,
													  int index, boolean isSelected, boolean cellHasFocus) {
			CheckComboStore store = (CheckComboStore) value;
			checkBox.setText(store.id);
			checkBox.setSelected(((Boolean) store.state).booleanValue());
			boxes.add(checkBox);
			return checkBox;
		}

		public JCheckBox getCheckBoxAt(int i) {
			return boxes.get(i);
		}
	}

	class CheckComboStore {
		String id;
		Boolean state;
		int index;

		public CheckComboStore(int index, String id, Boolean state) {
			this.id = id;
			this.state = state;
			this.index = index;
		}
	}

	class FilterClickListener extends MouseAdapter {
		CheckCombo combo;

		public FilterClickListener(CheckCombo combo) {
			this.combo = combo;
		}
		public void mousePressed(MouseEvent e) {
			if (e.isPopupTrigger()) {
				doPop(e);
			}
		}

		public void mouseReleased(MouseEvent e) {
			if (e.isPopupTrigger()) {
				doPop(e);
			}
		}

		private void doPop(MouseEvent e) {
			FilterPopup menu = new FilterPopup(combo);
			menu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	class FilterPopup extends JPopupMenu{
		CheckCombo combo;
		JMenuItem selectAll;
		JMenuItem unselectAll;
		JMenuItem selectAttached;
		public FilterPopup(CheckCombo combo) {
			this.combo = combo;
			selectAll = new JMenuItem(NLS.str( "logcat.select_all" ));
			selectAll.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent actionEvent) {
					combo.toggleAll(true);
				}
			});

			unselectAll = new JMenuItem(NLS.str( "logcat.unselect_all" ));
			unselectAll.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent actionEvent) {
					combo.toggleAll(false);
				}
			});

			if(combo.type == 1) {
				selectAttached = new JMenuItem(NLS.str( "logcat.select_attached") );
				selectAttached.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent actionEvent) {
						combo.selectAllBut(pids.indexOf(pid));
					}
				});
				add(selectAttached);
			}


			add(selectAll);
			add(unselectAll);
		}
	}


}
