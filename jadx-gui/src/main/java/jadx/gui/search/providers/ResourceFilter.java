package jadx.gui.search.providers;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jadx.api.resources.ResourceContentType;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.InvalidDataException;

import static jadx.api.resources.ResourceContentType.CONTENT_BINARY;
import static jadx.api.resources.ResourceContentType.CONTENT_TEXT;

public class ResourceFilter {

	private static final ResourceFilter ANY = new ResourceFilter(Set.of(), Set.of());

	private static final String VAR_TEXT = "$TEXT";
	private static final String VAR_BIN = "$BIN";

	public static final String DEFAULT_STR = VAR_TEXT;

	public static ResourceFilter parse(String filterStr) {
		String str = filterStr.trim();
		if (str.isEmpty() || str.equals("*")) {
			return ANY;
		}
		Set<ResourceContentType> contentTypes = EnumSet.noneOf(ResourceContentType.class);
		Set<String> extSet = new LinkedHashSet<>();
		String[] parts = filterStr.split("[|, ]");
		for (String part : parts) {
			if (part.isEmpty()) {
				continue;
			}
			if (part.startsWith("$")) {
				switch (part) {
					case VAR_TEXT:
						contentTypes.add(CONTENT_TEXT);
						break;
					case VAR_BIN:
						contentTypes.add(CONTENT_BINARY);
						break;
					default:
						throw new InvalidDataException("Unknown var name: " + part);
				}
			} else {
				extSet.add(part);
			}
		}
		return new ResourceFilter(contentTypes, extSet);
	}

	public static String format(ResourceFilter filter) {
		if (filter.isAnyFile()) {
			return "*";
		}
		List<String> list = new ArrayList<>();
		Set<ResourceContentType> types = filter.getContentTypes();
		if (types.contains(CONTENT_TEXT)) {
			list.add(VAR_TEXT);
		}
		if (types.contains(CONTENT_BINARY)) {
			list.add(VAR_BIN);
		}
		list.addAll(filter.getExtSet());
		return Utils.listToString(list, "|");
	}

	public static String withContentType(String filterStr, Set<ResourceContentType> contentTypes) {
		ResourceFilter filter = parse(filterStr);
		return format(new ResourceFilter(contentTypes, filter.getExtSet()));
	}

	private final boolean anyFile;
	private final Set<ResourceContentType> contentTypes;
	private final Set<String> extSet;

	private ResourceFilter(Set<ResourceContentType> contentTypes, Set<String> extSet) {
		this.anyFile = contentTypes.isEmpty() && extSet.isEmpty();
		this.contentTypes = contentTypes.isEmpty() ? Set.of() : contentTypes;
		this.extSet = extSet.isEmpty() ? Set.of() : extSet;
	}

	public boolean isAnyFile() {
		return anyFile;
	}

	public Set<ResourceContentType> getContentTypes() {
		return contentTypes;
	}

	public Set<String> getExtSet() {
		return extSet;
	}

	@Override
	public String toString() {
		return format(this);
	}
}
