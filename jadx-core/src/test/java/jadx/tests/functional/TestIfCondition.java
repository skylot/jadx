package jadx.tests.functional;

import org.junit.jupiter.api.Test;

import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.IfOp;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.regions.conditions.Compare;
import jadx.core.dex.regions.conditions.IfCondition;

import static jadx.core.dex.regions.conditions.IfCondition.Mode;
import static jadx.core.dex.regions.conditions.IfCondition.Mode.AND;
import static jadx.core.dex.regions.conditions.IfCondition.Mode.COMPARE;
import static jadx.core.dex.regions.conditions.IfCondition.Mode.NOT;
import static jadx.core.dex.regions.conditions.IfCondition.Mode.OR;
import static jadx.core.dex.regions.conditions.IfCondition.merge;
import static jadx.core.dex.regions.conditions.IfCondition.not;
import static jadx.core.dex.regions.conditions.IfCondition.simplify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestIfCondition {

	private static IfCondition makeCondition(IfOp op, InsnArg a, InsnArg b) {
		return IfCondition.fromIfNode(new IfNode(op, -1, a, b));
	}

	private static IfCondition makeSimpleCondition() {
		return makeCondition(IfOp.EQ, mockArg(), LiteralArg.litTrue());
	}

	private static IfCondition makeNegCondition() {
		return makeCondition(IfOp.NE, mockArg(), LiteralArg.litTrue());
	}

	private static InsnArg mockArg() {
		return InsnArg.reg(0, ArgType.INT);
	}

	@Test
	public void testNormalize() {
		// 'a != false' => 'a == true'
		InsnArg a = mockArg();
		IfCondition c = makeCondition(IfOp.NE, a, LiteralArg.litFalse());
		IfCondition simp = simplify(c);

		assertThat(simp.getMode(), is(COMPARE));
		Compare compare = simp.getCompare();
		assertThat(compare.getA(), is(a));
		assertThat(compare.getB(), is(LiteralArg.litTrue()));
	}

	@Test
	public void testMerge() {
		IfCondition a = makeSimpleCondition();
		IfCondition b = makeSimpleCondition();
		IfCondition c = merge(Mode.OR, a, b);

		assertThat(c.getMode(), is(OR));
		assertThat(c.first(), is(a));
		assertThat(c.second(), is(b));
	}

	@Test
	public void testSimplifyNot() {
		// !(!a) => a
		IfCondition a = not(not(makeSimpleCondition()));
		assertThat(simplify(a), is(a));
	}

	@Test
	public void testSimplifyNot2() {
		// !(!a) => a
		IfCondition a = not(makeNegCondition());
		assertThat(simplify(a), is(a));
	}

	@Test
	public void testSimplify() {
		// '!(!a || !b)' => 'a && b'
		IfCondition a = makeSimpleCondition();
		IfCondition b = makeSimpleCondition();
		IfCondition c = not(merge(Mode.OR, not(a), not(b)));
		IfCondition simp = simplify(c);

		assertThat(simp.getMode(), is(AND));
		assertThat(simp.first(), is(a));
		assertThat(simp.second(), is(b));
	}

	@Test
	public void testSimplify2() {
		// '(!a || !b) && !c' => '!((a && b) || c)'
		IfCondition a = makeSimpleCondition();
		IfCondition b = makeSimpleCondition();
		IfCondition c = makeSimpleCondition();
		IfCondition cond = merge(Mode.AND, merge(Mode.OR, not(a), not(b)), not(c));
		IfCondition simp = simplify(cond);

		assertThat(simp.getMode(), is(NOT));
		IfCondition f = simp.first();
		assertThat(f.getMode(), is(OR));
		assertThat(f.first().getMode(), is(AND));
		assertThat(f.first().first(), is(a));
		assertThat(f.first().second(), is(b));
		assertThat(f.second(), is(c));
	}
}
