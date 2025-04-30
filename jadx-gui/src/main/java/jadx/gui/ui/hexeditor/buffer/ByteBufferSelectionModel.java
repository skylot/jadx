package jadx.gui.ui.hexeditor.buffer;

import java.util.ArrayList;
import java.util.List;

/*
 * code taken from https://github.com/kreativekorp/hexcellent
 */
public class ByteBufferSelectionModel {
	private long start = 0;
	private long end = 0;
	private boolean midbyte = false;

	public long getSelectionStart() {
		return start;
	}

	public long getSelectionEnd() {
		return end;
	}

	public long getSelectionMin() {
		return Math.min(start, end);
	}

	public long getSelectionMax() {
		return Math.max(start, end);
	}

	public long getSelectionLength() {
		return Math.abs(start - end);
	}

	public boolean isMidByte() {
		return midbyte;
	}

	public void setSelectionStart(long start) {
		this.start = start;
		this.midbyte = false;
		fireSelectionChanged(start, end);
	}

	public void setSelectionEnd(long end) {
		this.end = end;
		this.midbyte = false;
		fireSelectionChanged(start, end);
	}

	public void setSelectionRange(long start, long end) {
		this.start = start;
		this.end = end;
		this.midbyte = false;
		fireSelectionChanged(start, end);
	}

	public void setMidByte(boolean midbyte) {
		this.midbyte = midbyte;
		fireSelectionChanged(start, end);
	}

	private final List<ByteBufferSelectionListener> listeners = new ArrayList<ByteBufferSelectionListener>();

	public final void addSelectionListener(ByteBufferSelectionListener listener) {
		listeners.add(listener);
	}

	public final void removeSelectionListener(ByteBufferSelectionListener listener) {
		listeners.add(listener);
	}

	protected final void fireSelectionChanged(long start, long end) {
		for (ByteBufferSelectionListener l : listeners) {
			l.selectionChanged(this, start, end);
		}
	}
}
