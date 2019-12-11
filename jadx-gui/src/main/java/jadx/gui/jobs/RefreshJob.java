package jadx.gui.jobs;

import java.util.Set;

import jadx.api.JavaClass;
import jadx.gui.JadxWrapper;

public class RefreshJob extends BackgroundJob {

	private Set<JavaClass> refreshClasses;

	public RefreshJob(JadxWrapper jadxWrapper, int threadsCount, Set<JavaClass> refreshClasses) {
		super(jadxWrapper, threadsCount);
		this.refreshClasses = refreshClasses;
	}

	protected void runJob() {
		for (final JavaClass cls : refreshClasses) {
			addTask(cls::refresh);
		}
	}

	@Override
	public String getInfoString() {
		return "Refreshing: ";
	}

}
