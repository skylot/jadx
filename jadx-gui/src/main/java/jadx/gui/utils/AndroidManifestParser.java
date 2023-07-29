package jadx.gui.utils;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import io.reactivex.annotations.Nullable;

import jadx.api.JavaClass;
import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.xmlgen.XmlSecurity;
import jadx.gui.JadxWrapper;

public class AndroidManifestParser {
	private final Document mXmlDocument;

	public AndroidManifestParser(List<ResourceFile> resourceFiles) {
		mXmlDocument = parseAndroidManifest(getAndroidManifest(resourceFiles));
	}

	public boolean isResourceFound() {
		return mXmlDocument != null;
	}

	public JavaClass getMainActivity(JadxWrapper decompiler) {
		final String mainActivityName = getMainActivityName();
		if (mainActivityName == null) {
			throw new JadxRuntimeException("Failed to get main activity name from manifest");
		}
		JavaClass javaClass = decompiler.searchJavaClassByOrigClassName(mainActivityName);
		if (javaClass == null) {
			throw new JadxRuntimeException("Failed to find main activity class: " + mainActivityName);
		}
		return javaClass;
	}

	private String getMainActivityName() {
		String mainActivityName = getMainActivityNameThroughActivityTag();
		if (mainActivityName == null) {
			mainActivityName = getMainActivityNameThroughActivityAliasTag();
		}
		return mainActivityName;
	}

	private String getMainActivityNameThroughActivityAliasTag() {
		NodeList activityAliasNodes = mXmlDocument.getElementsByTagName("activity-alias");
		for (int i = 0; i < activityAliasNodes.getLength(); i++) {
			Element activityElement = (Element) activityAliasNodes.item(i);
			if (isMainActivityElement(activityElement)) {
				return activityElement.getAttribute("android:targetActivity");
			}
		}
		return null;
	}

	private String getMainActivityNameThroughActivityTag() {
		NodeList activityNodes = mXmlDocument.getElementsByTagName("activity");
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

	@Nullable
	public static ResourceFile getAndroidManifest(List<ResourceFile> resources) {
		// TODO: taken from ExportGradleTask.java: also use this function there ?
		return resources.stream()
				.filter(resourceFile -> resourceFile.getType() == ResourceType.MANIFEST)
				.findFirst()
				.orElse(null);
	}

	public static Document parseAndroidManifest(ResourceFile androidManifest) {
		if (androidManifest == null) {
			return null;
		}

		// TODO: taken from ExportGradleProject.java: also use this function there ?
		Document androidManifestDocument;
		try {
			String xmlContent = androidManifest.loadContent().getText().getCodeStr();
			DocumentBuilder builder = XmlSecurity.getSecureDbf().newDocumentBuilder();
			androidManifestDocument = builder.parse(new InputSource(new StringReader(xmlContent)));
			androidManifestDocument.getDocumentElement().normalize();
		} catch (ParserConfigurationException | SAXException | IOException ex) {
			throw new RuntimeException(ex);
		}
		return androidManifestDocument;
	}
}
