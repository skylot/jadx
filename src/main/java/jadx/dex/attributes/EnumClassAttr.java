package jadx.dex.attributes;

import jadx.dex.instructions.args.InsnArg;
import jadx.dex.nodes.ClassNode;
import jadx.dex.nodes.MethodNode;
import jadx.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnumClassAttr implements IAttribute {

	public static class EnumField {
		private final String name;
		private final List<InsnArg> args;
		private ClassNode cls;

		public EnumField(String name, int argsCount) {
			this.name = name;
			if (argsCount != 0)
				this.args = new ArrayList<InsnArg>(argsCount);
			else
				this.args = Collections.emptyList();
		}

		public String getName() {
			return name;
		}

		public List<InsnArg> getArgs() {
			return args;
		}

		public ClassNode getCls() {
			return cls;
		}

		public void setCls(ClassNode cls) {
			this.cls = cls;
		}

		@Override
		public String toString() {
			return name + "(" + Utils.listToString(args) + ") " + cls;
		}
	}

	private final List<EnumField> fields;
	private MethodNode staticMethod;

	public EnumClassAttr(int fieldsCount) {
		this.fields = new ArrayList<EnumClassAttr.EnumField>(fieldsCount);
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
	public AttributeType getType() {
		return AttributeType.ENUM_CLASS;
	}

	@Override
	public String toString() {
		return "Enum fields: " + fields;
	}

}
