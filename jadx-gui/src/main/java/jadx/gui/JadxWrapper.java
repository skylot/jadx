package jadx.gui;

import jadx.api.Decompiler;
import jadx.api.IJadxArgs;
import jadx.api.JavaClass;
import jadx.api.JavaPackage;
import jadx.core.utils.exceptions.DecodeException;

import javax.swing.ProgressMonitor;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JadxWrapper {
	private static final Logger LOG = LoggerFactory.getLogger(JadxWrapper.class);

	private final Decompiler decompiler;
	private File openFile;

	public JadxWrapper(IJadxArgs jadxArgs) {
		this.decompiler = new Decompiler(jadxArgs);
	}

	public void openFile(File file) {
		this.openFile = file;
		try {
			this.decompiler.loadFile(file);
		} catch (IOException e) {
			LOG.error("Error open file: " + file, e);
		} catch (DecodeException e) {
			LOG.error("Error decode file: " + file, e);
		}
	}

	public void saveAll(final File dir, final ProgressMonitor progressMonitor) {
		Runnable save = new Runnable() {
			@Override
			public void run() {
				try {
					decompiler.setOutputDir(dir);
					ThreadPoolExecutor ex = decompiler.getSaveExecutor();
					while (ex.isTerminating()) {
						long total = ex.getTaskCount();
						long done = ex.getCompletedTaskCount();
						progressMonitor.setProgress((int) (done * 100.0 / (double) total));
						Thread.sleep(500);
					}
					progressMonitor.close();
					LOG.info("done");
				} catch (InterruptedException e) {
					e.printStackTrace();
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

	public File getOpenFile() {
		return openFile;
	}
}
