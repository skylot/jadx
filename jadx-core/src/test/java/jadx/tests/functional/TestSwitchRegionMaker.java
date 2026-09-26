package jadx.tests.functional;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.InsnContainer;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.SwitchRegion;
import jadx.core.dex.regions.conditions.IfRegion;
import jadx.core.dex.visitors.regions.maker.SwitchRegionMaker;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSwitchRegionMaker {

	@Test
	public void testAppendBreakWrapsIfRegion() {
		Region parent = new Region(null);
		IfRegion ifRegion = new IfRegion(parent);
		ifRegion.updateCondition(new BlockNode(1, 1, 0));
		parent.add(ifRegion);

		SwitchRegion switchRegion = new SwitchRegion(parent, new BlockNode(2, 2, 0));

		assertThat(SwitchRegionMaker.appendBreak(null, ifRegion, switchRegion)).isTrue();

		Region wrapper = (Region) parent.getSubBlocks().get(0);
		assertThat(wrapper.getSubBlocks().get(0)).isSameAs(ifRegion);
		assertThat(ifRegion.getParent()).isSameAs(wrapper);
		assertBreakContainer(wrapper.getSubBlocks().get(1));
	}

	@Test
	public void testAppendBreakReplacesSwitchCaseContainer() {
		SwitchRegion switchRegion = new SwitchRegion(null, new BlockNode(1, 1, 0));
		IfRegion caseRegion = new IfRegion(switchRegion);
		caseRegion.updateCondition(new BlockNode(2, 2, 0));
		switchRegion.addCase(Collections.<Object>singletonList(1), caseRegion);

		assertThat(SwitchRegionMaker.appendBreak(null, caseRegion, switchRegion)).isTrue();

		Region wrapper = (Region) switchRegion.getCases().get(0).getContainer();
		assertThat(wrapper.getSubBlocks().get(0)).isSameAs(caseRegion);
		assertThat(caseRegion.getParent()).isSameAs(wrapper);
		assertBreakContainer(wrapper.getSubBlocks().get(1));
	}

	private static void assertBreakContainer(IContainer container) {
		assertThat(container).isInstanceOf(InsnContainer.class);
		InsnContainer insnContainer = (InsnContainer) container;
		assertThat(insnContainer.getInstructions())
				.singleElement()
				.satisfies(insn -> assertThat(insn.getType()).isEqualTo(InsnType.BREAK));
	}
}
