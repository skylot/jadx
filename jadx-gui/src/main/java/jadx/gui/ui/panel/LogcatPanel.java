package jadx.gui.ui.panel;

import jadx.gui.device.debugger.LogcatController;
import jadx.gui.device.protocol.ADB;
import jadx.gui.utils.NLS;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class LogcatPanel extends JPanel{
	StyleContext sc = StyleContext.getDefaultStyleContext();
	private final AttributeSet defaultAset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.decode("#6c71c4"));
	private final AttributeSet verboseAset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.decode("#2aa198"));
	private final AttributeSet debugAset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.decode("#859900"));
	private final AttributeSet infoAset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.decode("#586e75"));
	private final AttributeSet warningAset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.decode("#b58900"));
	private final AttributeSet errorAset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.decode("#dc322f"));
	private final AttributeSet fatalAset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.decode("#d33682"));
	private final AttributeSet silentAset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.decode("#002b36"));

	private transient JTextPane logcatPane;
	private final transient JDebuggerPanel debugPanel;
	private LogcatController logcatController;
	private boolean ready = false;
	private List<ADB.Process> procs;
	private transient JPanel menuPanel;
	public LogcatPanel(JDebuggerPanel debugPanel){
		this.debugPanel = debugPanel;
	}
	private JScrollPane logcatScroll;

	public boolean showLogcat() {
		ArrayList<String> pkgs = new ArrayList<String>();
		ArrayList<Integer> pids = new ArrayList<Integer>();
		JPanel procBox;
		procs.forEach((proc) -> {
			pkgs.add(String.format("[pid: %-6s] %s", proc.pid, proc.name));
			pids.add(Integer.valueOf(proc.pid));
			logcatController.getFilter().addPid(Integer.valueOf(proc.pid));
		});

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
		JPanel msgTypeBox = new CheckCombo(NLS.str( "logcat.level" ), msgIndex,msgTypes).getContent();

		this.setLayout(new BorderLayout());
		logcatPane = new JTextPane();
		logcatPane.setEditable(false);
		//logcatPane.setLineWrap(true);
		logcatScroll = new JScrollPane(logcatPane);
		menuPanel = new JPanel();
		menuPanel.setLayout(new BorderLayout());

		procBox = new CheckCombo(NLS.str( "logcat.process" ) , pids.toArray(new Integer[0]), pkgs.toArray(new String[0])).getContent();

		menuPanel.add(procBox,BorderLayout.WEST);
		menuPanel.add(msgTypeBox,BorderLayout.CENTER);

		this.add(menuPanel,BorderLayout.NORTH);
		this.add(logcatScroll,BorderLayout.CENTER);

		return true;
	}

	public boolean clearLogcatArea() {
		logcatPane.setText("");
		return true;
	}

	public boolean init(ADB.Device device) {
		try {
			this.logcatController = new LogcatController(this, device);
			this.procs = device.getProcessList();
			if(this.showLogcat() == false) {
				debugPanel.log(NLS.str( "logcat.error_fail_start" ));
			}

		} catch (Exception e) {
			this.ready = false;
			e.printStackTrace();
			return false;
		}
		this.ready = true;
		return true;
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
				.append(" ")
				.append(String.valueOf(logcatInfo.getPid()))
				.append(" ")
				.append(String.valueOf(logcatInfo.getMsgType()))
				.append(" ")
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
			e.printStackTrace();
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
		private String type;
		private Integer[] index;
		private JComboBox combo;
		public CheckCombo(String type, Integer[] index, String[] ids) {
			this.ids = ids;
			this.type = type;
			this.index = index;

		}

		public void actionPerformed(ActionEvent e) {
			JComboBox cb = (JComboBox) e.getSource();
			CheckComboStore store = (CheckComboStore) cb.getSelectedItem();
			CheckComboRenderer ccr = (CheckComboRenderer) cb.getRenderer();
			ccr.checkBox.setSelected((store.state = !store.state));

			switch(this.type) {
				case "Level":
					logcatController.getFilter().toggleMsgType((byte)store.index, store.state);
					logcatController.reload();
				case "Process":
					logcatController.getFilter().togglePid(store.index, store.state);
					logcatController.reload();
			}

		}

		public JPanel getContent() {
			JLabel label = new JLabel(this.type + ": ");
			CheckComboStore[] stores = new CheckComboStore[ids.length];
			for (int j = 0; j < ids.length; j++)
				stores[j] = new CheckComboStore(index[j], ids[j], Boolean.TRUE);
			combo = new JComboBox(stores);
			combo.setRenderer(new CheckComboRenderer());
			JPanel panel = new JPanel();
			panel.add(label);
			panel.add(combo);
			combo.addActionListener(this);
			combo.addMouseListener(new FilterClickListener(this));
			return panel;
		}

		public void toggleAll(boolean checked) {
			for(int i = 0; i < combo.getItemCount(); i++) {
				CheckComboStore ccs = (CheckComboStore) combo.getItemAt(i);
				ccs.state = checked;
				switch (type) {
					case "Process":
						logcatController.getFilter().togglePid(ccs.index, checked);
					case "Level":
						logcatController.getFilter().toggleMsgType((byte) ccs.index, checked);
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
			debugPanel.log(String.valueOf(boxes.size()));
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
		public FilterPopup(CheckCombo combo) {
			this.combo = combo;
			selectAll = new JMenuItem("Select All");
			selectAll.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent actionEvent) {
					combo.toggleAll(true);
				}
			});

			unselectAll = new JMenuItem("Unselect All");
			unselectAll.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent actionEvent) {
					combo.toggleAll(false);
				}
			});


			add(selectAll);
			add(unselectAll);
		}
	}


}
