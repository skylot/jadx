package jadx.gui.links;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.core.dex.instructions.args.ArgType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JadxLinkTest {

	@Test
	void parseMethodLink() {
		JadxLink link = JadxLink.parse("jadx://3FA9C01B7E/com.example.app.LoginActivity.checkPassword()");
		assertThat(link.getHashPrefix()).isEqualTo("3fa9c01b7e");
		assertThat(link.getClsName()).isEqualTo("com.example.app.LoginActivity");
		assertThat(link.getMthName()).isEqualTo("checkPassword");
		assertThat(link.getArgTypes()).isEmpty();
	}

	@Test
	void parseMethodWithArgs() {
		JadxLink link = JadxLink.parse("jadx://3fa9c01b7e/a.b.C.m(java.lang.String, int[])/");
		assertThat(link.getClsName()).isEqualTo("a.b.C");
		assertThat(link.getMthName()).isEqualTo("m");
		assertThat(link.getArgTypes()).containsExactly("java.lang.String", "int[]");
	}

	@Test
	void parseEncodedConstructor() {
		JadxLink link = JadxLink.parse("jadx://3fa9c01b7e/a.b.C.%3Cinit%3E(int%5B%5D)");
		assertThat(link.getMthName()).isEqualTo("<init>");
		assertThat(link.getArgTypes()).containsExactly("int[]");
	}

	@Test
	void parseClassLink() {
		JadxLink link = JadxLink.parse("jadx://3fa9c01b7e/a.b.C");
		assertThat(link.getClsName()).isEqualTo("a.b.C");
		assertThat(link.getMthName()).isNull();
		assertThat(link.getArgTypes()).isNull();
	}

	@Test
	void parseLegacyColonSeparator() {
		// links created before the '/' separator must still resolve
		JadxLink link = JadxLink.parse("jadx://3fa9c01b7e:a.b.C.m(int)");
		assertThat(link.getHashPrefix()).isEqualTo("3fa9c01b7e");
		assertThat(link.getClsName()).isEqualTo("a.b.C");
		assertThat(link.getMthName()).isEqualTo("m");
		assertThat(link.getArgTypes()).containsExactly("int");
	}

	@Test
	void buildEmitsSlashSeparator() {
		String hash = "3fa9c01b7edeadbeef";
		String link = JadxLink.build(hash, "com.example.Foo");
		assertThat(link).isEqualTo("jadx://3fa9c01b7e/com.example.Foo");
		// round-trips back
		assertThat(JadxLink.parse(link).getClsName()).isEqualTo("com.example.Foo");
	}

	@Test
	void parseInvalid() {
		assertThatThrownBy(() -> JadxLink.parse("http://x")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> JadxLink.parse("jadx://3fa9c01b7e")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> JadxLink.parse("jadx://nothex/a.B.m()")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> JadxLink.parse("jadx://3fa9c01b7e/m()")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> JadxLink.parse("jadx://3fa9c01b7e/a.B.m(int")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void argsMatch() {
		List<ArgType> args = List.of(ArgType.STRING, ArgType.array(ArgType.INT), ArgType.object("a.Outer$Inner"));
		assertThat(JadxLink.parse("jadx://3fa9c01b7e/a.B.m(java.lang.String,int[],a.Outer.Inner)").argsMatch(args)).isTrue();
		assertThat(JadxLink.parse("jadx://3fa9c01b7e/a.B.m(String,int[],a.Outer$Inner)").argsMatch(args)).isTrue();
		assertThat(JadxLink.parse("jadx://3fa9c01b7e/a.B.m(String,int,a.Outer.Inner)").argsMatch(args)).isFalse();
		assertThat(JadxLink.parse("jadx://3fa9c01b7e/a.B.m()").argsMatch(args)).isFalse();
		assertThat(JadxLink.parse("jadx://3fa9c01b7e/a.B.m").argsMatch(args)).isTrue();
	}
}
