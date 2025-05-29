package jadx.gui.utils.rx;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.function.Supplier;

import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentListener;

import org.jetbrains.annotations.NotNull;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableEmitter;
import io.reactivex.rxjava3.core.FlowableOnSubscribe;

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
			KeyListener keyListener = enterKeyListener(emitter, textField::getText);
			textField.addKeyListener(keyListener);
			emitter.setDisposable(new CustomDisposable(() -> textField.removeKeyListener(keyListener)));
		};
		return Flowable.create(source, BackpressureStrategy.LATEST).distinctUntilChanged();
	}

	public static Flowable<String> spinnerChanges(final JSpinner spinner) {
		FlowableOnSubscribe<String> source = emitter -> {
			ChangeListener changeListener = e -> emitter.onNext(String.valueOf(spinner.getValue()));
			spinner.addChangeListener(changeListener);
			emitter.setDisposable(new CustomDisposable(() -> spinner.removeChangeListener(changeListener)));
		};
		return Flowable.create(source, BackpressureStrategy.LATEST).distinctUntilChanged();
	}

	public static Flowable<String> spinnerEnterPress(final JSpinner spinner) {
		FlowableOnSubscribe<String> source = emitter -> {
			KeyListener keyListener = enterKeyListener(emitter, () -> String.valueOf(spinner.getValue()));
			spinner.addKeyListener(keyListener);
			emitter.setDisposable(new CustomDisposable(() -> spinner.removeKeyListener(keyListener)));
		};
		return Flowable.create(source, BackpressureStrategy.LATEST).distinctUntilChanged();
	}

	private static @NotNull KeyListener enterKeyListener(FlowableEmitter<String> emitter, Supplier<String> supplier) {
		return new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent ev) {
				if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
					emitter.onNext(supplier.get());
				}
			}
		};
	}

}
