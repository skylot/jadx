package jadx.tests.integration.arrays;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;
import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;
import org.junit.jupiter.api.Test;

public class TestArrays4 extends SmaliTest {

    public static class TestCls {
        char[] payload;

        public TestCls(byte[] bytes) {
            char[] a = toChars(bytes);
            this.payload = new char[a.length];
            System.arraycopy(a, 0, this.payload, 0, bytes.length);
        }

        private static char[] toChars(byte[] bArr) {
            return new char[bArr.length];
        }
    }

    @Test
    public void testArrayTypeInference() {
        noDebugInfo();
        ClassNode cls = getClassNode(TestCls.class);
        String code = cls.getCode().toString();

        assertThat(code, containsOne("char[] toChars = toChars(bArr);"));
    }

}
