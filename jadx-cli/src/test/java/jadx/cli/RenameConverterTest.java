package jadx.cli;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jadx.api.JadxArgs.RenameEnum;
import jadx.cli.JadxCLIArgs.RenameConverter;
import jadx.core.utils.exceptions.JadxArgsValidateException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RenameConverterTest {

	private RenameConverter converter;

	@BeforeEach
	public void init() {
		converter = new RenameConverter("someParam");
	}

	@Test
	public void all() {
		Set<RenameEnum> set = converter.convert("all");
		assertThat(set).hasSize(3);
		assertThat(set).contains(RenameEnum.CASE);
		assertThat(set).contains(RenameEnum.VALID);
		assertThat(set).contains(RenameEnum.PRINTABLE);
	}

	@Test
	public void none() {
		Set<RenameEnum> set = converter.convert("none");
		assertThat(set).isEmpty();
	}

	@Test
	public void wrong() {
		JadxArgsValidateException thrown = assertThrows(JadxArgsValidateException.class,
				() -> converter.convert("wrong"),
				"Expected convert() to throw, but it didn't");

		assertThat(thrown.getMessage()).isEqualTo("'wrong' is unknown for parameter someParam, possible values are case, valid, printable");
	}
}
