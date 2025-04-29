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
			return src &= mask;
		}
	},
	OR {
		@Override
		public byte op(byte src, byte mask) {
			return src |= mask;
		}
	},
	XOR {
		@Override
		public byte op(byte src, byte mask) {
			return src ^= mask;
		}
	},
	NAND {
		@Override
		public byte op(byte src, byte mask) {
			src &= mask;
			return src ^= -1;
		}
	},
	NOR {
		@Override
		public byte op(byte src, byte mask) {
			src |= mask;
			return src ^= -1;
		}
	},
	XNOR {
		@Override
		public byte op(byte src, byte mask) {
			src ^= mask;
			return src ^= -1;
		}
	};

	public abstract byte op(byte src, byte mask);
}
