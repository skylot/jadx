package jadx.gui.ui;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.swing.FocusManager;
import javax.swing.JProgressBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hu.akarnokd.rxjava2.swing.SwingSchedulers;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class HeapUsageBar extends JProgressBar {
	private static final long serialVersionUID = -8739563124249884967L;

	private static final Logger LOG = LoggerFactory.getLogger(HeapUsageBar.class);

	private static final double GB = 1024 * 1024 * 1024d;

	private static final Color GREEN = new Color(0, 180, 0);
	private static final Color RED = new Color(200, 0, 0);

	private final transient Runtime runtime = Runtime.getRuntime();
	private final transient FocusManager focusManager = FocusManager.getCurrentManager();

	private final double maxGB;
	private final long limit;
	private final String labelTemplate;

	private transient Disposable timer;
	private transient Color currentColor;

	public HeapUsageBar() {
		setBorderPainted(false);
		setStringPainted(true);

		long maxMemory = runtime.maxMemory();
		maxGB = maxMemory / GB;
		limit = maxMemory - UiUtils.MIN_FREE_MEMORY;
		labelTemplate = NLS.str("heapUsage.text");

		setMaximum((int) (maxMemory / 1024));
		setColor(GREEN);

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Runtime.getRuntime().gc();
				HeapUsageBar.this.update();
				if (LOG.isDebugEnabled()) {
					LOG.debug("Memory used: {}", UiUtils.memoryInfo());
				}
			}
		});
	}

	@Override
	public void setVisible(boolean enabled) {
		super.setVisible(enabled);
		if (enabled) {
			startTimer();
		} else {
			reset();
		}
	}

	private static class UpdateData {
		int value;
		String label;
		Color color;
	}

	private static final UpdateData SKIP_UPDATE = new UpdateData();

	private void startTimer() {
		if (timer != null) {
			return;
		}
		update();
		timer = Flowable.interval(2, TimeUnit.SECONDS, Schedulers.newThread())
				.map(i -> prepareUpdate())
				.filter(update -> update != SKIP_UPDATE)
				.distinctUntilChanged((a, b) -> Objects.equals(a.label, b.label)) // pass only if label changed
				.subscribeOn(SwingSchedulers.edt())
				.subscribe(this::applyUpdate);
	}

	public UpdateData prepareUpdate() {
		if (focusManager.getActiveWindow() == null) {
			// skip update if app window not active
			return SKIP_UPDATE;
		}
		UpdateData updateData = new UpdateData();
		long used = runtime.totalMemory() - runtime.freeMemory();
		updateData.value = (int) (used / 1024);
		updateData.label = String.format(labelTemplate, used / GB, maxGB);
		updateData.color = used > limit ? RED : GREEN;
		return updateData;
	}

	public void applyUpdate(UpdateData update) {
		setValue(update.value);
		setString(update.label);
		setColor(update.color);
	}

	private void setColor(Color color) {
		if (currentColor != color) {
			setForeground(color);
			currentColor = color;
		}
	}

	private void update() {
		UpdateData update = prepareUpdate();
		if (update != SKIP_UPDATE) {
			applyUpdate(update);
		}
	}

	public void reset() {
		if (timer != null) {
			timer.dispose();
			timer = null;
		}
	}
}
