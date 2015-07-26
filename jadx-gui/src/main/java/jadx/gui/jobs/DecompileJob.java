package jadx.gui.jobs;

import jadx.api.JavaClass;
import jadx.gui.JadxWrapper;

public class DecompileJob extends BackgroundJob {

	public DecompileJob(JadxWrapper wrapper, int threadsCount) {
		super(wrapper, threadsCount);
	}

	protected void runJob() {
		for (final JavaClass cls : wrapper.getClasses()) {
			addTask(new Runnable() {
				@Override
				public void run() {
					cls.decompile();
				}
			});
		}
	}

	@Override
	public String getInfoString() {
		return "Decompiling: ";
	}

}
