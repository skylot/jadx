package jadx.core.clsp;

/**
 * Class node in classpath graph
 */
public class NClass {

	private final String name;
	private NClass[] parents;
	private int id;

	public NClass(String name, int id) {
		this.name = name;
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public NClass[] getParents() {
		return parents;
	}

	public void setParents(NClass[] parents) {
		this.parents = parents;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		NClass nClass = (NClass) o;
		return name.equals(nClass.name);
	}

	@Override
	public String toString() {
		return name;
	}
}
