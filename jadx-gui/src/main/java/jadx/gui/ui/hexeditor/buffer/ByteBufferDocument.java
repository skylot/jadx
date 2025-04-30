package jadx.gui.ui.hexeditor.buffer;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/*
 * code taken from https://github.com/kreativekorp/hexcellent
 */
public class ByteBufferDocument {
	private final ByteBuffer buffer;
	private final ByteBufferSelectionModel selection;
	private final ByteBufferHistory history;

	public ByteBufferDocument(ByteBuffer buffer) {
		this.buffer = buffer;
		this.selection = new ByteBufferSelectionModel();
		this.history = new ByteBufferHistory();
	}

	public ByteBuffer getByteBuffer() {
		return buffer;
	}

	public boolean isEmpty() {
		return buffer.isEmpty();
	}

	public long length() {
		return buffer.length();
	}

	public boolean get(long offset, byte[] dst, int dstOffset, int length) {
		return buffer.get(offset, dst, dstOffset, length);
	}

	public ByteBuffer slice(long offset, long length) {
		return buffer.slice(offset, length);
	}

	public boolean write(OutputStream out, long offset, long length) throws IOException {
		return buffer.write(out, offset, length);
	}

	public long indexOf(byte[] pattern) {
		return buffer.indexOf(pattern);
	}

	public long indexOf(byte[] pattern, long index) {
		return buffer.indexOf(pattern, index);
	}

	public long lastIndexOf(byte[] pattern) {
		return buffer.lastIndexOf(pattern);
	}

	public long lastIndexOf(byte[] pattern, long index) {
		return buffer.lastIndexOf(pattern, index);
	}

	public void addByteBufferListener(ByteBufferListener l) {
		buffer.addByteBufferListener(l);
	}

	public void removeByteBufferListener(ByteBufferListener l) {
		buffer.removeByteBufferListener(l);
	}

	public ByteBufferSelectionModel getSelectionModel() {
		return selection;
	}

	public long getSelectionStart() {
		return selection.getSelectionStart();
	}

	public long getSelectionEnd() {
		return selection.getSelectionEnd();
	}

	public long getSelectionMin() {
		return selection.getSelectionMin();
	}

	public long getSelectionMax() {
		return selection.getSelectionMax();
	}

	public long getSelectionLength() {
		return selection.getSelectionLength();
	}

	public boolean isMidByte() {
		return selection.isMidByte();
	}

	public void setSelectionStart(long start) {
		selection.setSelectionStart(start);
	}

	public void setSelectionEnd(long end) {
		selection.setSelectionEnd(end);
	}

	public void setSelectionRange(long start, long end) {
		selection.setSelectionRange(start, end);
	}

	public void setMidByte(boolean midbyte) {
		selection.setMidByte(midbyte);
	}

	public void selectAll() {
		selection.setSelectionRange(0, buffer.length());
	}

	public void addSelectionListener(ByteBufferSelectionListener l) {
		selection.addSelectionListener(l);
	}

	public void removeSelectionListener(ByteBufferSelectionListener l) {
		selection.removeSelectionListener(l);
	}

	public ByteBufferHistory getHistory() {
		return history;
	}

	public boolean canUndo() {
		return history.canUndo();
	}

	public boolean canRedo() {
		return history.canRedo();
	}

	public ByteBufferAction getUndoAction() {
		return history.getUndoAction();
	}

	public ByteBufferAction getRedoAction() {
		return history.getRedoAction();
	}

	public String getUndoActionName() {
		return history.getUndoActionName();
	}

	public String getRedoActionName() {
		return history.getRedoActionName();
	}

	public void undo() {
		history.undo();
	}

	public void redo() {
		history.redo();
	}

	public void clearHistory() {
		history.clear();
	}

	public byte[] getSelection() {
		long length = selection.getSelectionLength();
		if (length <= 0 || length >= Integer.MAX_VALUE) {
			return null;
		}
		long offset = selection.getSelectionMin();
		byte[] data = new byte[(int) length];
		buffer.get(offset, data, 0, (int) length);
		return data;
	}

	public String getSelectionAsHex() {
		byte[] data = getSelection();
		if (data == null) {
			return null;
		}
		if (data.length == 0) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < data.length; i++) {
			sb.append(String.format("%02X", data[i] & 0xFF));
			if (i < data.length - 1) {
				sb.append(' ');
			}
		}
		return sb.toString();
	}

	public String getSelectionAsString(String charset) {
		byte[] data = getSelection();
		if (data == null) {
			return null;
		}
		try {
			return new String(data, charset);
		} catch (IOException e) {
			return null;
		}
	}

	private class DeleteSelectionAction extends ByteBufferAction {
		private final long selectionStart;
		private final long selectionEnd;
		private final long offset;
		private final byte[] removed;

		public DeleteSelectionAction(String name) {
			super(name);
			this.selectionStart = selection.getSelectionStart();
			this.selectionEnd = selection.getSelectionEnd();
			this.offset = Math.min(selectionStart, selectionEnd);
			int length = (int) Math.abs(selectionStart - selectionEnd);
			this.removed = new byte[length];
			buffer.get(offset, removed, 0, length);
		}

		@Override
		public void redo() {
			buffer.remove(offset, removed.length);
			selection.setSelectionRange(offset, offset);
		}

		@Override
		public void undo() {
			buffer.insert(offset, removed, 0, removed.length);
			selection.setSelectionRange(selectionStart, selectionEnd);
		}
	}

	public boolean deleteSelection(String actionName) {
		long length = selection.getSelectionLength();
		if (length <= 0 || length >= Integer.MAX_VALUE) {
			return false;
		}
		ByteBufferAction a = new DeleteSelectionAction(actionName);
		a.redo();
		history.add(a);
		return true;
	}

	private class ReplaceSelectionAction extends ByteBufferAction {
		private final long selectionStart;
		private final long selectionEnd;
		private final long offset;
		private final byte[] removed;
		private final byte[] inserted;
		private final boolean keepSelected;

		public ReplaceSelectionAction(String name, byte[] data, boolean keepSelected) {
			super(name);
			this.selectionStart = selection.getSelectionStart();
			this.selectionEnd = selection.getSelectionEnd();
			this.offset = Math.min(selectionStart, selectionEnd);
			int length = (int) Math.abs(selectionStart - selectionEnd);
			this.removed = new byte[length];
			buffer.get(offset, removed, 0, length);
			this.inserted = data;
			this.keepSelected = keepSelected;
		}

		@Override
		public void redo() {
			buffer.remove(offset, removed.length);
			buffer.insert(offset, inserted, 0, inserted.length);
			if (keepSelected) {
				selection.setSelectionRange(offset, offset + inserted.length);
			} else {
				selection.setSelectionRange(offset + inserted.length, offset + inserted.length);
			}
		}

		@Override
		public void undo() {
			buffer.remove(offset, inserted.length);
			buffer.insert(offset, removed, 0, removed.length);
			selection.setSelectionRange(selectionStart, selectionEnd);
		}
	}

	public boolean replaceSelection(String actionName, byte[] data, boolean keepSelected) {
		long length = selection.getSelectionLength();
		if (length >= Integer.MAX_VALUE) {
			return false;
		}
		ByteBufferAction a = new ReplaceSelectionAction(actionName, data, keepSelected);
		a.redo();
		history.add(a);
		return true;
	}

	public boolean transformSelection(ByteTransform tx) {
		long length = selection.getSelectionLength();
		if (length <= 0 || length >= Integer.MAX_VALUE) {
			return false;
		}
		long offset = selection.getSelectionMin();
		byte[] data = new byte[(int) length];
		if (!buffer.get(offset, data, 0, (int) length)) {
			return false;
		}
		if (!tx.transform(data, 0, (int) length)) {
			return false;
		}
		ByteBufferAction a = new ReplaceSelectionAction(tx.getName(), data, true);
		a.redo();
		history.add(a);
		return true;
	}

	private class ReplaceAllAction extends ByteBufferAction {
		private final long selectionStart;
		private final long selectionEnd;
		private final byte[] pattern;
		private final byte[] replacement;
		private final List<Long> offsets;

		public ReplaceAllAction(byte[] pattern, byte[] replacement) {
			super("Replace All");
			this.selectionStart = selection.getSelectionStart();
			this.selectionEnd = selection.getSelectionEnd();
			this.pattern = pattern;
			this.replacement = replacement;
			this.offsets = new ArrayList<>();
		}

		@Override
		public void redo() {
			long ss = selectionStart;
			long se = selectionEnd;
			if (offsets.isEmpty()) {
				long o = buffer.indexOf(pattern);
				while (o >= 0) {
					if (!buffer.remove(o, pattern.length)) {
						break;
					}
					if (!buffer.insert(o, replacement, 0, replacement.length)) {
						break;
					}
					offsets.add(o);
					se = o + replacement.length;
					ss = se;
					o = buffer.indexOf(pattern, ss);
				}
			} else {
				for (long o : offsets) {
					if (!buffer.remove(o, pattern.length)) {
						break;
					}
					if (!buffer.insert(o, replacement, 0, replacement.length)) {
						break;
					}
					se = o + replacement.length;
					ss = se;
				}
			}
			selection.setSelectionRange(ss, se);
		}

		@Override
		public void undo() {
			for (int i = offsets.size() - 1; i >= 0; i--) {
				long o = offsets.get(i);
				buffer.remove(o, replacement.length);
				buffer.insert(o, pattern, 0, pattern.length);
			}
			selection.setSelectionRange(selectionStart, selectionEnd);
		}
	}

	public boolean replaceAll(byte[] pattern, byte[] replacement) {
		if (pattern.length == 0) {
			return false;
		}
		ReplaceAllAction a = new ReplaceAllAction(pattern, replacement);
		a.redo();
		if (a.offsets.isEmpty()) {
			return false;
		}
		history.add(a);
		return true;
	}

	public boolean cutAsHex() {
		return copyAsHex() && deleteSelection("Cut");
	}

	public boolean cutAsString(String charset) {
		return copyAsString(charset) && deleteSelection("Cut");
	}

	public boolean copyAsHex() {
		String s = getSelectionAsHex();
		if (s == null) {
			return false;
		}
		Toolkit tk = Toolkit.getDefaultToolkit();
		Clipboard cb = tk.getSystemClipboard();
		StringSelection ss = new StringSelection(s);
		cb.setContents(ss, OWNER);
		return true;
	}

	public boolean copyAsString(String charset) {
		String s = getSelectionAsString(charset);
		if (s == null) {
			return false;
		}
		Toolkit tk = Toolkit.getDefaultToolkit();
		Clipboard cb = tk.getSystemClipboard();
		StringSelection ss = new StringSelection(s);
		cb.setContents(ss, OWNER);
		return true;
	}

	public boolean copyOffset(boolean decimalAddresses) {
		String s = addressString(getSelectionStart(), decimalAddresses);
		Toolkit tk = Toolkit.getDefaultToolkit();
		Clipboard cb = tk.getSystemClipboard();
		StringSelection ss = new StringSelection(s);
		cb.setContents(ss, OWNER);
		return true;
	}

	public String addressString(long address, boolean decimalAddresses) {
		return decimalAddresses
				? String.valueOf(address)
				: String.format("%08X", address);
	}

	public boolean pasteAsHex() {
		String s = getClipboardString();
		if (s == null) {
			return false;
		}
		byte[] data = decodeHex(s);
		if (data == null) {
			return false;
		}
		return replaceSelection("Paste", data, false);
	}

	public boolean pasteAsString(String charset) {
		String s = getClipboardString();
		if (s == null) {
			return false;
		}
		try {
			return replaceSelection("Paste", s.getBytes(charset), false);
		} catch (IOException e) {
			return false;
		}
	}

	private class KeyboardDeleteAction extends ByteBufferAction {
		private final long origin;
		private long offset;
		private byte[] removed;

		public KeyboardDeleteAction(boolean forward) {
			super("Delete");
			this.origin = selection.getSelectionStart();
			this.offset = forward ? origin : (origin - 1);
			this.removed = new byte[1];
			buffer.get(offset, removed, 0, 1);
		}

		public boolean prepend() {
			if (selection.getSelectionStart() != this.offset) {
				return false;
			}
			if (selection.getSelectionEnd() != this.offset) {
				return false;
			}
			this.offset--;
			//
			byte[] newRemoved = new byte[removed.length + 1];
			buffer.get(offset, newRemoved, 0, 1);
			System.arraycopy(removed, 0, newRemoved, 1, removed.length);
			//
			this.removed = newRemoved;
			return true;
		}

		public boolean append() {
			if (selection.getSelectionStart() != this.offset) {
				return false;
			}
			if (selection.getSelectionEnd() != this.offset) {
				return false;
			}
			//
			byte[] newRemoved = new byte[removed.length + 1];
			System.arraycopy(removed, 0, newRemoved, 0, removed.length);
			buffer.get(offset, newRemoved, removed.length, 1);
			//
			this.removed = newRemoved;
			return true;
		}

		@Override
		public void redo() {
			buffer.remove(offset, removed.length);
			selection.setSelectionRange(offset, offset);
		}

		@Override
		public void undo() {
			buffer.insert(offset, removed, 0, removed.length);
			selection.setSelectionRange(origin, origin);
		}
	}

	public boolean deleteBackward() {
		if (selection.getSelectionLength() > 0) {
			return deleteSelection("Delete");
		} else {
			long offset = selection.getSelectionStart();
			if (offset <= 0) {
				return false;
			}
			ByteBufferAction a = history.getUndoAction();
			if (!(a instanceof KeyboardDeleteAction && ((KeyboardDeleteAction) a).prepend())) {
				history.add(new KeyboardDeleteAction(false));
			}
			buffer.remove(offset - 1, 1);
			selection.setSelectionRange(offset - 1, offset - 1);
			return true;
		}
	}

	public boolean deleteForward() {
		if (selection.getSelectionLength() > 0) {
			return deleteSelection("Delete");
		} else {
			long offset = selection.getSelectionStart();
			if (offset >= buffer.length()) {
				return false;
			}
			ByteBufferAction a = history.getUndoAction();
			if (!(a instanceof KeyboardDeleteAction && ((KeyboardDeleteAction) a).append())) {
				history.add(new KeyboardDeleteAction(true));
			}
			buffer.remove(offset, 1);
			selection.setSelectionRange(offset, offset);
			return true;
		}
	}

	private class KeyboardInsertAction extends ByteBufferAction {
		private final long selectionStart;
		private final long selectionEnd;
		private final long offset;
		private final byte[] removed;
		private byte[] inserted;

		public KeyboardInsertAction(byte[] data) {
			super("Insert");
			this.selectionStart = selection.getSelectionStart();
			this.selectionEnd = selection.getSelectionEnd();
			this.offset = Math.min(selectionStart, selectionEnd);
			int length = (int) Math.abs(selectionStart - selectionEnd);
			this.removed = new byte[length];
			buffer.get(offset, removed, 0, length);
			this.inserted = data;
		}

		public boolean append(byte[] data) {
			if (selection.getSelectionStart() != offset + inserted.length) {
				return false;
			}
			if (selection.getSelectionEnd() != offset + inserted.length) {
				return false;
			}
			//
			byte[] newInserted = new byte[inserted.length + data.length];
			System.arraycopy(inserted, 0, newInserted, 0, inserted.length);
			System.arraycopy(data, 0, newInserted, inserted.length, data.length);
			//
			this.inserted = newInserted;
			return true;
		}

		public boolean shiftIn(int nybble) {
			if (selection.getSelectionStart() != offset + inserted.length) {
				return false;
			}
			if (selection.getSelectionEnd() != offset + inserted.length) {
				return false;
			}
			if (selection.isMidByte() && inserted.length > 0) {
				inserted[inserted.length - 1] <<= 4;
				inserted[inserted.length - 1] |= (byte) nybble;
				return true;
			}
			return false;
		}

		@Override
		public void redo() {
			buffer.remove(offset, removed.length);
			buffer.insert(offset, inserted, 0, inserted.length);
			selection.setSelectionRange(offset + inserted.length, offset + inserted.length);
		}

		@Override
		public void undo() {
			buffer.remove(offset, inserted.length);
			buffer.insert(offset, removed, 0, removed.length);
			selection.setSelectionRange(selectionStart, selectionEnd);
		}
	}

	public boolean insert(byte[] data) {
		long length = selection.getSelectionLength();
		if (length >= Integer.MAX_VALUE) {
			return false;
		}
		ByteBufferAction a = history.getUndoAction();
		if (a instanceof KeyboardInsertAction) {
			KeyboardInsertAction ia = (KeyboardInsertAction) a;
			if (ia.append(data)) {
				long offset = selection.getSelectionStart();
				buffer.insert(offset, data, 0, data.length);
				selection.setSelectionRange(offset + data.length, offset + data.length);
				return true;
			}
		}
		a = new KeyboardInsertAction(data);
		a.redo();
		history.add(a);
		return true;
	}

	public boolean insert(int nybble) {
		long length = selection.getSelectionLength();
		if (length >= Integer.MAX_VALUE) {
			return false;
		}
		ByteBufferAction a = history.getUndoAction();
		if (a instanceof KeyboardInsertAction) {
			KeyboardInsertAction ia = (KeyboardInsertAction) a;
			if (ia.shiftIn(nybble)) {
				long offset = selection.getSelectionStart();
				byte[] tmp = new byte[1];
				buffer.get(offset - 1, tmp, 0, 1);
				tmp[0] <<= 4;
				tmp[0] |= (byte) nybble;
				buffer.overwrite(offset - 1, tmp, 0, 1);
				selection.setMidByte(false);
				return true;
			}
		}
		if (insert(new byte[] { (byte) nybble })) {
			selection.setMidByte(true);
			return true;
		}
		return false;
	}

	private class KeyboardOverwriteAction extends ByteBufferAction {
		private final long selectionStart;
		private final long selectionEnd;
		private final long offset;
		private byte[] removed;
		private byte[] inserted;

		public KeyboardOverwriteAction(byte[] data) {
			super("Overwrite");
			this.selectionStart = selection.getSelectionStart();
			this.selectionEnd = selection.getSelectionEnd();
			this.offset = Math.min(selectionStart, selectionEnd);
			int slength = (int) Math.abs(selectionStart - selectionEnd);
			long tlength = buffer.length() - Math.max(selectionStart, selectionEnd);
			int rlength = slength + (int) Math.min(data.length, tlength);
			this.removed = new byte[rlength];
			buffer.get(offset, removed, 0, rlength);
			this.inserted = data;
		}

		public boolean append(byte[] data) {
			if (selection.getSelectionStart() != offset + inserted.length) {
				return false;
			}
			if (selection.getSelectionEnd() != offset + inserted.length) {
				return false;
			}
			//
			long tlength = buffer.length() - (offset + inserted.length);
			int ralength = (int) Math.min(data.length, tlength);
			byte[] newRemoved = new byte[removed.length + ralength];
			System.arraycopy(removed, 0, newRemoved, 0, removed.length);
			buffer.get(offset + inserted.length, newRemoved, removed.length, ralength);
			//
			byte[] newInserted = new byte[inserted.length + data.length];
			System.arraycopy(inserted, 0, newInserted, 0, inserted.length);
			System.arraycopy(data, 0, newInserted, inserted.length, data.length);
			//
			this.removed = newRemoved;
			this.inserted = newInserted;
			return true;
		}

		public boolean shiftIn(int nybble) {
			if (selection.getSelectionStart() != offset + inserted.length) {
				return false;
			}
			if (selection.getSelectionEnd() != offset + inserted.length) {
				return false;
			}
			if (selection.isMidByte() && inserted.length > 0) {
				inserted[inserted.length - 1] <<= 4;
				inserted[inserted.length - 1] |= (byte) nybble;
				return true;
			}
			return false;
		}

		@Override
		public void redo() {
			buffer.remove(offset, removed.length);
			buffer.insert(offset, inserted, 0, inserted.length);
			selection.setSelectionRange(offset + inserted.length, offset + inserted.length);
		}

		@Override
		public void undo() {
			buffer.remove(offset, inserted.length);
			buffer.insert(offset, removed, 0, removed.length);
			selection.setSelectionRange(selectionStart, selectionEnd);
		}
	}

	public boolean overwrite(byte[] data) {
		long length = selection.getSelectionLength();
		if (length >= Integer.MAX_VALUE) {
			return false;
		}
		ByteBufferAction a = history.getUndoAction();
		if (a instanceof KeyboardOverwriteAction) {
			KeyboardOverwriteAction oa = (KeyboardOverwriteAction) a;
			if (oa.append(data)) {
				long offset = selection.getSelectionStart();
				long tlength = buffer.length() - offset;
				int ralength = (int) Math.min(data.length, tlength);
				buffer.remove(offset, ralength);
				buffer.insert(offset, data, 0, data.length);
				selection.setSelectionRange(offset + data.length, offset + data.length);
				return true;
			}
		}
		a = new KeyboardOverwriteAction(data);
		a.redo();
		history.add(a);
		return true;
	}

	public boolean overwrite(int nybble) {
		long length = selection.getSelectionLength();
		if (length >= Integer.MAX_VALUE) {
			return false;
		}
		ByteBufferAction a = history.getUndoAction();
		if (a instanceof KeyboardOverwriteAction) {
			KeyboardOverwriteAction oa = (KeyboardOverwriteAction) a;
			if (oa.shiftIn(nybble)) {
				long offset = selection.getSelectionStart();
				byte[] tmp = new byte[1];
				buffer.get(offset - 1, tmp, 0, 1);
				tmp[0] <<= 4;
				tmp[0] |= (byte) nybble;
				buffer.overwrite(offset - 1, tmp, 0, 1);
				selection.setMidByte(false);
				return true;
			}
		}
		if (overwrite(new byte[] { (byte) nybble })) {
			selection.setMidByte(true);
			return true;
		}
		return false;
	}

	private static String getClipboardString() {
		try {
			Toolkit tk = Toolkit.getDefaultToolkit();
			Clipboard cb = tk.getSystemClipboard();
			return cb.getData(DataFlavor.stringFlavor).toString();
		} catch (Exception e) {
			return null;
		}
	}

	private static boolean isHexDigit(char ch) {
		return ((ch >= '0' && ch <= '9')
				|| (ch >= 'A' && ch <= 'F')
				|| (ch >= 'a' && ch <= 'f'));
	}

	private static int hexDigitValue(char ch) {
		return ((ch >= '0' && ch <= '9') ? (ch - '0') : (ch >= 'A' && ch <= 'F') ? (ch - 'A' + 10)
				: (ch >= 'a' && ch <= 'f') ? (ch - 'a' + 10) : (-1));
	}

	private static byte[] decodeHex(String s) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		char[] ch = s.toCharArray();
		int i = 0;
		int n = ch.length;
		for (;;) {
			while (i < n && Character.isWhitespace(ch[i])) {
				i++;
				if (i < n) {
					if (isHexDigit(ch[i])) {
						int v = hexDigitValue(ch[i]) << 4;
						i++;
						if (i < n) {
							if (isHexDigit(ch[i])) {
								v |= hexDigitValue(ch[i]);
								i++;
								out.write(v);
								continue;
							}
						}
					}
					return null;
				} else {
					return out.toByteArray();
				}
			}
		}
	}

	private static final ClipboardOwner OWNER = (cb, t) -> {
		// Nothing.
	};
}
