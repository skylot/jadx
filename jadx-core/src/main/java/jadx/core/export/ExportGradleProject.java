package jadx.core.export;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import jadx.api.ResourceFile;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;
import jadx.core.xmlgen.ResContainer;
import jadx.core.xmlgen.XmlSecurity;

public class ExportGradleProject {

	private static final Logger LOG = LoggerFactory.getLogger(ExportGradleProject.class);

	private static final Pattern ILLEGAL_GRADLE_CHARS = Pattern.compile("[/\\\\:>\"?*|]");

	private static final Set<String> IGNORE_CLS_NAMES = new HashSet<>(Arrays.asList(
			"R",
			"BuildConfig"));

	private final RootNode root;
	private final File projectDir;
	private final File appDir;
	private final File srcOutDir;
	private final File resOutDir;
	private final ApplicationParams applicationParams;

	public ExportGradleProject(RootNode root, File projectDir, ResourceFile androidManifest, ResContainer appStrings) {
		this.root = root;
		this.projectDir = projectDir;
		this.appDir = new File(projectDir, "app");
		this.srcOutDir = new File(appDir, "src/main/java");
		this.resOutDir = new File(appDir, "src/main");
		this.applicationParams = getApplicationParams(
				parseAndroidManifest(androidManifest),
				parseAppStrings(appStrings));
	}

	public void init() {
		try {
			FileUtils.makeDirs(srcOutDir);
			FileUtils.makeDirs(resOutDir);
			saveProjectBuildGradle();
			saveApplicationBuildGradle();
			saveSettingsGradle();
			skipGeneratedClasses();
		} catch (Exception e) {
			throw new JadxRuntimeException("Gradle export failed", e);
		}
	}

	private void saveProjectBuildGradle() throws IOException {
		TemplateFile tmpl = TemplateFile.fromResources("/export/build.gradle.tmpl");
		tmpl.save(new File(projectDir, "build.gradle"));
	}

	private void saveSettingsGradle() throws IOException {
		TemplateFile tmpl = TemplateFile.fromResources("/export/settings.gradle.tmpl");

		tmpl.add("applicationName", ILLEGAL_GRADLE_CHARS.matcher(applicationParams.getApplicationName()).replaceAll(""));
		tmpl.save(new File(projectDir, "settings.gradle"));
	}

	private void saveApplicationBuildGradle() throws IOException {
		TemplateFile tmpl = TemplateFile.fromResources("/export/app.build.gradle.tmpl");
		String appPackage = root.getAppPackage();

		if (appPackage == null) {
			appPackage = "UNKNOWN";
		}

		tmpl.add("applicationId", appPackage);
		tmpl.add("minSdkVersion", applicationParams.getMinSdkVersion());
		tmpl.add("targetSdkVersion", applicationParams.getTargetSdkVersion());
		tmpl.add("versionCode", applicationParams.getVersionCode());
		tmpl.add("versionName", applicationParams.getVersionName());
		tmpl.save(new File(appDir, "build.gradle"));
	}

	private void skipGeneratedClasses() {
		for (ClassNode cls : root.getClasses()) {
			String shortName = cls.getClassInfo().getShortName();
			if (IGNORE_CLS_NAMES.contains(shortName)) {
				cls.add(AFlag.DONT_GENERATE);
				LOG.debug("Skip class: {}", cls);
			}
		}
	}

	private ApplicationParams getApplicationParams(Document androidManifest, Document appStrings) {
		Element manifest = (Element) androidManifest.getElementsByTagName("manifest").item(0);
		Element usesSdk = (Element) androidManifest.getElementsByTagName("uses-sdk").item(0);
		Element application = (Element) androidManifest.getElementsByTagName("application").item(0);

		Integer versionCode = Integer.valueOf(manifest.getAttribute("android:versionCode"));
		String versionName = manifest.getAttribute("android:versionName");
		Integer minSdk = Integer.valueOf(usesSdk.getAttribute("android:minSdkVersion"));
		String stringTargetSdk = usesSdk.getAttribute("android:targetSdkVersion");
		Integer targetSdk = stringTargetSdk.isEmpty() ? minSdk : Integer.valueOf(stringTargetSdk);
		String appName = "UNKNOWN";

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
						appName = strings.item(i).getTextContent();
						break;
					}
				}
			} else {
				appName = appLabelName;
			}
		}
		return new ApplicationParams(appName, minSdk, targetSdk, versionCode, versionName);
	}

	private Document parseXml(String xmlContent) {
		try {
			DocumentBuilder builder = XmlSecurity.getSecureDbf().newDocumentBuilder();
			Document document = builder.parse(new InputSource(new StringReader(xmlContent)));

			document.getDocumentElement().normalize();

			return document;
		} catch (Exception e) {
			throw new JadxRuntimeException("Can not parse xml content", e);
		}
	}

	private Document parseAppStrings(ResContainer appStrings) {
		String content = appStrings.getText().getCodeStr();

		return parseXml(content);
	}

	private Document parseAndroidManifest(ResourceFile androidManifest) {
		String content = androidManifest.loadContent().getText().getCodeStr();

		return parseXml(content);
	}

	public File getSrcOutDir() {
		return srcOutDir;
	}

	public File getResOutDir() {
		return resOutDir;
	}
}
