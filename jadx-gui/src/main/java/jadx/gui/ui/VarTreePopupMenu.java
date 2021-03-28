package jadx.gui.ui;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

import javax.swing.*;

import jadx.core.dex.instructions.args.ArgType;
import jadx.gui.ui.JDebuggerPanel.ValueTreeNode;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class VarTreePopupMenu extends JPopupMenu {
	private static final long serialVersionUID = -1111111202103170724L;

	private final MainWindow mainWindow;
	private ValueTreeNode valNode;

	public VarTreePopupMenu(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		addItems();
	}

	public void show(ValueTreeNode treeNode, Component invoker, int x, int y) {
		valNode = treeNode;
		super.show(invoker, x, y);
	}

	private void addItems() {
		JMenuItem copyValItem = new JMenuItem(new AbstractAction(NLS.str("debugger.popup_copy_value")) {
			private static final long serialVersionUID = -1111111202103171118L;

			@Override
			public void actionPerformed(ActionEvent e) {
				String val = valNode.getValue();
				if (val != null) {
					if (val.startsWith("\"") && val.endsWith("\"")) {
						val = val.substring(1, val.length() - 1);
					}
					StringSelection stringSelection = new StringSelection(val);
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					clipboard.setContents(stringSelection, null);
				}
			}
		});
		JMenuItem setValItem = new JMenuItem(new AbstractAction(NLS.str("debugger.popup_set_value")) {
			private static final long serialVersionUID = -1111111202103171119L;

			@Override
			public void actionPerformed(ActionEvent e) {
				(new SetValueDialog(mainWindow, valNode)).setVisible(true);
			}
		});

		JMenuItem zeroItem = new JMenuItem(new AbstractAction(NLS.str("debugger.popup_change_to_zero")) {
			private static final long serialVersionUID = -1111111202103171120L;

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					mainWindow.getDebuggerPanel()
							.getDbgController()
							.modifyRegValue(valNode, ArgType.INT, 0);
				} catch (Exception except) {
					except.printStackTrace();
					UiUtils.showMessageBox(mainWindow, except.getMessage());
				}
			}
		});
		JMenuItem oneItem = new JMenuItem(new AbstractAction(NLS.str("debugger.popup_change_to_one")) {
			private static final long serialVersionUID = -1111111202103171121L;

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					mainWindow.getDebuggerPanel()
							.getDbgController()
							.modifyRegValue(valNode, ArgType.INT, 1);
				} catch (Exception except) {
					except.printStackTrace();
					UiUtils.showMessageBox(mainWindow, except.getMessage());
				}
			}
		});

		this.add(copyValItem);
		this.add(new Separator());
		this.add(setValItem);
		this.add(zeroItem);
		this.add(oneItem);
		this.add(zeroItem);
	}
}
