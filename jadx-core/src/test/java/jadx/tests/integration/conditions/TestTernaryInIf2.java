package jadx.tests.integration.conditions;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

public class TestTernaryInIf2 extends IntegrationTest {

	public static class TestCls {
	    private String a;
	    private String b;

	    public boolean equals(TestCls other) {
	    	if (this.a == null ? other.a == null : this.a.equals(other.a)) {
    			if (this.b == null ? other.b == null : this.b.equals(other.b)) {
    				return true;
    			}
	    	}
	    	return false;
	    }
	}

	@Test
	@NotYetImplemented
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("this.a != null ? this.a.equals(other.a) : other.a == null"));
		assertThat(code, containsOne("this.b != null ? this.b.equals(other.b) : other.b == null"));
	}
}
