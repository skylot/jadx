package jadx.tests.integration.generics;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestClassSignature extends SmaliTest {
	// @formatter:off
	/*
		Incorrect class signature, super class is equals to this class: <T:Ljava/lang/Object;>Lgenerics/TestClassSignature<TT;>;
	*/
	// @formatter:on

	@Test
	public void test() {
		allowWarnInCode();
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("Incorrect class signature")
				.doesNotContain("StackOverflowError");
	}
}
