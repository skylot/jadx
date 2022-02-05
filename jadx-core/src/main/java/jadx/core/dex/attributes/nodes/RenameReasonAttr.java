package jadx.core.dex.attributes.nodes;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.AttrNode;

public class RenameReasonAttr implements IJadxAttribute {

	public static RenameReasonAttr forNode(AttrNode node) {
		RenameReasonAttr renameReasonAttr = node.get(AType.RENAME_REASON);
		if (renameReasonAttr != null) {
			return renameReasonAttr;
		}
		RenameReasonAttr newAttr = new RenameReasonAttr();
		node.addAttr(newAttr);
		return newAttr;
	}

	private String description;

	public RenameReasonAttr() {
		this.description = "";
	}

	public RenameReasonAttr(String description) {
		this.description = description;
	}

	public RenameReasonAttr(AttrNode node) {
		RenameReasonAttr renameReasonAttr = node.get(AType.RENAME_REASON);
		if (renameReasonAttr != null) {
			this.description = renameReasonAttr.description;
		} else {
			this.description = "";
		}
	}

	public RenameReasonAttr(AttrNode node, boolean notValid, boolean notPrintable) {
		this(node);
		if (notValid) {
			notValid();
		}
		if (notPrintable) {
			notPrintable();
		}
	}

	public RenameReasonAttr notValid() {
		return append("not valid java name");
	}

	public RenameReasonAttr notPrintable() {
		return append("contains not printable characters");
	}

	public RenameReasonAttr append(String reason) {
		if (description.isEmpty()) {
			description += reason;
		} else {
			description += " and " + reason;
		}
		return this;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public AType<RenameReasonAttr> getAttrType() {
		return AType.RENAME_REASON;
	}

	@Override
	public String toString() {
		return "RENAME_REASON:" + description;
	}
}
