package jadx.gui.utils;

import jadx.gui.treemodel.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
		if (jm.size() == 0) {
		}
		assertThat((Integer) currentPos.get(jm), is(0));


		//second click different class/ function/ filed
		JumpPosition pos2 = makeJumpPos();
		jm.addPosition(pos1);    //add current position
		jm.addPosition(pos2);    //add jump position

		assertThat((Integer) currentPos.get(jm), is(1));
	}


	/*
	 * test finite state machine
	 * Test the transitions between state empty, 0 ,1, 2 and outOfrange.
	 * This test covers most cases in the finite model.
	 * Especially test transitions changes before adding new position and after adding new position.
	 * */

	/*
	 * Test Finite State Machine
	 */
	@Test
	public void testNavigation4() throws IllegalAccessException {
		//no click, the list is empty, try get prev,next
		assertThat((Integer) currentPos.get(jm), is(0));
		assertThat(jm.getPrev(),is(nullValue()));
		assertThat(jm.getNext(),is(nullValue()));
		//first click, not jump, try get prev,next
		JumpPosition pos0 = makeJumpPos();
		jm.addPosition(pos0);
		assertThat((Integer) currentPos.get(jm), is(0));
		assertThat(jm.getPrev(), is(nullValue()));
		assertThat(jm.getNext(), is(nullValue()));
		jm.addPosition(pos0);
		assertThat((Integer) currentPos.get(jm), is(0));

		//second click different class/ function/ filed, go to the new pos
		//try get prev,next before adding new position and after adding new positons
		JumpPosition pos1 = makeJumpPos();
		jm.addPosition(pos1);
		assertThat((Integer) currentPos.get(jm), is(1));
		assertThat(jm.getNext(), is(nullValue()));
		jm.addPosition(pos1);
		assertThat((Integer) currentPos.get(jm), is(1));
		jm.getPrev();
		assertThat((Integer) currentPos.get(jm), is(0));
		jm.getNext();
		assertThat((Integer) currentPos.get(jm), is(1));
//		jm.addPosition(pos0);
//		assertThat((Integer) currentPos.get(jm),is(0));

		//the last
		//try get prev,next before adding new position and after adding new positons
		JumpPosition pos2 = makeJumpPos();
		jm.addPosition(pos2);
		assertThat((Integer) currentPos.get(jm), is(2));
		assertThat(jm.getNext(), is(nullValue()));
		jm.addPosition(pos2);
		assertThat((Integer) currentPos.get(jm), is(2));
		jm.getPrev();
		assertThat((Integer) currentPos.get(jm), is(1));
		jm.getNext();
		assertThat((Integer) currentPos.get(jm), is(2));

		//test reset state
		jm.reset();
		jm.addPosition(pos0); //in gui, there will be a start position as current, so add it here to simulate the process
		assertThat((Integer) currentPos.get(jm), is(0));
		assertThat(jm.getPrev(), is(nullValue()));
		assertThat(jm.getNext(), is(nullValue()));
		jm.addPosition(pos0);
		assertThat((Integer) currentPos.get(jm), is(0));

		//try to go to the out of range state
		jm.addPosition(pos0);
		jm.addPosition(pos1);
		jm.addPosition(pos2);
		jm.addPosition(makeJumpPos());
		assertThat((Integer) currentPos.get(jm), is(3));
	}


	/*
	 * Test Finite State Machine
	 * When currentPos is 0, click pre, forward, new stuff, and open new project
	 * */
	@Test
	public void testNavigation5() throws IllegalAccessException {
		//[]
		jm.getPrev();
		//When currentPos is 0, click pre
		assertThat((Integer) currentPos.get(jm), is(0));

		//[]
		jm.getNext();
		//When currentPos is 0, click forward
		assertThat((Integer) currentPos.get(jm), is(0));

		JumpPosition pos1 = makeJumpPos();
		jm.addPosition(pos1);    //add jump position
		//[0@]
		//When currentPos is 0, click new stuff
		assertThat((Integer) currentPos.get(jm), is(0));

		jm.reset();
		//[]
		//When currentPos is 0, click open new project
		assertThat((Integer) currentPos.get(jm), is(0));
	}

	/*
	 * Test Finite State Machine
	 * When currentPos is 0, click new stuff
	 * When currentPos is 1, click new stuff, current stuff and forward
	 * When currentPos is 2, click current stuff and forward
	 * All test ok
	 * */
	@Test
	public void testNavigation6() throws IllegalAccessException {
		//[]
		JumpPosition pos1 = makeJumpPos();
		jm.addPosition(pos1);    //add jump position
		//[0@]
		//When currentPos is 0, click new stuff
		assertThat((Integer) currentPos.get(jm), is(0));

		JumpPosition pos2 = makeJumpPos();
		jm.addPosition(pos2);    //add jump position
		//[0 - 1@]
		//When currentPos is 1, click new stuff
		assertThat((Integer) currentPos.get(jm), is(1));

		//click current stuff
		jm.addPosition(pos2);
		//[0 - 1@]
		//When currentPos is 1, click current stuff
		assertThat((Integer) currentPos.get(jm), is(1));

		//click forward
		jm.getNext();
		//[0 - 1@]
		//When currentPos is 1, click forward
		assertThat((Integer) currentPos.get(jm), is(1));

		JumpPosition pos3 = makeJumpPos();
		jm.addPosition(pos3);    //add jump position
		//[0 - 1 - 2@]
		assertThat((Integer) currentPos.get(jm), is(2));

		//click current stuff
		jm.addPosition(pos3);
		//[0 - 1 - 2@]
		//When currentPos is 2, click current stuff
		assertThat((Integer) currentPos.get(jm), is(2));

		//click forward
		jm.getNext();
		//[0 - 1 - 2@]
		//When currentPos is 2, click forward
		assertThat((Integer) currentPos.get(jm), is(2));
	}

	/*
	 * Test Finite State Machine
	 * When currentPos is 0, click new stuff	: Tests ok
	 * When currentPos is 0, 1, 2 click open new project: Tests failed
	 * */
	@Test
	public void testNavigation7() throws IllegalAccessException {
		//[]
		JumpPosition pos1 = makeJumpPos();
		jm.addPosition(pos1);    //add jump position
		//[0@]
		//When currentPos is 0, click new stuff
		assertThat((Integer) currentPos.get(jm), is(0));
		//test open new projects on state 0
		jm.reset();
		assertThat((Integer) currentPos.get(jm), is(0));

		//test open new projects on state 1
		jm.addPosition(pos1);
		JumpPosition pos2 = makeJumpPos();
		jm.addPosition(pos2);    //add jump position
		//[0 - 1@]
		assertThat((Integer) currentPos.get(jm), is(1));

		//When currentPos is 1, click open new project
		jm.reset();
		//[]
		assertThat((Integer) currentPos.get(jm), is(0));
		//no pass this case Expected: is <0> but: was <1>


		//test open new projects on state 2
		jm.addPosition(pos1);
		jm.addPosition(pos2);
		JumpPosition pos3 = makeJumpPos();
		//[0 -1 -2@]
		jm.reset();
		assertThat((Integer) currentPos.get(jm), is(0));
		//no pass this case Expected: is <0> but: was <2>
	}
}
