package jadx.gui.treemodel;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import jadx.api.ResourceFile;

/**
 * Resource inside resource file.
 * Add base file prefix to distinguish from other files.
 */
public class JSubResource extends JResource {
	public static final String SUB_RES_PREFIX = ":/";

	public JResource baseRes;

	public JSubResource(JResource baseRes, @Nullable ResourceFile resFile, String name, String shortName, JResType type) {
		super(resFile, name, shortName, type);
		this.baseRes = Objects.requireNonNull(baseRes);
	}

	public JResource getBaseRes() {
		return baseRes;
	}

	@Override
	public String makeLongString() {
		return baseRes.makeLongString() + SUB_RES_PREFIX + super.makeLongString();
	}

	@Override
	public final boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof JSubResource)) {
			return false;
		}
		JSubResource other = (JSubResource) o;
		return baseRes.equals(other.baseRes)
				&& getName().equals(other.getName())
				&& getType().equals(other.getType());
	}

	@Override
	public int hashCode() {
		return baseRes.hashCode() + 31 * super.hashCode();
	}
}
