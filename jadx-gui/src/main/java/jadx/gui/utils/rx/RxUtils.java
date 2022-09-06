package jadx.gui.utils.rx;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JTextField;
import javax.swing.event.DocumentListener;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;

import jadx.gui.utils.ui.DocumentUpdateListener;

public class RxUtils {

	public static Flowable<String> textFieldChanges(final JTextField textField) {
		FlowableOnSubscribe<String> source = emitter -> {
			DocumentListener listener = new DocumentUpdateListener(ev -> emitter.onNext(textField.getText()));
			textField.getDocument().addDocumentListener(listener);
			emitter.setDisposable(new CustomDisposable(() -> textField.getDocument().removeDocumentListener(listener)));
		};
		return Flowable.create(source, BackpressureStrategy.LATEST).distinctUntilChanged();
	}

	public static Flowable<String> textFieldEnterPress(final JTextField textField) {
		FlowableOnSubscribe<String> source = emitter -> {
			KeyListener keyListener = new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent ev) {
					if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
						emitter.onNext(textField.getText());
					}
				}
			};
			textField.addKeyListener(keyListener);
			emitter.setDisposable(new CustomDisposable(() -> textField.removeKeyListener(keyListener)));
		};
		return Flowable.create(source, BackpressureStrategy.LATEST).distinctUntilChanged();
	}

}
