package jadx.core.dex.attributes.nodes;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.instructions.args.InsnArg;

public class FieldReplaceAttr implements IAttribute {

	public enum ReplaceWith {
		CLASS_INSTANCE,
		VAR
	}

	private final ReplaceWith replaceType;
	private final Object replaceObj;

	public FieldReplaceAttr(ClassInfo cls) {
		this.replaceType = ReplaceWith.CLASS_INSTANCE;
		this.replaceObj = cls;
	}

	public FieldReplaceAttr(InsnArg reg) {
		this.replaceType = ReplaceWith.VAR;
		this.replaceObj = reg;
	}

	public ReplaceWith getReplaceType() {
		return replaceType;
	}

	public ClassInfo getClsRef() {
		return (ClassInfo) replaceObj;
	}

	public InsnArg getVarRef() {
		return (InsnArg) replaceObj;
	}

	@Override
	public AType<FieldReplaceAttr> getType() {
		return AType.FIELD_REPLACE;
	}

	@Override
	public String toString() {
		return "REPLACE: " + replaceType + " " + replaceObj;
	}
}
