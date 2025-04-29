package jadx.core.utils.android;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.api.security.IJadxSecurity;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.xmlgen.ResContainer;

public class AndroidManifestParser {
	private final Document androidManifest;
	private final @Nullable Document appStrings;
	private final EnumSet<AppAttribute> parseAttrs;
	private final IJadxSecurity security;

	public AndroidManifestParser(ResourceFile androidManifestRes, EnumSet<AppAttribute> parseAttrs, IJadxSecurity security) {
		this(androidManifestRes, null, parseAttrs, security);
	}

	public AndroidManifestParser(ResourceFile androidManifestRes, @Nullable ResContainer appStrings,
			EnumSet<AppAttribute> parseAttrs, IJadxSecurity security) {
		this.parseAttrs = parseAttrs;
		this.security = Objects.requireNonNull(security);

		this.androidManifest = parseAndroidManifest(androidManifestRes);
		this.appStrings = parseAppStrings(appStrings);
	}

	public boolean isManifestFound() {
		return androidManifest != null;
	}

	@Nullable
	public static ResourceFile getAndroidManifest(List<ResourceFile> resources) {
		return resources.stream()
				.filter(resourceFile -> resourceFile.getType() == ResourceType.MANIFEST)
				.findFirst()
				.orElse(null);
	}

	public ApplicationParams parse() {
		if (!isManifestFound()) {
			throw new JadxRuntimeException("AndroidManifest.xml is missing");
		}

		return parseAttributes();
	}

	private ApplicationParams parseAttributes() {
		String applicationLabel = null;
		Integer minSdkVersion = null;
		Integer targetSdkVersion = null;
		Integer versionCode = null;
		String versionName = null;
		String mainActivity = null;
		String application = null;

		@Nullable
		Element manifest = (Element) androidManifest.getElementsByTagName("manifest").item(0);
		@Nullable
		Element usesSdk = (Element) androidManifest.getElementsByTagName("uses-sdk").item(0);

		if (parseAttrs.contains(AppAttribute.APPLICATION_LABEL)) {
			applicationLabel = getApplicationLabel();
		}
		if (usesSdk != null) {
			if (parseAttrs.contains(AppAttribute.MIN_SDK_VERSION)) {
				minSdkVersion = Integer.valueOf(usesSdk.getAttribute("android:minSdkVersion"));
			}
			if (parseAttrs.contains(AppAttribute.TARGET_SDK_VERSION)) {
				String stringTargetSdk = usesSdk.getAttribute("android:targetSdkVersion");
				if (!stringTargetSdk.isEmpty()) {
					targetSdkVersion = Integer.valueOf(stringTargetSdk);
				} else {
					if (minSdkVersion == null) {
						minSdkVersion = Integer.valueOf(usesSdk.getAttribute("android:minSdkVersion"));
					}
					targetSdkVersion = minSdkVersion;
				}
			}
		}
		if (manifest != null) {
			if (parseAttrs.contains(AppAttribute.VERSION_CODE)) {
				versionCode = Integer.valueOf(manifest.getAttribute("android:versionCode"));
			}
			if (parseAttrs.contains(AppAttribute.VERSION_NAME)) {
				versionName = manifest.getAttribute("android:versionName");
			}
		}
		if (parseAttrs.contains(AppAttribute.MAIN_ACTIVITY)) {
			mainActivity = getMainActivityName();
		}
		if (parseAttrs.contains(AppAttribute.APPLICATION)) {
			application = getApplicationName();
		}

		return new ApplicationParams(applicationLabel, minSdkVersion, targetSdkVersion, versionCode,
				versionName, mainActivity, application);
	}

	private String getApplicationLabel() {
		Element application = (Element) androidManifest.getElementsByTagName("application").item(0);
		if (application.hasAttribute("android:label")) {
			String appLabelName = application.getAttribute("android:label");
			if (appLabelName.startsWith("@string")) {
				if (appStrings == null) {
					throw new IllegalArgumentException("APPLICATION_LABEL attribute requires non null appStrings");
				}
				appLabelName = appLabelName.split("/")[1];
				NodeList strings = appStrings.getElementsByTagName("string");

				for (int i = 0; i < strings.getLength(); i++) {
					String stringName = strings.item(i)
							.getAttributes()
							.getNamedItem("name")
							.getNodeValue();

					if (stringName.equals(appLabelName)) {
						return strings.item(i).getTextContent();
					}
				}
			} else {
				return appLabelName;
			}
		}

		return "UNKNOWN";
	}

	private String getMainActivityName() {
		String mainActivityName = getMainActivityNameThroughActivityTag();
		if (mainActivityName == null) {
			mainActivityName = getMainActivityNameThroughActivityAliasTag();
		}
		return mainActivityName;
	}

	private String getApplicationName() {
		Element application = (Element) androidManifest.getElementsByTagName("application").item(0);
		if (application.hasAttribute("android:name")) {
			return application.getAttribute("android:name");
		}
		return null;
	}

	private String getMainActivityNameThroughActivityAliasTag() {
		NodeList activityAliasNodes = androidManifest.getElementsByTagName("activity-alias");
		for (int i = 0; i < activityAliasNodes.getLength(); i++) {
			Element activityElement = (Element) activityAliasNodes.item(i);
			if (isMainActivityElement(activityElement)) {
				return activityElement.getAttribute("android:targetActivity");
			}
		}
		return null;
	}

	private String getMainActivityNameThroughActivityTag() {
		NodeList activityNodes = androidManifest.getElementsByTagName("activity");
		for (int i = 0; i < activityNodes.getLength(); i++) {
			Element activityElement = (Element) activityNodes.item(i);
			if (isMainActivityElement(activityElement)) {
				return activityElement.getAttribute("android:name");
			}
		}
		return null;
	}

	private boolean isMainActivityElement(Element element) {
		NodeList intentFilterNodes = element.getElementsByTagName("intent-filter");
		for (int j = 0; j < intentFilterNodes.getLength(); j++) {
			Element intentFilterElement = (Element) intentFilterNodes.item(j);
			NodeList actionNodes = intentFilterElement.getElementsByTagName("action");
			NodeList categoryNodes = intentFilterElement.getElementsByTagName("category");

			boolean isMainAction = false;
			boolean isLauncherCategory = false;

			for (int k = 0; k < actionNodes.getLength(); k++) {
				Element actionElement = (Element) actionNodes.item(k);
				String actionName = actionElement.getAttribute("android:name");
				if ("android.intent.action.MAIN".equals(actionName)) {
					isMainAction = true;
					break;
				}
			}

			for (int k = 0; k < categoryNodes.getLength(); k++) {
				Element categoryElement = (Element) categoryNodes.item(k);
				String categoryName = categoryElement.getAttribute("android:name");
				if ("android.intent.category.LAUNCHER".equals(categoryName)) {
					isLauncherCategory = true;
					break;
				}
			}

			if (isMainAction && isLauncherCategory) {
				return true;
			}
		}
		return false;
	}

	private Document parseXml(String xmlContent) {
		try (InputStream xmlStream = new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8))) {
			Document document = security.parseXml(xmlStream);
			document.getDocumentElement().normalize();
			return document;
		} catch (Exception e) {
			throw new JadxRuntimeException("Can not parse xml content", e);
		}
	}

	private Document parseAppStrings(ResContainer appStrings) {
		if (appStrings == null) {
			return null;
		}
		String content = appStrings.getText().getCodeStr();
		return parseXml(content);
	}

	private Document parseAndroidManifest(ResourceFile androidManifest) {
		if (androidManifest == null) {
			return null;
		}
		String content = androidManifest.loadContent().getText().getCodeStr();
		return parseXml(content);
	}
}
