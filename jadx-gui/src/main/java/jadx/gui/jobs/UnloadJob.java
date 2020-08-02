package jadx.gui.jobs;

import java.util.Set;

import jadx.api.JavaClass;
import jadx.gui.JadxWrapper;

public class UnloadJob extends BackgroundJob {

	private Set<JavaClass> refreshClasses;

	public UnloadJob(JadxWrapper jadxWrapper, int threadsCount, Set<JavaClass> refreshClasses) {
		super(jadxWrapper, threadsCount);
		this.refreshClasses = refreshClasses;
	}

	protected void runJob() {
		for (final JavaClass cls : refreshClasses) {
			addTask(() -> {
				cls.unload();
				cls.getClassNode().deepUnload();
			});
		}
	}

	@Override
	public String getInfoString() {
		return "Refreshing: ";
	}

}
