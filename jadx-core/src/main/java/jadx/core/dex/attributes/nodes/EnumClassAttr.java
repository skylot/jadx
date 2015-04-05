package jadx.core.dex.attributes.nodes;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;

import java.util.ArrayList;
import java.util.List;

public class EnumClassAttr implements IAttribute {

	public static class EnumField {
		private final String name;
		private final ConstructorInsn constrInsn;
		private final int startArg;
		private ClassNode cls;

		public EnumField(String name, ConstructorInsn co, int startArg) {
			this.name = name;
			this.constrInsn = co;
			this.startArg = startArg;
		}

		public String getName() {
			return name;
		}

		public ConstructorInsn getConstrInsn() {
			return constrInsn;
		}

		public int getStartArg() {
			return startArg;
		}

		public ClassNode getCls() {
			return cls;
		}

		public void setCls(ClassNode cls) {
			this.cls = cls;
		}

		@Override
		public String toString() {
			return name + "(" + constrInsn + ") " + cls;
		}
	}

	private final List<EnumField> fields;
	private MethodNode staticMethod;

	public EnumClassAttr(int fieldsCount) {
		this.fields = new ArrayList<EnumField>(fieldsCount);
	}

	public List<EnumField> getFields() {
		return fields;
	}

	public MethodNode getStaticMethod() {
		return staticMethod;
	}

	public void setStaticMethod(MethodNode staticMethod) {
		this.staticMethod = staticMethod;
	}

	@Override
	public AType<EnumClassAttr> getType() {
		return AType.ENUM_CLASS;
	}

	@Override
	public String toString() {
		return "Enum fields: " + fields;
	}

}
