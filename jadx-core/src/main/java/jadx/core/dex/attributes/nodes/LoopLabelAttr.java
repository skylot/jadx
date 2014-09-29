package jadx.core.dex.attributes.nodes;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;

public class LoopLabelAttr implements IAttribute {

	private final LoopInfo loop;

	public LoopLabelAttr(LoopInfo loop) {
		this.loop = loop;
	}

	public LoopInfo getLoop() {
		return loop;
	}

	@Override
	public AType<LoopLabelAttr> getType() {
		return AType.LOOP_LABEL;
	}

	@Override
	public String toString() {
		return "LOOP_LABEL: " + loop;
	}
}
