package jadx.gui;

import javax.swing.*;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaPackage;
import jadx.api.ResourceFile;
import jadx.gui.settings.JadxSettings;

public class JadxWrapper {
	private static final Logger LOG = LoggerFactory.getLogger(JadxWrapper.class);

	private final JadxSettings settings;
	private JadxDecompiler decompiler;
	private File openFile;

	public JadxWrapper(JadxSettings settings) {
		this.settings = settings;
	}

	public void openFile(File file) {
		this.openFile = file;
		try {
			this.decompiler = new JadxDecompiler(settings.toJadxArgs());
			this.decompiler.getArgs().setInputFiles(Collections.singletonList(file));
			this.decompiler.load();
		} catch (Exception e) {
			LOG.error("Error load file: {}", file, e);
		}
	}

	public void saveAll(final File dir, final ProgressMonitor progressMonitor) {
		Runnable save = new Runnable() {
			@Override
			public void run() {
				try {
					decompiler.getArgs().setRootDir(dir);
					ThreadPoolExecutor ex = (ThreadPoolExecutor) decompiler.getSaveExecutor();
					ex.shutdown();
					while (ex.isTerminating()) {
						long total = ex.getTaskCount();
						long done = ex.getCompletedTaskCount();
						progressMonitor.setProgress((int) (done * 100.0 / (double) total));
						Thread.sleep(500);
					}
					progressMonitor.close();
					LOG.info("done");
				} catch (InterruptedException e) {
					LOG.error("Save interrupted", e);
					Thread.currentThread().interrupt();
				}
			}
		};
		new Thread(save).start();
	}

	public List<JavaClass> getClasses() {
		return decompiler.getClasses();
	}

	public List<JavaPackage> getPackages() {
		return decompiler.getPackages();
	}

	public List<ResourceFile> getResources() {
		return decompiler.getResources();
	}

	public File getOpenFile() {
		return openFile;
	}
}
