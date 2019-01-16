package jadx.gui.treemodel;

import com.android.apksig.ApkVerifier;
import com.android.apksig.apk.ApkFormatException;
import jadx.gui.utils.CertificateManager;
import jadx.gui.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.security.cert.Certificate;
import java.util.List;

public class ApkSignature extends JNode {

	private static final Logger log = LoggerFactory.getLogger(ApkSignature.class);
	private static final ImageIcon CERTIFICATE_ICON = Utils.openIcon("certificate_obj");

	private final transient File openFile;
	private String content = null;

	public static ApkSignature getApkSignature(File openFile) {
		return new ApkSignature(openFile);
	}

	public ApkSignature(File openFile) {
		super();
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
	public String getContent() {
		if (content != null)
			return this.content;
		ApkVerifier verifier = new ApkVerifier.Builder(openFile).build();
		try {
			ApkVerifier.Result result = verifier.verify();
			StringBuilder str = new StringBuilder();
			str.append("<h1>APK signature verification result:</h1>");

			str.append("<p><b>");
			if (result.isVerified()) {
				str.append("Signature verification succeeded");
			} else {
				str.append("Signature verification failed\n");
			}
			str.append("</b></p>");

			writeIssues(str, "Errors", result.getErrors());
			writeIssues(str, "Warnings", result.getWarnings());

			if (result.isVerifiedUsingV1Scheme()) {
				str.append("<h2>APK signature v1 found</h2>\n");

				str.append("<blockquote>");
				for (ApkVerifier.Result.V1SchemeSignerInfo signer : result.getV1SchemeSigners()) {
					str.append("<h3>Signer " + signer.getName() + " (" + signer.getSignatureFileName() + ")</h3>");
					writeCertificate(str, signer.getCertificate());
					writeIssues(str, "Errors", signer.getErrors());
					writeIssues(str, "Warnings", signer.getWarnings());
				}
				str.append("</blockquote>");
			}
			if (result.isVerifiedUsingV2Scheme()) {
				str.append("<h2>APK signature v2 found</h2>\n");

				str.append("<blockquote>");
				for (ApkVerifier.Result.V2SchemeSignerInfo signer : result.getV2SchemeSigners()) {
					str.append("<h3>Signer " + (signer.getIndex() + 1) + "</h3>");
					writeCertificate(str, signer.getCertificate());
					writeIssues(str, "Errors", signer.getErrors());
					writeIssues(str, "Warnings", signer.getWarnings());
				}
				str.append("</blockquote>");
			}
			this.content = str.toString();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			this.content = String.format("<h1>APK verification failed: %s</h1><h2>Check log for details</h2>", e);
		}
		return this.content;
	}

	private void writeCertificate(StringBuilder str, Certificate cert) {
		CertificateManager certMgr = new CertificateManager(cert);
		str.append("<blockquote>");
		str.append("<pre>" + certMgr.generateHeader() + "</pre>");
		str.append("<pre>" + certMgr.generatePublicKey() + "</pre>");
		str.append("<pre>" + certMgr.generateSignature() + "</pre>");
		str.append("<pre>" + certMgr.generateFingerprint() + "</pre>");
		str.append("</blockquote>");
	}

	private void writeIssues(StringBuilder str, String issueType, List<ApkVerifier.IssueWithParams> issues) {
		if (issues.size() > 0) {
			str.append("<h3>" + issueType + "</h3><pre>");
			for (ApkVerifier.IssueWithParams e : issues) {
				str.append(e.toString() + "\n");
			}
			str.append("</pre>\n");
		}

	}


}
