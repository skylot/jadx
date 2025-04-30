package jadx.gui.ui.hexeditor.buffer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/*
 * code taken from https://github.com/kreativekorp/hexcellent
 */
public class CompositeByteBuffer extends ByteBuffer {
	private final List<ByteBuffer> buffers;

	public CompositeByteBuffer(ByteBuffer... buffers) {
		this(Arrays.asList(buffers));
	}

	public CompositeByteBuffer(Collection<? extends ByteBuffer> c) {
		this.buffers = new ArrayList<>();
		this.buffers.addAll(c);
	}

	public List<ByteBuffer> getBackingBufferList() {
		return this.buffers;
	}

	@Override
	public boolean isEmpty() {
		for (ByteBuffer buffer : buffers) {
			if (!buffer.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public long length() {
		long length = 0;
		for (ByteBuffer buffer : buffers) {
			length += buffer.length();
		}
		return length;
	}

	@Override
	public boolean get(long offset, byte[] dst, int dstOffset, int length) {
		if (length <= 0) {
			return true;
		}
		for (ByteBuffer buffer : buffers) {
			if (buffer.isEmpty()) {
				continue;
			}
			long len = buffer.length();
			if ((offset + length) <= len) {
				if (offset >= 0) {
					return buffer.get(offset, dst, dstOffset, length);
				} else {
					return buffer.get(0, dst, (int) (dstOffset - offset), (int) (length + offset));
				}
			}
			if (offset < len) {
				if (offset >= 0) {
					if (!buffer.get(offset, dst, dstOffset, (int) (len - offset))) {
						return false;
					}
				} else {
					if (!buffer.get(0, dst, (int) (dstOffset - offset), (int) len)) {
						return false;
					}
				}
			}
			offset -= len;
		}
		return false;
	}

	@Override
	public boolean insert(long offset, byte[] src, int srcOffset, int length) {
		if (length <= 0) {
			return true;
		}
		long off = offset;
		for (ByteBuffer buffer : buffers) {
			long len = buffer.length();
			if (off >= 0 && off <= len && buffer.insert(off, src, srcOffset, length)) {
				fireDataInserted(offset, length);
				return true;
			}
			off -= len;
		}
		List<ByteBuffer> newBuffers = new ArrayList<>();
		newBuffers.addAll(slice(0, offset).buffers);
		newBuffers.add(new ArrayByteBuffer(src, srcOffset, length));
		newBuffers.addAll(slice(offset, length() - offset).buffers);
		buffers.clear();
		buffers.addAll(newBuffers);
		fireDataInserted(offset, length);
		return true;
	}

	@Override
	public boolean overwrite(long offset, byte[] src, int srcOffset, int length) {
		if (length <= 0) {
			return true;
		}
		long off = offset;
		List<ByteBuffer> newBuffers = new ArrayList<>();
		for (ByteBuffer buffer : buffers) {
			if (buffer.isEmpty()) {
				continue;
			}
			long len = buffer.length();
			if ((off + length) <= len) {
				if (off >= 0) {
					if (buffer.overwrite(off, src, srcOffset, length)) {
						newBuffers.add(buffer);
					} else {
						if (off > 0) {
							newBuffers.add(buffer.slice(0, off));
						}
						newBuffers.add(new ArrayByteBuffer(src, srcOffset, length));
						long toff = off + length;
						long tlen = len - toff;
						if (tlen > 0) {
							newBuffers.add(buffer.slice(toff, tlen));
						}
					}
				} else if ((off + length) > 0) {
					if (buffer.overwrite(0, src, (int) (srcOffset - off), (int) (length + off))) {
						newBuffers.add(buffer);
					} else {
						newBuffers.add(new ArrayByteBuffer(src, (int) (srcOffset - off), (int) (length + off)));
						long toff = off + length;
						long tlen = len - toff;
						if (tlen > 0) {
							newBuffers.add(buffer.slice(toff, tlen));
						}
					}
				} else {
					newBuffers.add(buffer);
				}
			} else if (off < len) {
				if (off >= 0) {
					if (buffer.overwrite(off, src, srcOffset, (int) (len - off))) {
						newBuffers.add(buffer);
					} else {
						if (off > 0) {
							newBuffers.add(buffer.slice(0, off));
						}
						newBuffers.add(new ArrayByteBuffer(src, srcOffset, (int) (len - off)));
					}
				} else {
					if (buffer.overwrite(0, src, (int) (srcOffset - off), (int) len)) {
						newBuffers.add(buffer);
					} else {
						newBuffers.add(new ArrayByteBuffer(src, (int) (srcOffset - off), (int) len));
					}
				}
			} else {
				newBuffers.add(buffer);
			}
			off -= len;
		}
		buffers.clear();
		buffers.addAll(newBuffers);
		fireDataOverwritten(offset, length);
		return true;
	}

	@Override
	public boolean remove(long offset, long length) {
		if (length <= 0) {
			return true;
		}
		long off = offset;
		List<ByteBuffer> newBuffers = new ArrayList<ByteBuffer>();
		for (ByteBuffer buffer : buffers) {
			if (buffer.isEmpty()) {
				continue;
			}
			long len = buffer.length();
			if ((off + length) <= len) {
				if (off >= 0) {
					if (buffer.remove(off, length)) {
						if (!buffer.isEmpty()) {
							newBuffers.add(buffer);
						}
					} else {
						if (off > 0) {
							newBuffers.add(buffer.slice(0, off));
						}
						long toff = off + length;
						long tlen = len - toff;
						if (tlen > 0) {
							newBuffers.add(buffer.slice(toff, tlen));
						}
					}
				} else if ((off + length) > 0) {
					if (buffer.remove(0, length + off)) {
						if (!buffer.isEmpty()) {
							newBuffers.add(buffer);
						}
					} else {
						long toff = off + length;
						long tlen = len - toff;
						if (tlen > 0) {
							newBuffers.add(buffer.slice(toff, tlen));
						}
					}
				} else {
					newBuffers.add(buffer);
				}
			} else if (off < len) {
				if (off >= 0) {
					if (buffer.remove(off, len - off)) {
						if (!buffer.isEmpty()) {
							newBuffers.add(buffer);
						}
					} else {
						if (off > 0) {
							newBuffers.add(buffer.slice(0, off));
						}
					}
				}
			} else {
				newBuffers.add(buffer);
			}
			off -= len;
		}
		buffers.clear();
		buffers.addAll(newBuffers);
		fireDataRemoved(offset, length);
		return true;
	}

	@Override
	public CompositeByteBuffer slice(long offset, long length) {
		CompositeByteBuffer newBuffer = new CompositeByteBuffer();
		if (length <= 0) {
			return newBuffer;
		}
		for (ByteBuffer buffer : buffers) {
			if (buffer.isEmpty()) {
				continue;
			}
			long len = buffer.length();
			if ((offset + length) <= len) {
				if (offset >= 0) {
					newBuffer.buffers.add(buffer.slice(offset, length));
					break;
				} else {
					newBuffer.buffers.add(buffer.slice(0, length + offset));
					break;
				}
			}
			if (offset < len) {
				if (offset >= 0) {
					newBuffer.buffers.add(buffer.slice(offset, len - offset));
				} else {
					newBuffer.buffers.add(buffer.slice(0, len));
				}
			}
			offset -= len;
		}
		return newBuffer;
	}

	@Override
	public boolean write(OutputStream out, long offset, long length) throws IOException {
		if (length <= 0) {
			return true;
		}
		for (ByteBuffer buffer : buffers) {
			if (buffer.isEmpty()) {
				continue;
			}
			long len = buffer.length();
			if ((offset + length) <= len) {
				if (offset >= 0) {
					return buffer.write(out, offset, length);
				} else {
					return buffer.write(out, 0, length + offset);
				}
			}
			if (offset < len) {
				if (offset >= 0) {
					if (!buffer.write(out, offset, len - offset)) {
						return false;
					}
				} else {
					if (!buffer.write(out, 0, len)) {
						return false;
					}
				}
			}
			offset -= len;
		}
		return false;
	}
}
