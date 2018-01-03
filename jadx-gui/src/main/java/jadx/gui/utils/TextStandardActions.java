package jadx.gui.utils;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@SuppressWarnings("serial")
public class TextStandardActions {

	private final JTextComponent textComponent;

	private final JPopupMenu popup = new JPopupMenu();
	private final UndoManager undoManager;

	private Action undoAction;
	private Action redoAction;
	private Action cutAction;
	private Action copyAction;
	private Action pasteAction;
	private Action deleteAction;
	private Action selectAllAction;

	public TextStandardActions(JTextComponent textComponent) {
		this.textComponent = textComponent;
		this.undoManager = new UndoManager();

		initActions();
		addPopupItems();
		addKeyActions();

		registerListeners();
	}

	private void initActions() {
		undoAction = new AbstractAction(NLS.str("popup.undo")) {
			@Override
			public void actionPerformed(ActionEvent ae) {
				if (undoManager.canUndo()) {
					undoManager.undo();
				}
			}
		};
		redoAction = new AbstractAction(NLS.str("popup.redo")) {
			@Override
			public void actionPerformed(ActionEvent ae) {
				if (undoManager.canRedo()) {
					undoManager.redo();
				}
			}
		};
		cutAction = new AbstractAction(NLS.str("popup.cut")) {
			@Override
			public void actionPerformed(ActionEvent ae) {
				textComponent.cut();
			}
		};
		copyAction = new AbstractAction(NLS.str("popup.copy")) {
			@Override
			public void actionPerformed(ActionEvent ae) {
				textComponent.copy();
			}
		};
		pasteAction = new AbstractAction(NLS.str("popup.paste")) {
			@Override
			public void actionPerformed(ActionEvent ae) {
				textComponent.paste();
			}
		};
		deleteAction = new AbstractAction(NLS.str("popup.delete")) {
			@Override
			public void actionPerformed(ActionEvent ae) {
				textComponent.replaceSelection("");
			}
		};
		selectAllAction = new AbstractAction(NLS.str("popup.select_all")) {
			@Override
			public void actionPerformed(ActionEvent ae) {
				textComponent.selectAll();
			}
		};
	}

	void addPopupItems() {
		popup.add(undoAction);
		popup.add(redoAction);
		popup.addSeparator();
		popup.add(cutAction);
		popup.add(copyAction);
		popup.add(pasteAction);
		popup.add(deleteAction);
		popup.addSeparator();
		popup.add(selectAllAction);
	}

	private void addKeyActions() {
		KeyStroke undoKey = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK);
		textComponent.getInputMap().put(undoKey, undoAction);
		KeyStroke redoKey = KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK);
		textComponent.getInputMap().put(redoKey, redoAction);
	}

	private void registerListeners() {
		textComponent.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				if (e.getModifiers() == InputEvent.BUTTON3_MASK
						&& e.getSource() == textComponent) {
					process(e);
				}
			}
		});
		textComponent.getDocument().addUndoableEditListener(new UndoableEditListener() {
			public void undoableEditHappened(UndoableEditEvent event) {
				undoManager.addEdit(event.getEdit());
			}
		});
	}

	private void process(MouseEvent e) {
		textComponent.requestFocus();

		boolean enabled = textComponent.isEnabled();
		boolean editable = textComponent.isEditable();
		boolean nonempty = !(textComponent.getText() == null || textComponent.getText().equals(""));
		boolean marked = textComponent.getSelectedText() != null;
		boolean pasteAvailable = Toolkit.getDefaultToolkit().getSystemClipboard()
				.getContents(null).isDataFlavorSupported(DataFlavor.stringFlavor);

		undoAction.setEnabled(enabled && editable && undoManager.canUndo());
		redoAction.setEnabled(enabled && editable && undoManager.canRedo());
		cutAction.setEnabled(enabled && editable && marked);
		copyAction.setEnabled(enabled && marked);
		pasteAction.setEnabled(enabled && editable && pasteAvailable);
		deleteAction.setEnabled(enabled && editable && marked);
		selectAllAction.setEnabled(enabled && nonempty);

		int nx = e.getX();
		if (nx > 500) {
			nx = nx - popup.getSize().width;
		}
		popup.show(e.getComponent(), nx, e.getY() - popup.getSize().height);
	}
}
