package jadx.gui.ui.codearea;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConvertNumberActionTest {

	@Test
	public void nonNumeric() {
		assertThat(ConvertNumberAction.getConversionsFromWord("non-numeric")).isNullOrEmpty();
		assertThat(ConvertNumberAction.getConversionsFromWord("0xnon-numeric")).isNullOrEmpty();
		assertThat(ConvertNumberAction.getConversionsFromWord("non-numericL")).isNullOrEmpty();
		assertThat(ConvertNumberAction.getConversionsFromWord("-non-numeric")).isNullOrEmpty();
		assertThat(ConvertNumberAction.getConversionsFromWord("ABCD")).isNullOrEmpty();
	}

	@Test
	public void simpleDecimalToHex() {

		List<String> expected = new ArrayList<String>();
		expected.add("0x7b");
		expected.add("0b01111011");
		expected.add("'{'");

		List<String> result = ConvertNumberAction.getConversionsFromWord("123");

		assertThat(result).isNotEmpty();

		expected.removeAll(result);
		assertThat(expected).isEmpty();
	}

	@Test
	public void negativeDecimalToHex() {

		List<String> expected = new ArrayList<String>();
		expected.add("0xffffff85");
		expected.add("0b11111111111111111111111110000101");

		List<String> result = ConvertNumberAction.getConversionsFromWord("-123");

		assertThat(result).isNotEmpty();

		expected.removeAll(result);
		assertThat(expected).isEmpty();
	}

	@Test
	public void negativeLongDecimalToHex() {

		List<String> expected = new ArrayList<String>();
		expected.add("0xFFFFFFE8B7891800".toLowerCase());
		expected.add("0b1111111111111111111111111110100010110111100010010001100000000000");

		List<String> result = ConvertNumberAction.getConversionsFromWord("-100000000000");

		assertThat(result).isNotEmpty();

		expected.removeAll(result);
		assertThat(expected).isEmpty();
	}

	@Test
	public void simpleHexToDecimal() {

		List<String> expected = new ArrayList<String>();
		expected.add("123");
		expected.add("0b01111011");
		expected.add("'{'");

		List<String> result = ConvertNumberAction.getConversionsFromWord("0x7b");

		assertThat(result).isNotEmpty();

		expected.removeAll(result);
		assertThat(expected).isEmpty();
	}

	@Test
	public void zeroToHex() {

		List<String> expected = new ArrayList<String>();
		expected.add("0x0");
		expected.add("0b00000000");

		List<String> result = ConvertNumberAction.getConversionsFromWord(Integer.toString(0));

		assertThat(result).isNotEmpty();

		expected.removeAll(result);
		assertThat(expected).isEmpty();

	}

	@Test
	public void minIntToHex() {

		List<String> expected = new ArrayList<String>();
		expected.add("0x80000000");
		expected.add("0b10000000000000000000000000000000");

		List<String> result = ConvertNumberAction.getConversionsFromWord(Integer.toString(Integer.MIN_VALUE));

		assertThat(result).isNotEmpty();

		expected.removeAll(result);
		assertThat(expected).isEmpty();

	}

	@Test
	public void maxIntToHex() {

		List<String> expected = new ArrayList<String>();
		expected.add("0x7fffffff");
		expected.add("0b01111111111111111111111111111111");

		List<String> result = ConvertNumberAction.getConversionsFromWord(Integer.toString(Integer.MAX_VALUE));

		assertThat(result).isNotEmpty();

		expected.removeAll(result);
		assertThat(expected).isEmpty();

	}

	@Test
	public void minLongToHex() {

		List<String> expected = new ArrayList<String>();
		expected.add("0x8000000000000000");
		expected.add("0b1000000000000000000000000000000000000000000000000000000000000000");

		List<String> result = ConvertNumberAction.getConversionsFromWord(Long.toString(Long.MIN_VALUE));

		assertThat(result).isNotEmpty();

		expected.removeAll(result);
		assertThat(expected).isEmpty();

	}

	@Test
	public void maxLongToHex() {

		List<String> expected = new ArrayList<String>();
		expected.add("0x7fffffffffffffff");
		expected.add("0b0111111111111111111111111111111111111111111111111111111111111111");

		List<String> result = ConvertNumberAction.getConversionsFromWord(Long.toString(Long.MAX_VALUE));

		assertThat(result).isNotEmpty();

		expected.removeAll(result);
		assertThat(expected).isEmpty();

	}

	@Test
	public void simpleLongSuffix() {

		List<String> expected = new ArrayList<String>();
		expected.add("0x7b");
		expected.add("0b01111011");
		expected.add("'{'");

		List<String> result = ConvertNumberAction.getConversionsFromWord("123L");

		assertThat(result).isNotEmpty();

		expected.removeAll(result);
		assertThat(expected).isEmpty();

	}

	@Test
	public void binaryPadding() {

		assertThat(ConvertNumberAction.getConversionsFromWord("0")).containsOnlyOnce("0b00000000");
		assertThat(ConvertNumberAction.getConversionsFromWord("1")).containsOnlyOnce("0b00000001");
		assertThat(ConvertNumberAction.getConversionsFromWord("127")).containsOnlyOnce("0b01111111");
		assertThat(ConvertNumberAction.getConversionsFromWord("0xff")).containsOnlyOnce("0b11111111");
		assertThat(ConvertNumberAction.getConversionsFromWord("0x7fff")).containsOnlyOnce("0b0111111111111111");
		assertThat(ConvertNumberAction.getConversionsFromWord("0xffff")).containsOnlyOnce("0b1111111111111111");
		assertThat(ConvertNumberAction.getConversionsFromWord("0x10000")).containsOnlyOnce("0b000000010000000000000000");
		assertThat(ConvertNumberAction.getConversionsFromWord("0xffffffff")).containsOnlyOnce("0b11111111111111111111111111111111");

		assertThat(ConvertNumberAction.getConversionsFromWord("0xffffffffffff"))
				.containsOnlyOnce("0b111111111111111111111111111111111111111111111111");

		assertThat(ConvertNumberAction.getConversionsFromWord("0x7fffffffffff"))
				.containsOnlyOnce("0b011111111111111111111111111111111111111111111111");

		assertThat(ConvertNumberAction.getConversionsFromWord("0x7fffffffffffffff"))
				.containsOnlyOnce("0b0111111111111111111111111111111111111111111111111111111111111111");

	}

	@Test
	public void printableAscii() {

		for (int i = 32; i < 127; i++) {
			String printed = String.format("'%c'", i);
			assertThat(ConvertNumberAction.getConversionsFromWord(Integer.toString(i))).containsOnlyOnce(printed);
		}
	}

}
