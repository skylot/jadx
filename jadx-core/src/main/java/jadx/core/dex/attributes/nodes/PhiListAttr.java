package jadx.core.dex.attributes.nodes;

import java.util.ArrayList;
import java.util.List;

import jadx.api.ICodeWriter;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.RegisterArg;

public class PhiListAttr implements IJadxAttribute {

	private final List<PhiInsn> list = new ArrayList<>();

	@Override
	public AType<PhiListAttr> getAttrType() {
		return AType.PHI_LIST;
	}

	public List<PhiInsn> getList() {
		return list;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("PHI:");
		for (PhiInsn phiInsn : list) {
			RegisterArg resArg = phiInsn.getResult();
			if (resArg != null) {
				sb.append(" r").append(resArg.getRegNum());
			}
		}
		for (PhiInsn phiInsn : list) {
			sb.append(ICodeWriter.NL).append("  ").append(phiInsn);
		}
		return sb.toString();
	}
}
