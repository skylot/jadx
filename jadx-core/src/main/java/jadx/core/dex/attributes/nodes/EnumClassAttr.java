package jadx.core.dex.attributes.nodes;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.Utils;

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
			if (argsCount != 0) {
				this.args = new ArrayList<InsnArg>(argsCount);
			} else {
				this.args = Collections.emptyList();
			}
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
