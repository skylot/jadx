package jadx.gui.treemodel;

import java.io.File;
import java.security.cert.Certificate;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.android.apksig.ApkVerifier;

import jadx.api.ICodeInfo;
import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.api.impl.SimpleCodeInfo;
import jadx.gui.JadxWrapper;
import jadx.gui.ui.TabbedPane;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.panel.HtmlPanel;
import jadx.gui.utils.CertificateManager;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class ApkSignature extends JNode {
	private static final long serialVersionUID = -9121321926113143407L;

	private static final Logger LOG = LoggerFactory.getLogger(ApkSignature.class);

	private static final ImageIcon CERTIFICATE_ICON = UiUtils.openSvgIcon("nodes/styleKeyPack");

	private final transient File openFile;
	private ICodeInfo content;

	@Nullable
	public static ApkSignature getApkSignature(JadxWrapper wrapper) {
		// Only show the ApkSignature node if an AndroidManifest.xml is present.
		// Without a manifest the Google ApkVerifier refuses to work.
		File apkFile = null;
		for (ResourceFile resFile : wrapper.getResources()) {
			if (resFile.getType() == ResourceType.MANIFEST) {
				ResourceFile.ZipRef zipRef = resFile.getZipRef();
				if (zipRef != null) {
					apkFile = zipRef.getZipFile();
					break;
				}
			}
		}
		if (apkFile == null) {
			return null;
		}
		return new ApkSignature(apkFile);
	}

	public ApkSignature(File openFile) {
		this.openFile = openFile;
	}

	@Override
	public JClass getJParent() {
		return null;
	}

	@Override
	public Icon getIcon() {
		return CERTIFICATE_ICON;
	}

	@Override
	public String makeString() {
		return "APK signature";
	}

	@Override
	public ContentPanel getContentPanel(TabbedPane tabbedPane) {
		return new HtmlPanel(tabbedPane, this);
	}

	@Override
	public ICodeInfo getCodeInfo() {
		if (content != null) {
			return this.content;
		}
		ApkVerifier verifier = new ApkVerifier.Builder(openFile).build();
		try {
			ApkVerifier.Result result = verifier.verify();
			StringEscapeUtils.Builder builder = StringEscapeUtils.builder(StringEscapeUtils.ESCAPE_HTML4);
			builder.append("<h1>APK signature verification result:</h1>");

			builder.append("<p><b>");
			if (result.isVerified()) {
				builder.escape(NLS.str("apkSignature.verificationSuccess"));
			} else {
				builder.escape(NLS.str("apkSignature.verificationFailed"));
			}
			builder.append("</b></p>");

			final String err = NLS.str("apkSignature.errors");
			final String warn = NLS.str("apkSignature.warnings");
			final String sigSuccKey = "apkSignature.signatureSuccess";
			final String sigFailKey = "apkSignature.signatureFailed";

			writeIssues(builder, err, result.getErrors());

			if (!result.getV1SchemeSigners().isEmpty()) {
				builder.append("<h2>");
				builder.escape(NLS.str(result.isVerifiedUsingV1Scheme() ? sigSuccKey : sigFailKey, 1));
				builder.append("</h2>\n");

				builder.append("<blockquote>");
				for (ApkVerifier.Result.V1SchemeSignerInfo signer : result.getV1SchemeSigners()) {
					builder.append("<h3>");
					builder.escape(NLS.str("apkSignature.signer"));
					builder.append(" ");
					builder.escape(signer.getName());
					builder.append(" (");
					builder.escape(signer.getSignatureFileName());
					builder.append(")");
					builder.append("</h3>");
					writeCertificate(builder, signer.getCertificate());
					writeIssues(builder, err, signer.getErrors());
					writeIssues(builder, warn, signer.getWarnings());
				}
				builder.append("</blockquote>");
			}
			if (!result.getV2SchemeSigners().isEmpty()) {
				builder.append("<h2>");
				builder.escape(NLS.str(result.isVerifiedUsingV2Scheme() ? sigSuccKey : sigFailKey, 2));
				builder.append("</h2>\n");

				builder.append("<blockquote>");
				for (ApkVerifier.Result.V2SchemeSignerInfo signer : result.getV2SchemeSigners()) {
					builder.append("<h3>");
					builder.escape(NLS.str("apkSignature.signer"));
					builder.append(" ");
					builder.append(Integer.toString(signer.getIndex() + 1));
					builder.append("</h3>");
					writeCertificate(builder, signer.getCertificate());
					writeIssues(builder, err, signer.getErrors());
					writeIssues(builder, warn, signer.getWarnings());
				}
				builder.append("</blockquote>");
			}
			if (!result.getV3SchemeSigners().isEmpty()) {
				builder.append("<h2>");
				builder.escape(NLS.str(result.isVerifiedUsingV3Scheme() ? sigSuccKey : sigFailKey, 3));
				builder.append("</h2>\n");

				builder.append("<blockquote>");
				for (ApkVerifier.Result.V3SchemeSignerInfo signer : result.getV3SchemeSigners()) {
					builder.append("<h3>");
					builder.escape(NLS.str("apkSignature.signer"));
					builder.append(" ");
					builder.append(Integer.toString(signer.getIndex() + 1));
					builder.append("</h3>");
					writeCertificate(builder, signer.getCertificate());
					writeIssues(builder, err, signer.getErrors());
					writeIssues(builder, warn, signer.getWarnings());
				}
				builder.append("</blockquote>");
			}
			writeIssues(builder, warn, result.getWarnings());

			this.content = new SimpleCodeInfo(builder.toString());
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			StringEscapeUtils.Builder builder = StringEscapeUtils.builder(StringEscapeUtils.ESCAPE_HTML4);
			builder.append("<h1>");
			builder.escape(NLS.str("apkSignature.exception"));
			builder.append("</h1><pre>");
			builder.escape(ExceptionUtils.getStackTrace(e));
			builder.append("</pre>");
			return new SimpleCodeInfo(builder.toString());
		}
		return this.content;
	}

	private void writeCertificate(StringEscapeUtils.Builder builder, Certificate cert) {
		CertificateManager certMgr = new CertificateManager(cert);
		builder.append("<blockquote><pre>");
		builder.escape(certMgr.generateHeader());
		builder.append("</pre><pre>");
		builder.escape(certMgr.generatePublicKey());
		builder.append("</pre><pre>");
		builder.escape(certMgr.generateSignature());
		builder.append("</pre><pre>");
		builder.append(certMgr.generateFingerprint());
		builder.append("</pre></blockquote>");
	}

	private void writeIssues(StringEscapeUtils.Builder builder, String issueType, List<ApkVerifier.IssueWithParams> issueList) {
		if (!issueList.isEmpty()) {
			builder.append("<h3>");
			builder.escape(issueType);
			builder.append("</h3>");
			builder.append("<blockquote>");
			// Unprotected Zip entry issues are very common, handle them separately
			List<ApkVerifier.IssueWithParams> unprotIssues = issueList.stream()
					.filter(i -> i.getIssue() == ApkVerifier.Issue.JAR_SIG_UNPROTECTED_ZIP_ENTRY)
					.collect(Collectors.toList());
			if (!unprotIssues.isEmpty()) {
				builder.append("<h4>");
				builder.escape(NLS.str("apkSignature.unprotectedEntry"));
				builder.append("</h4><blockquote>");
				for (ApkVerifier.IssueWithParams issue : unprotIssues) {
					builder.escape((String) issue.getParams()[0]);
					builder.append("<br>");
				}
				builder.append("</blockquote>");
			}
			List<ApkVerifier.IssueWithParams> remainingIssues = issueList.stream()
					.filter(i -> i.getIssue() != ApkVerifier.Issue.JAR_SIG_UNPROTECTED_ZIP_ENTRY)
					.collect(Collectors.toList());
			if (!remainingIssues.isEmpty()) {
				builder.append("<pre>\n");
				for (ApkVerifier.IssueWithParams issue : remainingIssues) {
					builder.escape(issue.toString());
					builder.append("\n");
				}
				builder.append("</pre>\n");
			}
			builder.append("</blockquote>");
		}
	}
}
