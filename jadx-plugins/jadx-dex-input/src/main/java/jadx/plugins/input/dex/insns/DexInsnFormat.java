package jadx.plugins.input.dex.insns;

import jadx.api.plugins.input.insns.custom.impl.SwitchPayload;
import jadx.plugins.input.dex.DexException;
import jadx.plugins.input.dex.insns.payloads.DexArrayPayload;
import jadx.plugins.input.dex.sections.SectionReader;

public abstract class DexInsnFormat {
	public static final DexInsnFormat FORMAT_10X = new DexInsnFormat(1, 0) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			// no op
		}
	};

	public static final DexInsnFormat FORMAT_12X = new DexInsnFormat(1, 2) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			int[] regs = insn.getArgsReg();
			regs[0] = nibble2(opcodeUnit);
			regs[1] = nibble3(opcodeUnit);
		}
	};

	public static final DexInsnFormat FORMAT_11N = new DexInsnFormat(1, 1) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			int[] regs = insn.getArgsReg();
			regs[0] = nibble2(opcodeUnit);
			insn.setLiteral(signedNibble3(opcodeUnit));
		}
	};

	public static final DexInsnFormat FORMAT_11X = new DexInsnFormat(1, 1) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			int[] regs = insn.getArgsReg();
			regs[0] = byte1(opcodeUnit);
		}
	};

	public static final DexInsnFormat FORMAT_10T = new DexInsnFormat(1, 0) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			insn.setTarget(insn.getOffset() + signedByte1(opcodeUnit));
		}
	};

	public static final DexInsnFormat FORMAT_20T = new DexInsnFormat(2, 0) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			insn.setTarget(insn.getOffset() + in.readShort());
		}
	};

	public static final DexInsnFormat FORMAT_20BC = new DexInsnFormat(2, 0) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			insn.setLiteral(byte1(opcodeUnit));
			insn.setIndex(in.readUShort());
		}
	};

	public static final DexInsnFormat FORMAT_22X = new DexInsnFormat(2, 2) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			int[] regs = insn.getArgsReg();
			regs[0] = byte1(opcodeUnit);
			regs[1] = in.readUShort();
		}
	};

	public static final DexInsnFormat FORMAT_21T = new DexInsnFormat(2, 1) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			int[] regs = insn.getArgsReg();
			regs[0] = signedByte1(opcodeUnit);
			insn.setTarget(insn.getOffset() + in.readShort());
		}
	};

	public static final DexInsnFormat FORMAT_21S = new DexInsnFormat(2, 1) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			int[] regs = insn.getArgsReg();
			regs[0] = byte1(opcodeUnit);
			insn.setLiteral(in.readShort());
		}
	};

	public static final DexInsnFormat FORMAT_21H = new DexInsnFormat(2, 1) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			int[] regs = insn.getArgsReg();
			regs[0] = byte1(opcodeUnit);

			long literal = in.readShort();
			literal <<= (byte0(opcodeUnit) == DexOpcodes.CONST_HIGH16) ? 16 : 48;
			insn.setLiteral(literal);
		}
	};

	public static final DexInsnFormat FORMAT_21C = new DexInsnFormat(2, 1) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			int[] regs = insn.getArgsReg();
			regs[0] = byte1(opcodeUnit);
			insn.setIndex(in.readUShort());
		}
	};

	public static final DexInsnFormat FORMAT_23X = new DexInsnFormat(2, 3) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			int[] regs = insn.getArgsReg();
			regs[0] = byte1(opcodeUnit);
			int next = in.readUShort();
			regs[1] = byte0(next);
			regs[2] = byte1(next);
		}
	};

	public static final DexInsnFormat FORMAT_22B = new DexInsnFormat(2, 2) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			int[] regs = insn.getArgsReg();
			regs[0] = byte1(opcodeUnit);
			int next = in.readUShort();
			regs[1] = byte0(next);
			insn.setLiteral(signedByte1(next));
		}
	};

	public static final DexInsnFormat FORMAT_22T = new DexInsnFormat(2, 2) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			int[] regs = insn.getArgsReg();
			regs[0] = nibble2(opcodeUnit);
			regs[1] = nibble3(opcodeUnit);
			insn.setTarget(insn.getOffset() + in.readShort());
		}
	};

	public static final DexInsnFormat FORMAT_22S = new DexInsnFormat(2, 2) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			int[] regs = insn.getArgsReg();
			regs[0] = nibble2(opcodeUnit);
			regs[1] = nibble3(opcodeUnit);
			insn.setLiteral(in.readShort());
		}
	};

	public static final DexInsnFormat FORMAT_22C = new DexInsnFormat(2, 2) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			int[] regs = insn.getArgsReg();
			regs[0] = nibble2(opcodeUnit);
			regs[1] = nibble3(opcodeUnit);
			insn.setIndex(in.readUShort());
			insn.setLiteral(0L);
		}
	};

	public static final DexInsnFormat FORMAT_22CS = FORMAT_22C;

	public static final DexInsnFormat FORMAT_30T = new DexInsnFormat(3, 0) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			insn.setTarget(insn.getOffset() + in.readInt());
		}
	};

	public static final DexInsnFormat FORMAT_32X = new DexInsnFormat(3, 2) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			int[] regs = insn.getArgsReg();
			regs[0] = in.readUShort();
			regs[1] = in.readUShort();
		}
	};

	public static final DexInsnFormat FORMAT_31I = new DexInsnFormat(3, 1) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			int[] regs = insn.getArgsReg();
			regs[0] = byte1(opcodeUnit);
			insn.setLiteral(in.readInt());
		}
	};

	public static final DexInsnFormat FORMAT_31T = new DexInsnFormat(3, 1) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			int[] regs = insn.getArgsReg();
			regs[0] = byte1(opcodeUnit);
			insn.setTarget(insn.getOffset() + in.readInt());
		}
	};

	public static final DexInsnFormat FORMAT_31C = new DexInsnFormat(3, 1) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			int[] regs = insn.getArgsReg();
			regs[0] = byte1(opcodeUnit);
			insn.setIndex(in.readInt());
		}
	};

	public static final DexInsnFormat FORMAT_35C = new DexInsnFormat(3, -1) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			readRegsList(insn, opcodeUnit, in);
		}
	};

	public static final DexInsnFormat FORMAT_35MS = FORMAT_35C;
	public static final DexInsnFormat FORMAT_35MI = FORMAT_35C;

	public static final DexInsnFormat FORMAT_3RC = new DexInsnFormat(3, -1) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			readRegsRange(insn, opcodeUnit, in);
		}
	};

	public static final DexInsnFormat FORMAT_3RMS = FORMAT_3RC;
	public static final DexInsnFormat FORMAT_3RMI = FORMAT_3RC;

	public static final DexInsnFormat FORMAT_45CC = new DexInsnFormat(4, -1) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			readRegsList(insn, opcodeUnit, in);
			insn.setTarget(in.readUShort());
		}
	};

	public static final DexInsnFormat FORMAT_4RCC = new DexInsnFormat(4, -1) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			readRegsRange(insn, opcodeUnit, in);
			insn.setTarget(in.readUShort());
		}
	};

	public static final DexInsnFormat FORMAT_51I = new DexInsnFormat(5, 1) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			int[] regs = insn.getArgsReg();
			regs[0] = byte1(opcodeUnit);
			insn.setLiteral(in.readLong());
		}
	};

	public static final DexInsnFormat FORMAT_PACKED_SWITCH_PAYLOAD = new DexInsnFormat(-1, -1) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			int size = in.readUShort();
			int firstKey = in.readInt();
			int[] keys = new int[size];
			int[] targets = new int[size];
			for (int i = 0; i < size; i++) {
				targets[i] = in.readInt();
				keys[i] = firstKey + i;
			}
			insn.setPayload(new SwitchPayload(size, keys, targets));
			insn.setLength(size * 2 + 4);
		}

		@Override
		public void skip(DexInsnData insn, SectionReader in) {
			int size = in.readUShort();
			in.skip(4 + size * 4);
			insn.setLength(size * 2 + 4);
		}
	};

	public static final DexInsnFormat FORMAT_SPARSE_SWITCH_PAYLOAD = new DexInsnFormat(-1, -1) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			int size = in.readUShort();
			int[] keys = new int[size];
			for (int i = 0; i < size; i++) {
				keys[i] = in.readInt();
			}
			int[] targets = new int[size];
			for (int i = 0; i < size; i++) {
				targets[i] = in.readInt();
			}
			insn.setPayload(new SwitchPayload(size, keys, targets));
			insn.setLength(size * 4 + 2);
		}

		@Override
		public void skip(DexInsnData insn, SectionReader in) {
			int size = in.readUShort();
			in.skip(size * 8);
			insn.setLength(size * 4 + 2);
		}
	};

	public static final DexInsnFormat FORMAT_FILL_ARRAY_DATA_PAYLOAD = new DexInsnFormat(-1, -1) {
		@Override
		public void decode(DexInsnData insn, int opcodeUnit, SectionReader in) {
			int elemSize = in.readUShort();
			int size = in.readInt();
			Object data;
			switch (elemSize) {
				case 1: {
					data = in.readByteArray(size);
					if (size % 2 != 0) {
						in.readUByte();
					}
					break;
				}
				case 2: {
					short[] array = new short[size];
					for (int i = 0; i < size; i++) {
						array[i] = (short) in.readShort();
					}
					data = array;
					break;
				}
				case 4: {
					int[] array = new int[size];
					for (int i = 0; i < size; i++) {
						array[i] = in.readInt();
					}
					data = array;
					break;
				}
				case 8: {
					long[] array = new long[size];
					for (int i = 0; i < size; i++) {
						array[i] = in.readLong();
					}
					data = array;
					break;
				}
				case 0: {
					data = new byte[0];
					break;
				}
				default:
					throw new DexException("Unexpected element size in FILL_ARRAY_DATA_PAYLOAD: " + elemSize);
			}
			insn.setLength((size * elemSize + 1) / 2 + 4);
			insn.setPayload(new DexArrayPayload(size, elemSize, data));
		}

		@Override
		public void skip(DexInsnData insn, SectionReader in) {
			int elemSize = in.readUShort();
			int size = in.readInt();
			if (elemSize == 1) {
				in.skip(size + size % 2);
			} else {
				in.skip(size * elemSize);
			}
			insn.setLength((size * elemSize + 1) / 2 + 4);
		}
	};

	protected void readRegsList(DexInsnData insn, int opcodeUnit, SectionReader in) {
		int regsCount1 = nibble3(opcodeUnit);
		int index = in.readUShort();
		int rs = in.readUShort();

		int[] regs = insn.getArgsReg();
		regs[0] = nibble0(rs);
		regs[1] = nibble1(rs);
		regs[2] = nibble2(rs);
		regs[3] = nibble3(rs);
		regs[4] = nibble2(opcodeUnit);

		insn.setRegsCount(regsCount1);
		insn.setIndex(index);
	}

	protected void readRegsRange(DexInsnData insn, int opcodeUnit, SectionReader in) {
		int regsCount = byte1(opcodeUnit);
		int index = in.readUShort();
		int startReg = in.readUShort();

		int[] regs = insn.getArgsReg();
		if (regs.length < regsCount) {
			regs = new int[regsCount];
			insn.setArgsReg(regs);
		}
		int regNum = startReg;
		for (int i = 0; i < regsCount; i++) {
			regs[i] = regNum;
			regNum++;
		}
		insn.setRegsCount(regsCount);
		insn.setIndex(index);
	}

	private final int length;
	private final int regsCount;

	protected DexInsnFormat(int length, int regsCount) {
		this.length = length;
		this.regsCount = regsCount;
	}

	public abstract void decode(DexInsnData insn, int opcodeUnit, SectionReader in);

	public void skip(DexInsnData insn, SectionReader in) {
		int len = this.length;
		if (len == 1) {
			return;
		}
		in.skip((len - 1) * 2);
	}

	public int getLength() {
		return length;
	}

	public int getRegsCount() {
		return regsCount;
	}

	private static int byte0(int value) {
		return value & 0xFF;
	}

	private static int byte1(int value) {
		return (value >> 8) & 0xFF;
	}

	private static int signedByte1(int value) {
		return (byte) (value >> 8);
	}

	private static int nibble0(int value) {
		return value & 0xF;
	}

	private static int nibble1(int value) {
		return (value >> 4) & 0xF;
	}

	private static int nibble2(int value) {
		return (value >> 8) & 0xF;
	}

	private static int nibble3(int value) {
		return (value >> 12) & 0xF;
	}

	private static int signedNibble3(int value) {
		return (((value >> 12) & 0xF) << 28) >> 28;
	}
}
