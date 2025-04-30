package jadx.gui.ui.hexeditor.buffer;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

/*
 * code taken from https://github.com/kreativekorp/hexcellent
 */
public final class FloatFormat implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final double LOG2OF10 = 3.321928094887362348;

	public static final FloatFormat RESPL4 = new FloatFormat(2, 1);
	public static final FloatFormat RESPL8 = new FloatFormat(4, 3);
	public static final FloatFormat WIKIPEDIA = new FloatFormat(4, 3, -2);
	public static final FloatFormat HALF = new FloatFormat(5, 10); // 2 bytes
	public static final FloatFormat BFLOAT16 = new FloatFormat(8, 7); // 2 bytes
	public static final FloatFormat TENSORFLOAT = new FloatFormat(8, 10);
	public static final FloatFormat FP24 = new FloatFormat(7, 16); // 3 bytes
	public static final FloatFormat PXR24 = new FloatFormat(8, 15); // 3 bytes
	public static final FloatFormat SINGLE = new FloatFormat(8, 23); // 4 bytes
	public static final FloatFormat RESPL48 = new FloatFormat(10, 37); // 6 bytes
	public static final FloatFormat DOUBLE = new FloatFormat(11, 52); // 8 bytes
	public static final FloatFormat QUADRUPLE = new FloatFormat(15, 112); // 16 bytes
	public static final FloatFormat OCTUPLE = new FloatFormat(19, 236); // 32 bytes

	private final int signBit;
	private final int expWidth;
	private final int manWidth;
	private final int quietBit;
	private final BigInteger expMask;
	private final BigInteger expBias;
	private final BigInteger expShift;
	private final BigInteger manMask;
	private final BigInteger patMask;
	private final int decimalDigits;
	private final MathContext mc;

	public FloatFormat(int expWidth, int manWidth) {
		this.signBit = expWidth + manWidth;
		this.expWidth = expWidth;
		this.manWidth = manWidth;
		this.quietBit = manWidth - 1;
		this.expMask = BigInteger.ONE.shiftLeft(expWidth).subtract(BigInteger.ONE);
		this.expBias = BigInteger.ONE.shiftLeft(expWidth - 1).subtract(BigInteger.ONE);
		this.expShift = this.expBias.add(BigInteger.valueOf(manWidth));
		this.manMask = BigInteger.ONE.shiftLeft(manWidth).subtract(BigInteger.ONE);
		this.patMask = BigInteger.ONE.shiftLeft(manWidth - 1).subtract(BigInteger.ONE);
		this.decimalDigits = (int) Math.ceil((manWidth + 1) / LOG2OF10);
		this.mc = new MathContext(decimalDigits + 1, RoundingMode.HALF_EVEN);
	}

	public FloatFormat(int expWidth, int manWidth, int expBias) {
		this.signBit = expWidth + manWidth;
		this.expWidth = expWidth;
		this.manWidth = manWidth;
		this.quietBit = manWidth - 1;
		this.expMask = BigInteger.ONE.shiftLeft(expWidth).subtract(BigInteger.ONE);
		this.expBias = BigInteger.valueOf(expBias);
		this.expShift = this.expBias.add(BigInteger.valueOf(manWidth));
		this.manMask = BigInteger.ONE.shiftLeft(manWidth).subtract(BigInteger.ONE);
		this.patMask = BigInteger.ONE.shiftLeft(manWidth - 1).subtract(BigInteger.ONE);
		this.decimalDigits = (int) Math.ceil((manWidth + 1) / LOG2OF10);
		this.mc = new MathContext(decimalDigits + 1, RoundingMode.HALF_EVEN);
	}

	public int getExponentWidth() {
		return expWidth;
	}

	public int getMantissaWidth() {
		return manWidth;
	}

	public int getExponentBias() {
		return expBias.intValue();
	}

	public int getDecimalDigits() {
		return decimalDigits;
	}

	public Number bitsToNumber(BigInteger bits) {
		boolean negative = bits.testBit(signBit);
		BigInteger rawExp = bits.shiftRight(manWidth).and(expMask);
		if (rawExp.equals(expMask)) {
			boolean quiet = bits.testBit(quietBit);
			BigInteger pattern = bits.and(patMask);
			return new NaN(negative, quiet, pattern);
		}

		BigDecimal mantissa;
		if (rawExp.equals(BigInteger.ZERO)) {
			BigInteger rawMan = bits.and(manMask);
			if (rawMan.equals(BigInteger.ZERO)) {
				return negative ? Zero.NEGATIVE_ZERO : Zero.POSITIVE_ZERO;
			} else {
				mantissa = new BigDecimal(rawMan.shiftLeft(1));
			}
		} else {
			mantissa = new BigDecimal(bits.and(manMask).setBit(manWidth));
		}

		BigInteger exp = rawExp.subtract(expShift);
		BigDecimal factor = new BigDecimal(BigInteger.ONE.shiftLeft(exp.abs().intValue()));
		BigDecimal magnitude = (exp.signum() < 0) ? mantissa.divide(factor) : mantissa.multiply(factor);
		BigDecimal result = negative ? magnitude.negate() : magnitude;
		if (result.scale() == 0) {
			result = result.setScale(1);
		}
		return result;
	}

	public Number round(Number value) {
		if (value instanceof BigDecimal) {
			BigDecimal result = ((BigDecimal) value).round(mc);
			if (result.scale() == 0) {
				result = result.setScale(1);
			}
			return result;
		}
		return value;
	}

	public BigInteger nanToBits(boolean negative, boolean quiet, BigInteger pattern) {
		BigInteger bits = pattern.and(patMask);
		if (quiet) {
			bits = bits.setBit(quietBit);
		}
		bits = bits.or(expMask.shiftLeft(manWidth));
		if (negative) {
			bits = bits.setBit(signBit);
		}
		return bits;
	}

	public BigInteger nanToBits(NaN nan) {
		return nanToBits(nan.isNegative(), nan.isQuiet(), nan.getPattern());
	}

	public BigInteger zeroToBits(boolean negative) {
		BigInteger bits = BigInteger.ZERO;
		if (negative) {
			bits = bits.setBit(signBit);
		}
		return bits;
	}

	public BigInteger zeroToBits(Zero zero) {
		return zeroToBits(zero.isNegative());
	}

	public BigInteger binaryToBits(boolean negative, BigInteger exponent, BigInteger mantissa) {
		int sign = mantissa.signum();
		if (sign == 0) {
			return zeroToBits(negative);
		}
		if (sign < 0) {
			negative = !negative;
			mantissa = mantissa.negate();
		}

		int shift = manWidth + 1 - mantissa.bitLength();
		exponent = exponent.subtract(BigInteger.valueOf(shift));
		mantissa = mantissa.shiftLeft(shift);
		BigInteger rawExp = exponent.add(expShift);

		if (rawExp.compareTo(expMask) >= 0) {
			// Overflow / Infinity
			mantissa = BigInteger.ZERO;
			rawExp = expMask;
		} else if (rawExp.signum() <= 0) {
			// Underflow / Subnormal
			shift = BigInteger.ONE.subtract(rawExp).intValue();
			mantissa = mantissa.shiftRight(shift);
			rawExp = BigInteger.ZERO;
		}

		BigInteger bits = mantissa.and(manMask);
		bits = bits.or(rawExp.shiftLeft(manWidth));
		if (negative) {
			bits = bits.setBit(signBit);
		}
		return bits;
	}

	public BigInteger decimalToBits(boolean negative, BigInteger exponent, BigInteger mantissa) {
		int sign = mantissa.signum();
		if (sign == 0) {
			return zeroToBits(negative);
		}
		if (sign < 0) {
			negative = !negative;
			mantissa = mantissa.negate();
		}

		// Here we convert decimal to binary using the identity
		// m * 10^n = m * (5*2)^n = m * 5^n * 2^n = (m * 5^n) * 2^n
		// and shifting (m * 5^n) by powers of two until we have
		// enough bits of precision (it is greater than 2^manWidth).
		// This only works with RoundingMode.UP; why?
		// Is there a better way to do this?
		MathContext mc = new MathContext(decimalDigits + 5, RoundingMode.UP);
		BigDecimal binMan = new BigDecimal(mantissa);
		BigDecimal bigFive = new BigDecimal(BigInteger.valueOf(5).pow(exponent.abs().intValue()));
		BigDecimal manSize = new BigDecimal(BigInteger.ONE.shiftLeft(this.manWidth));
		BigInteger manWidth = BigInteger.valueOf(this.manWidth);
		binMan = (exponent.signum() < 0) ? binMan.divide(bigFive, mc) : binMan.multiply(bigFive, mc);
		while (binMan.compareTo(manSize) < 0) {
			binMan = binMan.multiply(manSize, mc);
			exponent = exponent.subtract(manWidth);
		}
		return binaryToBits(negative, exponent, binMan.toBigInteger());
	}

	public BigInteger bigDecimalToBits(BigDecimal value) {
		return decimalToBits(false, BigInteger.valueOf(-value.scale()), value.unscaledValue());
	}

	public BigInteger bigIntegerToBits(BigInteger value) {
		return binaryToBits(false, BigInteger.ZERO, value);
	}

	public BigInteger longToBits(long value) {
		return binaryToBits(false, BigInteger.ZERO, BigInteger.valueOf(value));
	}

	public BigInteger intToBits(int value) {
		return binaryToBits(false, BigInteger.ZERO, BigInteger.valueOf(value));
	}

	public BigInteger floatToBits(float value) {
		int fbits = Float.floatToRawIntBits(value);
		boolean negative = (fbits < 0);
		int rawExp = (fbits >> 23) & 0xFF;
		if (rawExp == 0xFF) {
			boolean quiet = ((fbits & (1 << 22)) != 0);
			int pattern = fbits & ((1 << 22) - 1);
			return nanToBits(negative, quiet, BigInteger.valueOf(pattern));
		}

		int mantissa;
		if (rawExp == 0) {
			int rawMan = fbits & ((1 << 23) - 1);
			if (rawMan == 0) {
				return zeroToBits(negative);
			}
			mantissa = rawMan << 1;
		} else {
			mantissa = (fbits & ((1 << 23) - 1)) | (1 << 23);
		}

		int exp = rawExp - (127 + 23);
		return binaryToBits(negative, BigInteger.valueOf(exp), BigInteger.valueOf(mantissa));
	}

	public BigInteger doubleToBits(double value) {
		long dbits = Double.doubleToRawLongBits(value);
		boolean negative = (dbits < 0);
		long rawExp = (dbits >> 52) & 0x7FF;
		if (rawExp == 0x7FF) {
			boolean quiet = ((dbits & (1L << 51)) != 0);
			long pattern = dbits & ((1L << 51) - 1);
			return nanToBits(negative, quiet, BigInteger.valueOf(pattern));
		}

		long mantissa;
		if (rawExp == 0) {
			long rawMan = dbits & ((1L << 52) - 1);
			if (rawMan == 0) {
				return zeroToBits(negative);
			}
			mantissa = rawMan << 1;
		} else {
			mantissa = (dbits & ((1L << 52) - 1)) | (1L << 52);
		}

		long exp = rawExp - (1023 + 52);
		return binaryToBits(negative, BigInteger.valueOf(exp), BigInteger.valueOf(mantissa));
	}

	public BigInteger numberToBits(Number value) {
		if (value instanceof NaN) {
			return nanToBits((NaN) value);
		}
		if (value instanceof Zero) {
			return zeroToBits((Zero) value);
		}
		if (value instanceof BigDecimal) {
			return bigDecimalToBits((BigDecimal) value);
		}
		if (value instanceof BigInteger) {
			return bigIntegerToBits((BigInteger) value);
		}
		if (value instanceof Long) {
			return longToBits(value.longValue());
		}
		if (value instanceof Integer) {
			return intToBits(value.intValue());
		}
		if (value instanceof Short) {
			return intToBits(value.intValue());
		}
		if (value instanceof Byte) {
			return intToBits(value.intValue());
		}
		if (value instanceof Float) {
			return floatToBits(value.floatValue());
		}
		if (value instanceof Double) {
			return doubleToBits(value.doubleValue());
		}
		throw new UnsupportedOperationException(
				"unknown subclass of Number; please convert to a known subclass "
						+ "(Byte, Short, Integer, Long, Float, Double, BigInteger, BigDecimal, "
						+ "FloatFormat.Zero, or FloatFormat.NaN)");
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof FloatFormat) {
			FloatFormat that = (FloatFormat) o;
			return (this.expWidth == that.expWidth
					&& this.manWidth == that.manWidth
					&& this.expBias.equals(that.expBias));
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = expBias.hashCode();
		hash += expWidth * 257;
		hash += manWidth * 65537;
		return hash;
	}

	@Override
	public String toString() {
		return "FloatFormat[1." + expWidth + "." + manWidth + "." + expBias + "]";
	}

	public static final class Zero extends Number {
		private static final long serialVersionUID = 1L;
		private static final long DV = 0x8000000000000000L;
		private static final int FV = 0x80000000;

		public static final Zero POSITIVE_ZERO = new Zero(false);
		public static final Zero NEGATIVE_ZERO = new Zero(true);

		private final boolean n;

		private Zero(boolean n) {
			this.n = n;
		}

		public boolean isNegative() {
			return n;
		}

		public Zero abs() {
			return POSITIVE_ZERO;
		}

		public Zero negate() {
			return n ? POSITIVE_ZERO : NEGATIVE_ZERO;
		}

		public Zero plus() {
			return n ? NEGATIVE_ZERO : POSITIVE_ZERO;
		}

		public int signum() {
			return 0;
		}

		@Override
		public float floatValue() {
			return Float.intBitsToFloat(n ? FV : 0);
		}

		@Override
		public double doubleValue() {
			return Double.longBitsToDouble(n ? DV : 0);
		}

		@Override
		public byte byteValue() {
			return 0;
		}

		@Override
		public short shortValue() {
			return 0;
		}

		@Override
		public int intValue() {
			return 0;
		}

		@Override
		public long longValue() {
			return 0;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Zero && n == ((Zero) o).n;
		}

		@Override
		public int hashCode() {
			return n ? FV : 0;
		}

		@Override
		public String toString() {
			return n ? "-0.0" : "0.0";
		}
	}

	public static final class NaN extends Number {
		private static final long serialVersionUID = 1L;
		private static final long DOUBLE_S = 0x8000000000000000L; // sign bit
		private static final long DOUBLE_I = 0x7FF0000000000000L; // infinity
		private static final long DOUBLE_Q = 0x0008000000000000L; // quiet bit
		private static final long DOUBLE_P = 0x0007FFFFFFFFFFFFL; // nan pattern
		private static final int FLOAT_S = 0x80000000; // sign bit
		private static final int FLOAT_I = 0x7F800000; // infinity
		private static final int FLOAT_Q = 0x00400000; // quiet bit
		private static final int FLOAT_P = 0x003FFFFF; // nan pattern

		public static final NaN POSITIVE_INFINITY = new NaN(false, false, BigInteger.ZERO);
		public static final NaN NEGATIVE_INFINITY = new NaN(true, false, BigInteger.ZERO);
		public static final NaN NA_N = new NaN(false, true, BigInteger.ZERO);

		private final boolean negative;
		private final boolean quiet;
		private final BigInteger pattern;

		public NaN(boolean negative, boolean quiet, BigInteger pattern) {
			if (pattern == null) {
				pattern = BigInteger.ZERO;
			}
			this.negative = negative;
			this.quiet = quiet;
			this.pattern = pattern;
		}

		public NaN(float value) {
			int bits = Float.floatToRawIntBits(value);
			if ((bits & FLOAT_I) != FLOAT_I) {
				throw new IllegalArgumentException("Not a NaN");
			}
			this.negative = ((bits & FLOAT_S) != 0);
			this.quiet = ((bits & FLOAT_Q) != 0);
			this.pattern = BigInteger.valueOf(bits & FLOAT_P);
		}

		public NaN(double value) {
			long bits = Double.doubleToRawLongBits(value);
			if ((bits & DOUBLE_I) != DOUBLE_I) {
				throw new IllegalArgumentException("Not a NaN");
			}
			this.negative = ((bits & DOUBLE_S) != 0);
			this.quiet = ((bits & DOUBLE_Q) != 0);
			this.pattern = BigInteger.valueOf(bits & DOUBLE_P);
		}

		public boolean isNegative() {
			return negative;
		}

		public boolean isQuiet() {
			return quiet;
		}

		public BigInteger getPattern() {
			return pattern;
		}

		public NaN abs() {
			return new NaN(false, quiet, pattern);
		}

		public NaN negate() {
			return new NaN(!negative, quiet, pattern);
		}

		public NaN plus() {
			return new NaN(negative, quiet, pattern);
		}

		public int signum() {
			if (quiet || !pattern.equals(BigInteger.ZERO)) {
				return 0;
			} else {
				return (negative ? -1 : 1);
			}
		}

		@Override
		public float floatValue() {
			int bits = (pattern.intValue() & FLOAT_P) | FLOAT_I;
			if (negative) {
				bits |= FLOAT_S;
			}
			if (quiet) {
				bits |= FLOAT_Q;
			}
			return Float.intBitsToFloat(bits);
		}

		@Override
		public double doubleValue() {
			long bits = (pattern.longValue() & DOUBLE_P) | DOUBLE_I;
			if (negative) {
				bits |= DOUBLE_S;
			}
			if (quiet) {
				bits |= DOUBLE_Q;
			}
			return Double.longBitsToDouble(bits);
		}

		@Override
		public byte byteValue() {
			return 0;
		}

		@Override
		public short shortValue() {
			return 0;
		}

		@Override
		public int intValue() {
			return 0;
		}

		@Override
		public long longValue() {
			return 0;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof NaN) {
				NaN that = (NaN) o;
				return (this.negative == that.negative
						&& this.quiet == that.quiet
						&& this.pattern.equals(that.pattern));
			}
			return false;
		}

		@Override
		public int hashCode() {
			int hash = pattern.hashCode();
			if (negative) {
				hash ^= 0x80000000;
			}
			if (quiet) {
				hash ^= 0x40000000;
			}
			return hash;
		}

		@Override
		public String toString() {
			if (quiet || !pattern.equals(BigInteger.ZERO)) {
				return "NaN";
			} else {
				return (negative ? "-Infinity" : "Infinity");
			}
		}
	}
}
