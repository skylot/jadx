package jadx.gui.ui.panel;

import jadx.gui.device.debugger.LogcatController;
import jadx.gui.device.protocol.ADB;

import javax.swing.*;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class LogcatPanel extends JPanel{
	private final Highlighter.HighlightPainter defaultPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.PINK);
	private final Highlighter.HighlightPainter verbosePainter = new DefaultHighlighter.DefaultHighlightPainter(Color.CYAN);
	private final Highlighter.HighlightPainter debugPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.GREEN);
	private final Highlighter.HighlightPainter infoPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.LIGHT_GRAY);
	private final Highlighter.HighlightPainter warningPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);
	private final Highlighter.HighlightPainter errorPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.RED);
	private final Highlighter.HighlightPainter fatalPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.MAGENTA);
	private final Highlighter.HighlightPainter silentPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.WHITE);

	private transient JTextArea logcatArea;
	private final transient JDebuggerPanel debugPanel;
	private LogcatController logcatController;
	private boolean ready = false;
	private List<ADB.Process> procs;
	private transient JPanel menuPanel;
	public LogcatPanel(JDebuggerPanel debugPanel){
		this.debugPanel = debugPanel;
	}

	public boolean showLogcat() {
		ArrayList<String> pkgs = new ArrayList<String>();
		ArrayList<Integer> pids = new ArrayList<Integer>();
		JPanel procBox;
		procs.forEach((proc) -> {
			pkgs.add(String.format("[pid: %-6s] %s", proc.pid, proc.name));
			pids.add(Integer.valueOf(proc.pid));
			logcatController.getFilter().addPid(Integer.valueOf(proc.pid));
		});

		String msgTypes[] = {"Default", "Verbose", "Debug", "Info", "Warn", "Error", "Fatal", "Silent"};
		Integer msgIndex[] = {1,2,3,4,5,6,7,8};
		JPanel msgTypeBox = new CheckCombo("Level", msgIndex,msgTypes).getContent();

		this.setLayout(new BorderLayout());
		logcatArea = new JTextArea();
		logcatArea.setEditable(false);
		logcatArea.setLineWrap(true);
		JScrollPane logcatScroll = new JScrollPane(logcatArea);
		menuPanel = new JPanel();
		menuPanel.setLayout(new BorderLayout());

		procBox = new CheckCombo("Process", pids.toArray(new Integer[0]), pkgs.toArray(new String[0])).getContent();

		menuPanel.add(procBox,BorderLayout.WEST);
		menuPanel.add(msgTypeBox,BorderLayout.CENTER);

		this.add(menuPanel,BorderLayout.NORTH);
		this.add(logcatScroll,BorderLayout.CENTER);

		return true;
	}

	public boolean clearLogcatArea() {
		logcatArea.getHighlighter().removeAllHighlights();
		logcatArea.setText("");
		return true;
	}

	public boolean init(ADB.Device device) {
		try {
			this.logcatController = new LogcatController(this, device);
			this.procs = device.getProcessList();
			if(this.showLogcat() == false) {
				debugPanel.log("Failed To Start Logcat");
			}

		} catch (Exception e) {
			this.ready = false;
			e.printStackTrace();
			return false;
		}
		this.ready = true;
		return true;
	}


	public void log(LogcatController.logcatInfo logcatInfo) {
		int lines = logcatArea.getLineCount();
		Highlighter logcatHilight = logcatArea.getHighlighter();
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
		logcatArea.append(sb.toString());
		try {
			switch(logcatInfo.getMsgType()) {
				case 0: //Unknown
					break;
				case 1: //Default
					logcatHilight.addHighlight(logcatArea.getLineStartOffset(lines-1), logcatArea.getLineEndOffset(lines-1) - 1, defaultPainter);
					break;
				case 2: //Verbose
					logcatHilight.addHighlight(logcatArea.getLineStartOffset(lines-1), logcatArea.getLineEndOffset(lines-1) - 1, verbosePainter);
					break;
				case 3: //Debug
					logcatHilight.addHighlight(logcatArea.getLineStartOffset(lines-1), logcatArea.getLineEndOffset(lines-1) - 1, debugPainter);
					break;
				case 4: //Info
					logcatHilight.addHighlight(logcatArea.getLineStartOffset(lines-1), logcatArea.getLineEndOffset(lines-1) - 1, infoPainter);
					break;
				case 5: //Warn
					logcatHilight.addHighlight(logcatArea.getLineStartOffset(lines-1), logcatArea.getLineEndOffset(lines-1) - 1, warningPainter);
					break;
				case 6: //Error
					logcatHilight.addHighlight(logcatArea.getLineStartOffset(lines-1), logcatArea.getLineEndOffset(lines-1) - 1, errorPainter);
					break;
				case 7: //Fatal
					logcatHilight.addHighlight(logcatArea.getLineStartOffset(lines-1), logcatArea.getLineEndOffset(lines-1) - 1, fatalPainter);
					break;
				case 8: //Silent
					logcatHilight.addHighlight(logcatArea.getLineStartOffset(lines-1), logcatArea.getLineEndOffset(lines-1) - 1, silentPainter);
					break;
				default:
					break;
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
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

		public void checkAll() {
			ListModel model = combo.getModel();
			for(int i = 0; i < model.getSize(); i++) {
				CheckComboStore ccs = (CheckComboStore) model.getElementAt(i);
				switch (type) {
					case "Process":
						logcatController.getFilter().togglePid(ccs.index, true);
					case "Level":
						logcatController.getFilter().toggleMsgType((byte) ccs.index, true);
				}
			}
			logcatController.reload();
		}
	}

	class CheckComboRenderer implements ListCellRenderer {
		JCheckBox checkBox;

		public CheckComboRenderer() {
			checkBox = new JCheckBox();
		}

		public Component getListCellRendererComponent(JList list, Object value,
													  int index, boolean isSelected, boolean cellHasFocus) {
			CheckComboStore store = (CheckComboStore) value;
			checkBox.setText(store.id);
			checkBox.setSelected(((Boolean) store.state).booleanValue());
			return checkBox;
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
					test();
				}
			});

			unselectAll = new JMenuItem("Unselect All");
			unselectAll.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent actionEvent) {
					test();
				}
			});


			add(selectAll);
			add(unselectAll);
		}

		public void test() {
			combo.checkAll();
		}
	}


}
