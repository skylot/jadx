package jadx.core.dex.visitors;

import java.lang.reflect.Method;
import java.util.Objects;

import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.helpers.GenericTypeHelper;
import jadx.core.utils.exceptions.JadxException;

public class GenericsTypeVisitor extends AbstractVisitor {

	private GenericTypeHelper helper = new GenericTypeHelper();

	@Override
	public void visit(MethodNode mth) throws JadxException {
		InsnNode[] instructions = mth.getInstructions();
		if (instructions == null) {
			return;
		}
		try {
			for (InsnNode node : instructions) {
				if (node != null) {
					switch(node.getType()) {
					case INVOKE:
						processInvoke((InvokeNode) node, mth);
						break;
					case CHECK_CAST:
						processCheckCast(node);
						break;
					}
				}
			}
		} catch (ClassNotFoundException | NoSuchMethodException e) {
		} finally {
			mth.unloadInsnArr();
		}
	}

	private void processInvoke(InvokeNode node, MethodNode mth) throws ClassNotFoundException, NoSuchMethodException {
		MethodInfo info = node.getCallMth();
		if (!info.isConstructor()) {

			Class<?> k = Class.forName(info.getDeclClass().getFullName());
			Class<?>[] list = info.getArgumentsTypes().stream().map(t -> {
				if (t.isObject()) {
					try {
						return Class.forName(t.getObject());
					} catch (ClassNotFoundException e) {
					}
				}
				return null;
			}).filter(Objects::nonNull).toArray(Class[]::new);

			Method method = k.getMethod(info.getName(), list);
			helper.isMethodReturnTypeAsClass(node, method);
		}
	}

	private void processCheckCast(InsnNode node) {
		ArgType type = node.getResult().getType();
		RegisterArg arg = (RegisterArg) node.getArg(0);
		if (!arg.getType().equals(type)) {
			helper.setType(arg, type, null);
		}
	}

}
