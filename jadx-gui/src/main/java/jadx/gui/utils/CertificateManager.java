package jadx.gui.utils;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CertificateManager {
	private static final Logger LOG = LoggerFactory.getLogger(CertificateManager.class);
	private static final String CERTIFICATE_TYPE_NAME = "X.509";

	private final Certificate cert;
	private X509Certificate x509cert;

	public static String decode(InputStream in) {
		StringBuilder strBuild = new StringBuilder();
		Collection<? extends Certificate> certificates = readCertificates(in);
		if (certificates != null) {
			for (Certificate cert : certificates) {
				CertificateManager certificateManager = new CertificateManager(cert);
				strBuild.append(certificateManager.generateText());
			}
		}
		return strBuild.toString();
	}

	static Collection<? extends Certificate> readCertificates(InputStream in) {
		try {
			CertificateFactory cf = CertificateFactory.getInstance(CERTIFICATE_TYPE_NAME);
			return cf.generateCertificates(in);
		} catch (Exception e) {
			LOG.error("Certificate read error", e);
		}
		return Collections.emptyList();
	}

	public CertificateManager(Certificate cert) {
		this.cert = cert;
		String type = cert.getType();
		if (type.equals(CERTIFICATE_TYPE_NAME) && cert instanceof X509Certificate) {
			x509cert = (X509Certificate) cert;
		}
	}

	public String generateHeader() {
		StringBuilder builder = new StringBuilder();
		append(builder, NLS.str("certificate.cert_type"), x509cert.getType());
		append(builder, NLS.str("certificate.serialSigVer"), ((Integer) x509cert.getVersion()).toString());
		// serial number
		append(builder, NLS.str("certificate.serialNumber"), "0x" + x509cert.getSerialNumber().toString(16));

		// Get subject
		Principal subjectDN = x509cert.getSubjectDN();
		append(builder, NLS.str("certificate.cert_subject"), subjectDN.getName());

		append(builder, NLS.str("certificate.serialValidFrom"), x509cert.getNotBefore().toString());
		append(builder, NLS.str("certificate.serialValidUntil"), x509cert.getNotAfter().toString());
		return builder.toString();
	}

	public String generateSignature() {
		StringBuilder builder = new StringBuilder();
		append(builder, NLS.str("certificate.serialSigType"), x509cert.getSigAlgName());
		append(builder, NLS.str("certificate.serialSigOID"), x509cert.getSigAlgOID());
		return builder.toString();
	}

	public String generateFingerprint() {
		StringBuilder builder = new StringBuilder();
		try {
			append(builder, NLS.str("certificate.serialMD5"), getThumbPrint(x509cert, "MD5"));
			append(builder, NLS.str("certificate.serialSHA1"), getThumbPrint(x509cert, "SHA-1"));
			append(builder, NLS.str("certificate.serialSHA256"), getThumbPrint(x509cert, "SHA-256"));
		} catch (Exception e) {
			LOG.error("Failed to parse fingerprint", e);
		}
		return builder.toString();
	}

	public String generatePublicKey() {
		PublicKey publicKey = x509cert.getPublicKey();
		if (publicKey instanceof RSAPublicKey) {
			return generateRSAPublicKey();
		}
		if (publicKey instanceof DSAPublicKey) {
			return generateDSAPublicKey();
		}
		return "";
	}

	String generateRSAPublicKey() {
		RSAPublicKey pub = (RSAPublicKey) cert.getPublicKey();
		StringBuilder builder = new StringBuilder();

		append(builder, NLS.str("certificate.serialPubKeyType"), pub.getAlgorithm());
		append(builder, NLS.str("certificate.serialPubKeyExponent"), pub.getPublicExponent().toString(10));
		append(builder, NLS.str("certificate.serialPubKeyModulusSize"), Integer.toString(
				pub.getModulus().toString(2).length()));
		append(builder, NLS.str("certificate.serialPubKeyModulus"), pub.getModulus().toString(10));

		return builder.toString();
	}

	String generateDSAPublicKey() {
		DSAPublicKey pub = (DSAPublicKey) cert.getPublicKey();
		StringBuilder builder = new StringBuilder();
		append(builder, NLS.str("certificate.serialPubKeyType"), pub.getAlgorithm());
		append(builder, NLS.str("certificate.serialPubKeyY"), pub.getY().toString(10));

		return builder.toString();
	}

	public String generateTextForX509() {
		StringBuilder builder = new StringBuilder();
		if (x509cert != null) {
			builder.append(generateHeader());
			builder.append('\n');

			builder.append(generatePublicKey());
			builder.append('\n');

			builder.append(generateSignature());
			builder.append('\n');
			builder.append(generateFingerprint());
		}
		return builder.toString();
	}

	public String generateText() {
		StringBuilder str = new StringBuilder();
		String type = cert.getType();
		if (type.equals(CERTIFICATE_TYPE_NAME)) {
			str.append(generateTextForX509());
		} else {
			str.append(cert.toString());
		}
		return str.toString();
	}

	static void append(StringBuilder str, String name, String value) {
		str.append(name).append(": ").append(value).append('\n');
	}

	public static String getThumbPrint(X509Certificate cert, String type)
			throws NoSuchAlgorithmException, CertificateEncodingException {
		MessageDigest md = MessageDigest.getInstance(type); // lgtm [java/weak-cryptographic-algorithm]
		byte[] der = cert.getEncoded();
		md.update(der);
		byte[] digest = md.digest();
		return hexify(digest);
	}

	public static String hexify(byte[] bytes) {
		char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
		StringBuilder buf = new StringBuilder(bytes.length * 3);
		for (byte aByte : bytes) {
			buf.append(hexDigits[(aByte & 0xf0) >> 4]);
			buf.append(hexDigits[aByte & 0x0f]);
			buf.append(' ');
		}
		return buf.toString();
	}
}
