package jadx.gui.jobs;

import java.io.File;
import java.util.List;

import javax.swing.JOptionPane;

import jadx.api.ICodeCache;
import jadx.api.JadxDecompiler;
import jadx.gui.JadxWrapper;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.FixedCodeCache;
import jadx.gui.utils.NLS;

public class ExportTask implements IBackgroundTask {

	private final MainWindow mainWindow;
	private final JadxWrapper wrapper;
	private final File saveDir;

	private int timeLimit;
	private ICodeCache uiCodeCache;

	public ExportTask(MainWindow mainWindow, JadxWrapper wrapper, File saveDir) {
		this.mainWindow = mainWindow;
		this.wrapper = wrapper;
		this.saveDir = saveDir;
	}

	@Override
	public String getTitle() {
		return NLS.str("msg.saving_sources");
	}

	@Override
	public List<Runnable> scheduleJobs() {
		wrapCodeCache();
		JadxDecompiler decompiler = wrapper.getDecompiler();
		decompiler.getArgs().setRootDir(saveDir);
		List<Runnable> saveTasks = decompiler.getSaveTasks();
		this.timeLimit = DecompileTask.calcDecompileTimeLimit(saveTasks.size());
		return saveTasks;
	}

	private void wrapCodeCache() {
		uiCodeCache = wrapper.getArgs().getCodeCache();
		// do not save newly decompiled code in cache to not increase memory usage
		// TODO: maybe make memory limited cache?
		wrapper.getArgs().setCodeCache(new FixedCodeCache(uiCodeCache));
	}

	@Override
	public void onFinish(TaskStatus status, long skipped) {
		// restore initial code cache
		wrapper.getArgs().setCodeCache(uiCodeCache);
		if (skipped == 0) {
			return;
		}
		String reason = getIncompleteReason(status);
		if (reason != null) {
			JOptionPane.showMessageDialog(mainWindow,
					NLS.str("message.saveIncomplete", reason, skipped),
					NLS.str("message.errorTitle"), JOptionPane.ERROR_MESSAGE);
		}
	}

	private String getIncompleteReason(TaskStatus status) {
		switch (status) {
			case CANCEL_BY_USER:
				return NLS.str("message.userCancelTask");

			case CANCEL_BY_TIMEOUT:
				return NLS.str("message.taskTimeout", timeLimit());

			case CANCEL_BY_MEMORY:
				mainWindow.showHeapUsageBar();
				return NLS.str("message.memoryLow");

			case ERROR:
				return NLS.str("message.taskError");
		}
		return null;
	}

	@Override
	public int timeLimit() {
		return timeLimit;
	}

	@Override
	public boolean canBeCanceled() {
		return true;
	}

	@Override
	public boolean checkMemoryUsage() {
		return true;
	}
}
