package jadx.core.dex.attributes;

import jadx.core.dex.instructions.PhiInsn;

import java.util.LinkedList;
import java.util.List;

public class PhiListAttr implements IAttribute {

	private final List<PhiInsn> list = new LinkedList<PhiInsn>();

	@Override
	public AttributeType getType() {
		return AttributeType.PHI_LIST;
	}

	public List<PhiInsn> getList() {
		return list;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("PHI: ");
		for (PhiInsn phiInsn : list) {
			sb.append('r').append(phiInsn.getResult().getRegNum()).append(" ");
		}
		return sb.toString();
	}
}
