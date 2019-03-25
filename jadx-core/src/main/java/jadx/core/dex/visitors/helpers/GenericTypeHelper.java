package jadx.core.dex.visitors.helpers;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jadx.core.Consts;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.InsnNode;

public class GenericTypeHelper {

	private List<Listener> listeners = new ArrayList<>();

	/** Key is class name, value is e.g. {@code K,V} */
	private Map<String, String> classVariables = new HashMap<>();

	public void isMethodReturnTypeAsClass(InsnNode node, Method method) {
		Class<?> cls = method.getDeclaringClass();
		TypeVariable<?>[] classTypes = cls.getTypeParameters();
		if (classTypes.length == 0) {
			return;
		}

		boolean added = false;
		Type type = method.getGenericReturnType();
		if (type instanceof ParameterizedType) {
			Type[] typeArguments = ((ParameterizedType) type).getActualTypeArguments();

			for (TypeVariable<?> classType : classTypes) {
				for (Type argument : typeArguments) {
					String typeName = argument.getTypeName();
					if (typeName.equals(classType.getName())) {
						listeners.add(new Listener(node.getResult(), typeName, (RegisterArg) node.getArg(0)));
						added = true;
					}
				}
			}
		}
		else if (type instanceof TypeVariable) {
			TypeVariable<?> typeVariable = ((TypeVariable<?>) type);
			for (TypeVariable<?> classType : classTypes) {
				String typeName = typeVariable.getName();
				if (typeName.equals(classType.getName())) {
					listeners.add(new Listener(node.getResult(), typeName, (RegisterArg) node.getArg(0)));
					added = true;
				}
			}
		}
		if (added && !classVariables.containsKey(cls.getName())) {
			String value = "";
			for (int i = 0; i < classTypes.length; i++) {
				if (i != 0) {
					value += ',';
				}
				value += classTypes[i];
			}
			classVariables.put(cls.getName(), value);
		}
	}

	public void setType(RegisterArg arg, ArgType type, String typeName) {
		for (Listener listener : listeners) {
			if (listener.handles(arg)) {
				listener.setType(type);
			}
		}
		ArgType originalType = arg.getType();
		if (originalType.isObject()) {
			String variables = classVariables.get(arg.getType().getObject());
			if (variables != null) {
				int index = variables.indexOf(typeName) / 2;
				if (originalType.isGeneric()) {
					originalType.getGenericTypes()[index] = type;
				}
				else {
					ArgType[] types = new ArgType[(variables.length() + 1) / 2];
					Arrays.fill(types, ArgType.object(Consts.CLASS_OBJECT));
					types[index] = type;
					arg.setType(ArgType.generic(originalType.getObject(), types));
				}
			}
		}
	}

	class Listener {

		private final RegisterArg source;
		private final String typeName;
		private final RegisterArg destination;

		Listener(RegisterArg source, String typeName, RegisterArg destination) {
			this.source = source;
			this.typeName = typeName;
			this.destination = destination;
		}

		boolean handles(RegisterArg arg) {
			return source.getRegNum() == arg.getRegNum()
					&& Objects.equals(source.getSVar(), arg.getSVar());
		}

		void setType(ArgType type) {
			GenericTypeHelper.this.setType(destination, type, typeName);
		}

		@Override
		public String toString() {
			return "From " + source + " to " + destination + " with type " + typeName;
		}

	}
}
