package jadx.tests.functional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.AttributeStorage;

import static jadx.core.dex.attributes.AFlag.SYNTHETIC;
import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class AttributeStorageTest {
	private AttributeStorage storage;

	@BeforeEach
	public void setup() {
		storage = new AttributeStorage();
	}

	@Test
	public void testAdd() {
		storage.add(SYNTHETIC);
		assertThat(storage.contains(SYNTHETIC)).isTrue();
	}

	@Test
	public void testRemove() {
		storage.add(SYNTHETIC);
		storage.remove(SYNTHETIC);
		assertThat(storage.contains(SYNTHETIC)).isFalse();
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

		assertThat(storage.contains(TEST)).isTrue();
		assertThat(storage.get(TEST)).isEqualTo(attr);
	}

	@Test
	public void testRemoveAttribute() {
		TestAttr attr = new TestAttr();
		storage.add(attr);
		storage.remove(attr);

		assertThat(storage.contains(TEST)).isFalse();
		assertThat(storage.get(TEST)).isNull();
	}

	@Test
	public void testRemoveOtherAttribute() {
		TestAttr attr = new TestAttr();
		storage.add(attr);
		storage.remove(new TestAttr());

		assertThat(storage.contains(TEST)).isTrue();
		assertThat(storage.get(TEST)).isEqualTo(attr);
	}
}
