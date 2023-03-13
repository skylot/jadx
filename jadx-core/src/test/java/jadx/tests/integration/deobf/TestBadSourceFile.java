package jadx.tests.integration.deobf;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestBadSourceFile extends SmaliTest {

	@Test
	public void test() {
		// use source name disabled by default
		enableDeobfuscation();
		args.setDeobfuscationMinLength(100); // rename everything
		assertThat(searchCls(loadFromSmaliFiles(), "b"))
				.code()
				.containsOne("class C0000b {");
	}

	@Test
	public void testWithUseSourceName() {
		args.setUseSourceNameAsClassAlias(true);
		// deobfuscation disabled
		assertThat(searchCls(loadFromSmaliFiles(), "b"))
				.code()
				.containsOne("class a {");
	}

	@Test
	public void testWithUseSourceNameAndDeobf() {
		args.setUseSourceNameAsClassAlias(true);
		enableDeobfuscation();
		args.setDeobfuscationMinLength(100); // rename everything
		assertThat(searchCls(loadFromSmaliFiles(), "b"))
				.code()
				.containsOne("class C0000b {")
				.containsOne("/* compiled from: a.java */");
	}
}
