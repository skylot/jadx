package jadx.gui.utils.ui;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextCaretActionsTest {

	@BeforeAll
	static void install() {
		TextCaretActions.install();
	}

	@Test
	void leftMovesToSelectionStart() {
		JTextField field = painted(new JTextField("Mover"));
		field.selectAll();
		press(field, KeyEvent.VK_LEFT, 0);
		assertThat(field.getCaretPosition()).isEqualTo(0);
		assertThat(field.getSelectedText()).isNull();
	}

	@Test
	void rightMovesToSelectionEnd() {
		JTextField field = painted(new JTextField("Mover"));
		field.setCaretPosition(3);
		field.moveCaretPosition(1);
		press(field, KeyEvent.VK_RIGHT, 0);
		assertThat(field.getCaretPosition()).isEqualTo(3);
		assertThat(field.getSelectedText()).isNull();
	}

	@Test
	void keypadArrows() {
		JTextField field = painted(new JTextField("Mover"));
		field.selectAll();
		press(field, KeyEvent.VK_KP_LEFT, 0);
		assertThat(field.getCaretPosition()).isEqualTo(0);
	}

	@Test
	void moveOneCharWithoutSelection() {
		JTextField field = painted(new JTextField("Mover"));
		field.setCaretPosition(3);
		press(field, KeyEvent.VK_LEFT, 0);
		assertThat(field.getCaretPosition()).isEqualTo(2);
		press(field, KeyEvent.VK_RIGHT, 0);
		assertThat(field.getCaretPosition()).isEqualTo(3);
	}

	@Test
	void shiftArrowKeepsSelecting() {
		JTextField field = painted(new JTextField("Mover"));
		field.selectAll();
		press(field, KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK);
		assertThat(field.getSelectedText()).isEqualTo("Move");
	}

	@Test
	void textArea() {
		JTextArea area = painted(new JTextArea("first line\nsecond line"));
		area.setCaretPosition(14);
		area.moveCaretPosition(6);
		press(area, KeyEvent.VK_LEFT, 0);
		assertThat(area.getCaretPosition()).isEqualTo(6);
		assertThat(area.getSelectedText()).isNull();
	}

	// default caret moves need a painted text component
	private static <T extends JTextComponent> T painted(T textCmp) {
		textCmp.setSize(300, 50);
		BufferedImage img = new BufferedImage(300, 50, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		textCmp.paint(g);
		g.dispose();
		return textCmp;
	}

	private static void press(JTextComponent textCmp, int keyCode, int modifiers) {
		Object actionName = textCmp.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(keyCode, modifiers));
		assertThat(actionName).isNotNull();
		Action action = textCmp.getActionMap().get(actionName);
		action.actionPerformed(new ActionEvent(textCmp, ActionEvent.ACTION_PERFORMED, actionName.toString()));
	}
}
