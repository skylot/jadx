package jadx.tests.integration.others;

import java.io.EOFException;

import com.android.dex.Code;
import com.android.dx.io.instructions.DecodedInstruction;
import com.android.dx.io.instructions.ShortArrayCodeInput;
import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.DecodeException;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestLoopInTry2 extends IntegrationTest {

	public static class TestCls {
		private MethodNode method;
		private DexNode dex;
		private DecodedInstruction[] insnArr;

		public void test(Code mthCode) throws DecodeException {
			short[] encodedInstructions = mthCode.getInstructions();
			int size = encodedInstructions.length;
			DecodedInstruction[] decoded = new DecodedInstruction[size];
			ShortArrayCodeInput in = new ShortArrayCodeInput(encodedInstructions);
			try {
				while (in.hasMore()) {
					decoded[in.cursor()] = DecodedInstruction.decode(in);
				}
			} catch (EOFException e) {
				throw new DecodeException(method, "", e);
			}
			insnArr = decoded;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("try {"));
		assertThat(code, containsOne("while (in.hasMore()) {"));
		assertThat(code, containsOne("decoded[in.cursor()] = DecodedInstruction.decode(in);"));
		assertThat(code, containsOne("} catch (EOFException e) {"));
		assertThat(code, containsOne("throw new DecodeException"));
	}
}
