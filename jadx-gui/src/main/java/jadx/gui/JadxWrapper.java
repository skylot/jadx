package jadx.gui;

import jadx.api.IJadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaPackage;
import jadx.api.ResourceFile;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxException;

import javax.swing.ProgressMonitor;
import java.io.File;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JadxWrapper {
	private static final Logger LOG = LoggerFactory.getLogger(JadxWrapper.class);

	private final JadxDecompiler decompiler;
	private File openFile;

	public JadxWrapper(IJadxArgs jadxArgs) {
		this.decompiler = new JadxDecompiler(jadxArgs);
	}

	public void openFile(File file) {
		this.openFile = file;
		try {
			this.decompiler.loadFile(file);
		} catch (DecodeException e) {
			LOG.error("Error decode file: {}", file, e);
		} catch (JadxException e) {
			LOG.error("Error open file: {}", file, e);
		}
	}

	public void saveAll(final File dir, final ProgressMonitor progressMonitor) {
		Runnable save = new Runnable() {
			@Override
			public void run() {
				try {
					decompiler.setOutputDir(dir);
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
