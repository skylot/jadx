package jadx.core.utils.android;

import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.core.export.ApplicationParams;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.xmlgen.ResContainer;
import jadx.core.xmlgen.XmlSecurity;

public class AndroidManifestParser {
	private static final Logger LOG = LoggerFactory.getLogger(AndroidManifestParser.class);

	public static final int APPLICATION_LABEL = 1;
	public static final int MIN_SDK_VERSION = 1 << 1;
	public static final int TARGET_SDK_VERSION = 1 << 2;
	public static final int VERSION_CODE = 1 << 3;
	public static final int VERSION_NAME = 1 << 4;
	public static final int MAIN_ACTIVITY = 1 << 5;
	public static final int ALL = 0xffffff;

	private final Document androidManifest;
	private final Document appStrings;
	private final int parseFlags;

	private ApplicationParams applicationParams = null;

	public AndroidManifestParser(ResourceFile androidManifestRes, int parseFlags) {
		this(androidManifestRes, null, parseFlags);
	}

	public AndroidManifestParser(ResourceFile androidManifestRes, ResContainer appStrings, int parseFlags) {
		this.parseFlags = parseFlags;

		this.androidManifest = parseAndroidManifest(androidManifestRes);
		this.appStrings = parseAppStrings(appStrings);

		if (isManifestFound()) {
			checkFlags();
			parseAttributes();
		}
	}

	public boolean isManifestFound() {
		return androidManifest != null;
	}

	public ApplicationParams getParseResults() {
		return applicationParams;
	}

	@Nullable
	public static ResourceFile getAndroidManifest(List<ResourceFile> resources) {
		return resources.stream()
				.filter(resourceFile -> resourceFile.getType() == ResourceType.MANIFEST)
				.findFirst()
				.orElse(null);
	}

	private void checkFlags() {
		if (hasFlag(APPLICATION_LABEL) && appStrings == null) {
			throw new IllegalArgumentException("APPLICATION_LABEL flag requires non null appStrings");
		}
	}

	private void parseAttributes() {
		String applicationLabel = null;
		Integer minSdkVersion = null;
		Integer targetSdkVersion = null;
		Integer versionCode = null;
		String versionName = null;
		String mainActivity = null;

		Element manifest = (Element) androidManifest.getElementsByTagName("manifest").item(0);
		Element usesSdk = (Element) androidManifest.getElementsByTagName("uses-sdk").item(0);

		if (hasFlag(APPLICATION_LABEL)) {
			applicationLabel = getApplicationLabel();
		}
		if (hasFlag(MIN_SDK_VERSION)) {
			minSdkVersion = Integer.valueOf(usesSdk.getAttribute("android:minSdkVersion"));
		}
		if (hasFlag(TARGET_SDK_VERSION)) {
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
		if (hasFlag(VERSION_CODE)) {
			versionCode = Integer.valueOf(manifest.getAttribute("android:versionCode"));
		}
		if (hasFlag(VERSION_NAME)) {
			versionName = manifest.getAttribute("android:versionName");
		}
		if (hasFlag(MAIN_ACTIVITY)) {
			mainActivity = getMainActivityName();
		}

		applicationParams = new ApplicationParams(applicationLabel, minSdkVersion, targetSdkVersion, versionCode,
				versionName, mainActivity);
	}

	private String getApplicationLabel() {
		Element application = (Element) androidManifest.getElementsByTagName("application").item(0);
		if (application.hasAttribute("android:label")) {
			String appLabelName = application.getAttribute("android:label");
			if (appLabelName.startsWith("@string")) {
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

	private boolean hasFlag(int flag) {
		return (parseFlags & flag) != 0;
	}

	private String getMainActivityName() {
		String mainActivityName = getMainActivityNameThroughActivityTag();
		if (mainActivityName == null) {
			mainActivityName = getMainActivityNameThroughActivityAliasTag();
		}
		return mainActivityName;
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

	private static Document parseXml(String xmlContent) {
		try {
			DocumentBuilder builder = XmlSecurity.getSecureDbf().newDocumentBuilder();
			Document document = builder.parse(new InputSource(new StringReader(xmlContent)));

			document.getDocumentElement().normalize();

			return document;
		} catch (Exception e) {
			throw new JadxRuntimeException("Can not parse xml content", e);
		}
	}

	private static Document parseAppStrings(ResContainer appStrings) {
		if (appStrings == null) {
			return null;
		}

		String content = appStrings.getText().getCodeStr();

		return parseXml(content);
	}

	private static Document parseAndroidManifest(ResourceFile androidManifest) {
		String content = androidManifest.loadContent().getText().getCodeStr();

		return parseXml(content);
	}
}
