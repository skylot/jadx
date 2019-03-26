package jadx.gui.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jadx.gui.treemodel.TextNode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

class JumpManagerTest {
	private JumpManager jm;

	@BeforeEach
	public void setup() {
		jm = new JumpManager();
	}

	@Test
	public void testEmptyHistory() {
		assertThat(jm.getPrev(), nullValue());
		assertThat(jm.getNext(), nullValue());
	}

	@Test
	public void testEmptyHistory2() {
		assertThat(jm.getPrev(), nullValue());
		assertThat(jm.getNext(), nullValue());
		assertThat(jm.getPrev(), nullValue());
		assertThat(jm.getNext(), nullValue());
		assertThat(jm.getPrev(), nullValue());
	}

	@Test
	public void testOneElement() {
		jm.addPosition(makeJumpPos());

		assertThat(jm.getPrev(), nullValue());
		assertThat(jm.getNext(), nullValue());
	}

	@Test
	public void testTwoElements() {
		JumpPosition pos1 = makeJumpPos();
		jm.addPosition(pos1);
		JumpPosition pos2 = makeJumpPos();
		jm.addPosition(pos2);

		assertThat(jm.getPrev(), sameInstance(pos1));
		assertThat(jm.getPrev(), nullValue());
		assertThat(jm.getNext(), sameInstance(pos2));
		assertThat(jm.getNext(), nullValue());
	}

	@Test
	public void testNavigation() {
		JumpPosition pos1 = makeJumpPos();
		jm.addPosition(pos1);
		// 1@
		JumpPosition pos2 = makeJumpPos();
		jm.addPosition(pos2);
		// 1 - 2@
		assertThat(jm.getPrev(), sameInstance(pos1));
		// 1@ - 2
		JumpPosition pos3 = makeJumpPos();
		jm.addPosition(pos3);
		// 1 - 3@
		assertThat(jm.getNext(), nullValue());
		assertThat(jm.getPrev(), sameInstance(pos1));
		// 1@ - 3
		assertThat(jm.getNext(), sameInstance(pos3));
	}

	@Test
	public void testNavigation2() {
		JumpPosition pos1 = makeJumpPos();
		jm.addPosition(pos1);
		// 1@
		JumpPosition pos2 = makeJumpPos();
		jm.addPosition(pos2);
		// 1 - 2@
		JumpPosition pos3 = makeJumpPos();
		jm.addPosition(pos3);
		// 1 - 2 - 3@
		JumpPosition pos4 = makeJumpPos();
		jm.addPosition(pos4);
		// 1 - 2 - 3 - 4@
		assertThat(jm.getPrev(), sameInstance(pos3));
		// 1 - 2 - 3@ - 4
		assertThat(jm.getPrev(), sameInstance(pos2));
		// 1 - 2@ - 3 - 4
		JumpPosition pos5 = makeJumpPos();
		jm.addPosition(pos5);
		// 1 - 2 - 5@
		assertThat(jm.getNext(), nullValue());
		assertThat(jm.getNext(), nullValue());
		assertThat(jm.getPrev(), sameInstance(pos2));
		// 1 - 2@ - 5
		assertThat(jm.getPrev(), sameInstance(pos1));
		// 1@ - 2 - 5
		assertThat(jm.getPrev(), nullValue());
		assertThat(jm.getNext(), sameInstance(pos2));
		// 1 - 2@ - 5
		assertThat(jm.getNext(), sameInstance(pos5));
		// 1 - 2 - 5@
		assertThat(jm.getNext(), nullValue());
	}

	@Test
	public void addSame() {
		JumpPosition pos = makeJumpPos();
		jm.addPosition(pos);
		jm.addPosition(pos);

		assertThat(jm.getPrev(), nullValue());
		assertThat(jm.getNext(), nullValue());
	}

	private JumpPosition makeJumpPos() {
		return new JumpPosition(new TextNode(""), 0);
	}
}
