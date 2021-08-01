package jadx.tests.functional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.AttributeStorage;

import static jadx.core.dex.attributes.AFlag.SYNTHETIC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class AttributeStorageTest {
	private AttributeStorage storage;

	@BeforeEach
	public void setup() {
		storage = new AttributeStorage();
	}

	@Test
	public void testAdd() {
		storage.add(SYNTHETIC);
		assertThat(storage.contains(SYNTHETIC), is(true));
	}

	@Test
	public void testRemove() {
		storage.add(SYNTHETIC);
		storage.remove(SYNTHETIC);
		assertThat(storage.contains(SYNTHETIC), is(false));
	}

	public static final AType<TestAttr> TEST = new AType<>();

	public static class TestAttr implements IJadxAttribute {
		@Override
		public AType<TestAttr> getAttrType() {
			return TEST;
		}
	}

	@Test
	public void testAddAttribute() {
		TestAttr attr = new TestAttr();
		storage.add(attr);

		assertThat(storage.contains(TEST), is(true));
		assertThat(storage.get(TEST), is(attr));
	}

	@Test
	public void testRemoveAttribute() {
		TestAttr attr = new TestAttr();
		storage.add(attr);
		storage.remove(attr);

		assertThat(storage.contains(TEST), is(false));
		assertThat(storage.get(TEST), nullValue());
	}

	@Test
	public void testRemoveOtherAttribute() {
		TestAttr attr = new TestAttr();
		storage.add(attr);
		storage.remove(new TestAttr());

		assertThat(storage.contains(TEST), is(true));
		assertThat(storage.get(TEST), is(attr));
	}

	@Test
	public void clear() {
		storage.add(SYNTHETIC);
		storage.add(new TestAttr());
		storage.clear();

		assertThat(storage.contains(SYNTHETIC), is(false));
		assertThat(storage.contains(TEST), is(false));
		assertThat(storage.get(TEST), nullValue());
	}
}
