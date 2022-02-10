package jadx.gui.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jadx.gui.treemodel.TextNode;

import java.lang.reflect.Field;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class JumpManagerTest {
	private JumpManager jm;
	Field currentPos = null;

	@BeforeEach
	public void setup() throws NoSuchFieldException {
		jm = new JumpManager();
		currentPos = JumpManager.class.
				getDeclaredField("currentPos");
		currentPos.setAccessible(true);
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
		return new JumpPosition(new TextNode(""), 0, 0);
	}

	/*
	* test finite state machine
	*
	* */
	@Test
	public void testNavigation3() throws IllegalAccessException {
		//first click, not jump
		JumpPosition pos1 = makeJumpPos();
		if(jm.size() == 0){
		}
		assertThat((Integer) currentPos.get(jm), is(0));

		//second click different class/ function/ filed
		JumpPosition pos2 = makeJumpPos();
		jm.addPosition(pos1);	//add current position
		jm.addPosition(pos2);	//add jump position

		assertThat((Integer) currentPos.get(jm), is(1));


	}
}
