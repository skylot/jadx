package jadx.gui.ui.hexeditor.buffer;

/*
 * code taken from https://github.com/kreativekorp/hexcellent
 */
public enum BitOperation {
	FILL {
		@Override
		public byte op(byte src, byte mask) {
			return mask;
		}
	},
	AND {
		@Override
		public byte op(byte src, byte mask) {
			return (byte) (src & mask);
		}
	},
	OR {
		@Override
		public byte op(byte src, byte mask) {
			return (byte) (src | mask);
		}
	},
	XOR {
		@Override
		public byte op(byte src, byte mask) {
			return (byte) (src ^ mask);
		}
	},
	NAND {
		@Override
		public byte op(byte src, byte mask) {
			byte temp = (byte) (src & mask);
			return (byte) (~temp);
		}
	},
	NOR {
		@Override
		public byte op(byte src, byte mask) {
			byte temp = (byte) (src | mask);
			return (byte) (~temp);
		}
	},
	XNOR {
		@Override
		public byte op(byte src, byte mask) {
			byte temp = (byte) (src ^ mask);
			return (byte) (~temp);
		}
	};

	public abstract byte op(byte src, byte mask);
}
