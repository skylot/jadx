package jadx.gui.device.debugger;

public class SuspendInfo {
	private boolean terminated;
	private boolean newRound;
	private final InfoSetter updater = new InfoSetter();

	public long getThreadID() {
		return updater.thread;
	}

	public long getClassID() {
		return updater.clazz;
	}

	public long getMethodID() {
		return updater.method;
	}

	public long getOffset() {
		return updater.offset;
	}

	InfoSetter update() {
		updater.changed = false;
		updater.nextRound(newRound);
		this.newRound = false;
		return updater;
	}

	// called by decodingLoop, to tell the updater even though the values are the same,
	// they are decoded from another packet, they should be treated as new.
	void nextRound() {
		newRound = true;
	}

	// according to JDWP document it's legal to fire two or more events on a same location,
	// e.g. one for single step and the other for breakpoint, so when this happened we only
	// want one of them.
	boolean isAnythingChanged() {
		return updater.changed;
	}

	public boolean isTerminated() {
		return terminated;
	}

	void setTerminated() {
		terminated = true;
	}

	static class InfoSetter {
		private long thread;
		private long clazz;
		private long method;
		private long offset; // code offset;
		private boolean changed;

		void nextRound(boolean newRound) {
			if (!changed) {
				changed = newRound;
			}
		}

		InfoSetter updateThread(long thread) {
			if (!changed) {
				changed = this.thread != thread;
			}
			this.thread = thread;
			return this;
		}

		InfoSetter updateClass(long clazz) {
			if (!changed) {
				changed = this.clazz != clazz;
			}
			this.clazz = clazz;
			return this;
		}

		InfoSetter updateMethod(long method) {
			if (!changed) {
				changed = this.method != method;
			}
			this.method = method;
			return this;
		}

		InfoSetter updateOffset(long offset) {
			if (!changed) {
				changed = this.offset != offset;
			}
			this.offset = offset;
			return this;
		}
	}
}
