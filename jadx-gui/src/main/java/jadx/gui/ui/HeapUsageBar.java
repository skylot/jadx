package jadx.gui.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.utils.NLS;
import jadx.gui.utils.Utils;

public class HeapUsageBar extends JProgressBar implements ActionListener {
	private static final Logger LOG = LoggerFactory.getLogger(HeapUsageBar.class);
	private static final long serialVersionUID = -8739563124249884967L;

	private static final double TWO_TO_20 = 1048576d;

	private static final Color GREEN = new Color(0, 180, 0);
	private static final Color RED = new Color(200, 0, 0);

	private final transient Runtime runtime = Runtime.getRuntime();
	private final transient Timer timer;

	private final double maxGB;

	public HeapUsageBar() {
		setBorderPainted(false);
		setStringPainted(true);
		setValue(10);
		int maxKB = (int) (runtime.maxMemory() / 1024);
		setMaximum(maxKB);
		maxGB = maxKB / TWO_TO_20;
		update();
		timer = new Timer(2000, this);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Runtime.getRuntime().gc();
				update();
				if (LOG.isDebugEnabled()) {
					LOG.debug("Memory used: {}", Utils.memoryInfo());
				}
			}
		});
	}

	public void update() {
		long used = runtime.totalMemory() - runtime.freeMemory();
		int usedKB = (int) (used / 1024);
		setValue(usedKB);
		setString(NLS.str("heapUsage.text", (usedKB / TWO_TO_20), maxGB));

		if ((used + Utils.MIN_FREE_MEMORY) > runtime.maxMemory()) {
			setForeground(RED);
		} else {
			setForeground(GREEN);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		update();
	}

	@Override
	public void setVisible(boolean aFlag) {
		super.setVisible(aFlag);
		if (aFlag) {
			timer.start();
		} else {
			timer.stop();
		}
	}
}
