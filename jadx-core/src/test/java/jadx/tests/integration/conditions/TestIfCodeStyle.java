package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Issue #1455
 */
public class TestIfCodeStyle extends SmaliTest {

	@SuppressWarnings({ "ConstantConditions", "FieldCanBeLocal", "unused" })
	public static class TestCls {

		private String moduleName;
		private String modulePath;
		private String preinstalledModulePath;
		private long versionCode;
		private String versionName;
		private boolean isFactory;
		private boolean isActive;

		public void test(Parcel parcel) {
			int startPos = parcel.dataPosition();
			int size = parcel.readInt();
			if (size < 0) {
				if (startPos > Integer.MAX_VALUE - size) {
					throw new RuntimeException("Overflow in the size of parcelable");
				}
				parcel.setDataPosition(startPos + size);
				return;
			}
			try {
				if (parcel.dataPosition() - startPos >= size) {
					if (startPos > Integer.MAX_VALUE - size) {
						throw new RuntimeException("Overflow in the size of parcelable");
					}
					parcel.setDataPosition(startPos + size);
					return;
				}
				this.moduleName = parcel.readString();
				if (parcel.dataPosition() - startPos >= size) {
					if (startPos > Integer.MAX_VALUE - size) {
						throw new RuntimeException("Overflow in the size of parcelable");
					}
					parcel.setDataPosition(startPos + size);
					return;
				}
				this.modulePath = parcel.readString();
				if (parcel.dataPosition() - startPos >= size) {
					if (startPos > Integer.MAX_VALUE - size) {
						throw new RuntimeException("Overflow in the size of parcelable");
					}
					parcel.setDataPosition(startPos + size);
					return;
				}
				this.preinstalledModulePath = parcel.readString();
				if (parcel.dataPosition() - startPos >= size) {
					if (startPos > Integer.MAX_VALUE - size) {
						throw new RuntimeException("Overflow in the size of parcelable");
					}
					parcel.setDataPosition(startPos + size);
					return;
				}
				this.versionCode = parcel.readLong();
				if (parcel.dataPosition() - startPos >= size) {
					if (startPos > Integer.MAX_VALUE - size) {
						throw new RuntimeException("Overflow in the size of parcelable");
					}
					parcel.setDataPosition(startPos + size);
					return;
				}
				this.versionName = parcel.readString();
				if (parcel.dataPosition() - startPos >= size) {
					if (startPos > Integer.MAX_VALUE - size) {
						throw new RuntimeException("Overflow in the size of parcelable");
					}
					parcel.setDataPosition(startPos + size);
					return;
				}
				this.isFactory = parcel.readInt() != 0;
				if (parcel.dataPosition() - startPos >= size) {
					if (startPos > Integer.MAX_VALUE - size) {
						throw new RuntimeException("Overflow in the size of parcelable");
					}
					parcel.setDataPosition(startPos + size);
					return;
				}
				this.isActive = parcel.readInt() != 0;
				if (startPos > Integer.MAX_VALUE - size) {
					throw new RuntimeException("Overflow in the size of parcelable");
				}
				parcel.setDataPosition(startPos + size);
			} catch (Throwable e) {
				if (startPos <= Integer.MAX_VALUE - size) {
					parcel.setDataPosition(startPos + size);
					throw e;
				}
				throw new RuntimeException("Overflow in the size of parcelable");
			}
		}

		private static class Parcel {
			public void setDataPosition(int i) {
			}

			public int dataPosition() {
				return 0;
			}

			public int readInt() {
				return 0;
			}

			public String readString() {
				return null;
			}

			public long readLong() {
				return 0;
			}
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("else")
				.countString(8, "return;")
				.containsLines(2,
						"if (readInt < 0) {",
						indent() + "if (dataPosition > Integer.MAX_VALUE - readInt) {",
						indent(2) + "throw new RuntimeException(\"Overflow in the size of parcelable\");",
						indent() + "}",
						indent() + "parcel.setDataPosition(dataPosition + readInt);",
						indent() + "return;",
						"}");
	}

	@Test
	public void testSmali() {
		disableCompilation();
		assertThat(getClassNodeFromSmali())
				.code()
				.doesNotContain("else")
				.countString(8, "return;")
				.containsLines(2,
						"if (_aidl_parcelable_size < 0) {",
						indent() + "if (_aidl_start_pos > Integer.MAX_VALUE - _aidl_parcelable_size) {",
						indent(2) + "throw new RuntimeException(\"Overflow in the size of parcelable\");",
						indent() + "}",
						indent() + "_aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);",
						indent() + "return;",
						"}");
	}
}
