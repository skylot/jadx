package jadx.tests.functional;

import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.IfOp;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.regions.conditions.Compare;
import jadx.core.dex.regions.conditions.IfCondition;

import org.junit.Test;

import static jadx.core.dex.regions.conditions.IfCondition.Mode;
import static jadx.core.dex.regions.conditions.IfCondition.merge;
import static jadx.core.dex.regions.conditions.IfCondition.not;
import static jadx.core.dex.regions.conditions.IfCondition.simplify;
import static org.junit.Assert.assertEquals;

public class TestIfCondition {

	private static IfCondition makeCondition(IfOp op, InsnArg a, InsnArg b) {
		return IfCondition.fromIfNode(new IfNode(op, -1, a, b));
	}

	private static IfCondition makeSimpleCondition() {
		return makeCondition(IfOp.EQ, mockArg(), LiteralArg.TRUE);
	}

	private static IfCondition makeNegCondition() {
		return makeCondition(IfOp.NE, mockArg(), LiteralArg.TRUE);
	}

	private static InsnArg mockArg() {
		return InsnArg.reg(0, ArgType.INT);
	}

	@Test
	public void testNormalize() {
		// 'a != false' => 'a == true'
		InsnArg a = mockArg();
		IfCondition c = makeCondition(IfOp.NE, a, LiteralArg.FALSE);
		IfCondition simp = simplify(c);

		assertEquals(simp.getMode(), Mode.COMPARE);
		Compare compare = simp.getCompare();
		assertEquals(compare.getA(), a);
		assertEquals(compare.getB(), LiteralArg.TRUE);
	}

	@Test
	public void testMerge() {
		IfCondition a = makeSimpleCondition();
		IfCondition b = makeSimpleCondition();
		IfCondition c = merge(Mode.OR, a, b);

		assertEquals(c.getMode(), Mode.OR);
		assertEquals(c.first(), a);
		assertEquals(c.second(), b);
	}

	@Test
	public void testSimplifyNot() {
		// !(!a) => a
		IfCondition a = not(not(makeSimpleCondition()));
		assertEquals(simplify(a), a);
	}

	@Test
	public void testSimplifyNot2() {
		// !(!a) => a
		IfCondition a = not(makeNegCondition());
		assertEquals(simplify(a), a);
	}

	@Test
	public void testSimplify() {
		// '!(!a || !b)' => 'a && b'
		IfCondition a = makeSimpleCondition();
		IfCondition b = makeSimpleCondition();
		IfCondition c = not(merge(Mode.OR, not(a), not(b)));
		IfCondition simp = simplify(c);

		assertEquals(simp.getMode(), Mode.AND);
		assertEquals(simp.first(), a);
		assertEquals(simp.second(), b);
	}

	@Test
	public void testSimplify2() {
		// '(!a || !b) && !c' => '!((a && b) || c)'
		IfCondition a = makeSimpleCondition();
		IfCondition b = makeSimpleCondition();
		IfCondition c = makeSimpleCondition();
		IfCondition cond = merge(Mode.AND, merge(Mode.OR, not(a), not(b)), not(c));
		IfCondition simp = simplify(cond);

		assertEquals(simp.getMode(), Mode.NOT);
		IfCondition f = simp.first();
		assertEquals(f.getMode(), Mode.OR);
		assertEquals(f.first().getMode(), Mode.AND);
		assertEquals(f.first().first(), a);
		assertEquals(f.first().second(), b);
		assertEquals(f.second(), c);
	}
}
