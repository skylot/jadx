package jadx.tests.functional;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import jadx.api.JadxArgs;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.typeinference.TypeUpdate;
import jadx.core.dex.visitors.typeinference.TypeUpdateInfo;
import jadx.core.dex.visitors.typeinference.TypeUpdateResult;

import static org.assertj.core.api.Assertions.assertThat;

public class TestTypeUpdate {

	@Test
	public void testConstListenerWithMissingResult() throws Exception {
		TypeUpdate typeUpdate = new TypeUpdate(new RootNode(new JadxArgs()));
		InsnNode constInsn = new InsnNode(InsnType.CONST, 1);
		LiteralArg arg = LiteralArg.make(0, ArgType.UNKNOWN);
		constInsn.addArg(arg);

		Method listener = TypeUpdate.class.getDeclaredMethod("sameFirstArgListener",
				TypeUpdateInfo.class,
				InsnNode.class,
				InsnArg.class,
				ArgType.class);
		listener.setAccessible(true);

		Object result = listener.invoke(typeUpdate, null, constInsn, arg, ArgType.DOUBLE);

		assertThat(result).isEqualTo(TypeUpdateResult.CHANGED);
	}
}
