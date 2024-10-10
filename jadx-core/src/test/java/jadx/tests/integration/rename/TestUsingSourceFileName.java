package jadx.tests.integration.rename;

import org.junit.jupiter.api.Test;

import jadx.api.args.UseSourceNameAsClassNameAlias;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestUsingSourceFileName extends SmaliTest {

	@Test
	public void testNeverUseSourceName() {
		args.setUseSourceNameAsClassNameAlias(UseSourceNameAsClassNameAlias.NEVER);
		assertThat(searchCls(loadFromSmaliFiles(), "b"))
				.code()
				.containsOne("class b {");
	}

	@Test
	public void testIfBetterUseSourceName() {
		args.setUseSourceNameAsClassNameAlias(UseSourceNameAsClassNameAlias.IF_BETTER);
		assertThat(searchCls(loadFromSmaliFiles(), "b"))
				.code()
				.containsOne("class a {");
	}

	@Test
	public void testAlwaysUseSourceName() {
		args.setUseSourceNameAsClassNameAlias(UseSourceNameAsClassNameAlias.ALWAYS);
		assertThat(searchCls(loadFromSmaliFiles(), "b"))
				.code()
				.containsOne("class a {");
	}

	@Test
	public void testNeverUseSourceNameWithDeobf() {
		args.setUseSourceNameAsClassNameAlias(UseSourceNameAsClassNameAlias.NEVER);
		enableDeobfuscation();
		args.setDeobfuscationMinLength(100); // rename everything
		assertThat(searchCls(loadFromSmaliFiles(), "b"))
				.code()
				.containsOne("class C0000b {")
				.containsOne("/* compiled from: a.java */");
	}

	@Test
	public void testIfBetterUseSourceNameWithDeobf() {
		args.setUseSourceNameAsClassNameAlias(UseSourceNameAsClassNameAlias.IF_BETTER);
		enableDeobfuscation();
		args.setDeobfuscationMinLength(100); // rename everything
		assertThat(searchCls(loadFromSmaliFiles(), "b"))
				.code()
				.containsOne("class a {")
				.containsOne("/* compiled from: a.java */");
	}

	@Test
	public void testAlwaysUseSourceNameWithDeobf() {
		args.setUseSourceNameAsClassNameAlias(UseSourceNameAsClassNameAlias.ALWAYS);
		enableDeobfuscation();
		args.setDeobfuscationMinLength(100); // rename everything
		assertThat(searchCls(loadFromSmaliFiles(), "b"))
				.code()
				.containsOne("class a {")
				.containsOne("/* compiled from: a.java */");
	}

	@Test
	public void testDeprecatedDontUseSourceName() {
		// noinspection deprecation
		args.setUseSourceNameAsClassAlias(false);
		assertThat(searchCls(loadFromSmaliFiles(), "b"))
				.code()
				.containsOne("class b {");
	}

	@Test
	public void testDeprecatedUseSourceName() {
		// noinspection deprecation
		args.setUseSourceNameAsClassAlias(true);
		assertThat(searchCls(loadFromSmaliFiles(), "b"))
				.code()
				.containsOne("class a {");
	}

	@Test
	public void testDeprecatedDontUseSourceNameWithDeobf() {
		// noinspection deprecation
		args.setUseSourceNameAsClassAlias(false);
		enableDeobfuscation();
		args.setDeobfuscationMinLength(100); // rename everything
		assertThat(searchCls(loadFromSmaliFiles(), "b"))
				.code()
				.containsOne("class C0000b {")
				.containsOne("/* compiled from: a.java */");
	}

	@Test
	public void testDeprecatedUseSourceNameWithDeobf() {
		// noinspection deprecation
		args.setUseSourceNameAsClassAlias(true);
		enableDeobfuscation();
		args.setDeobfuscationMinLength(100); // rename everything
		assertThat(searchCls(loadFromSmaliFiles(), "b"))
				.code()
				.containsOne("class a {")
				.containsOne("/* compiled from: a.java */");
	}
}
