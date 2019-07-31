package jadx.core.dex.instructions.args;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PrimitiveTypeTest {

	@Rule public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void testGetSmaller() {
		Assert.assertEquals(PrimitiveType.ARRAY,
				PrimitiveType.getSmaller(PrimitiveType.ARRAY, PrimitiveType.ARRAY));
		Assert.assertEquals(PrimitiveType.ARRAY,
				PrimitiveType.getSmaller(PrimitiveType.ARRAY, PrimitiveType.VOID));
	}

	@Test
	public void testGetWidest() {
		Assert.assertEquals(PrimitiveType.ARRAY, PrimitiveType.getWidest(PrimitiveType.ARRAY, PrimitiveType.ARRAY));
		Assert.assertEquals(PrimitiveType.ARRAY, PrimitiveType.getWidest(PrimitiveType.ARRAY, PrimitiveType.OBJECT));
	}

	@Test
	public void testValueOfThrowsException() {
		thrown.expect(IllegalArgumentException.class);
		PrimitiveType.valueOf("A1B2C3");
	}
}
