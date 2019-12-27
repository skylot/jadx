package jadx.gui.jobs;

import jadx.api.JavaClass;
import jadx.gui.JadxWrapper;
import jadx.gui.utils.NLS;

public class DecompileJob extends BackgroundJob {

	public DecompileJob(JadxWrapper wrapper, int threadsCount) {
		super(wrapper, threadsCount);
	}

	@Override
	protected void runJob() {
		for (final JavaClass cls : wrapper.getIncludedClasses()) {
			addTask(cls::decompile);
		}
	}

	@Override
	public String getInfoString() {
		return NLS.str("progress.decompile") + "… ";
	}
}
