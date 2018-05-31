package jadx.gui.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

public class CertificateManager {

	private String info;

	CertificateManager(LinkedList<File> allFiles) {
		info = findCertificates(allFiles);
	}
	public String getInfo()
	{
		return info;
	}

	static String findCertificates(final LinkedList<File> allFiles) {
		ArrayList<Certificate> certs = new ArrayList<>(2);
		for (File file : allFiles) {
			String path = file.getAbsolutePath();
			path = path.toUpperCase();
			if (path.contains("META-INF")
					&& (path.endsWith(".RSA") || path.endsWith(".DSA"))) {
				certs.addAll(readCertificates(file));
			}
		}
		return generateText(certs);
	}

	static private Collection<? extends Certificate> readCertificates(File f) {
		CertificateFactory cf;
		try {
			cf = CertificateFactory.getInstance("X.509");
			InputStream in = new FileInputStream(f);
			Collection<? extends Certificate> certs = cf.generateCertificates(in);
			in.close();
			return certs;
		} catch (Exception e) {

			e.printStackTrace();
		}
		return null;
	}

	static String generateText(ArrayList<Certificate> certificates) {

		StringBuffer str = new StringBuffer();
		for (Certificate cert : certificates) {

			String type = cert.getType();
			append(str,NLS.str("CertType"), type);
			if (!type.equals("X.509")) {
				str.append(cert.toString());

			} else {
				// ��� ���� "X.509" ������� ������

				if (cert instanceof X509Certificate) {
					X509Certificate x509cert = (X509Certificate) cert;

					// Get subject
					Principal principal = x509cert.getSubjectDN();
					append(str, NLS.str("CertSubject"), principal.getName());

					// Get issuer
					principal = x509cert.getIssuerDN();
					append(str, NLS.str("CertIssuer"), principal.getName());

					// seral number
					append(str, NLS.str("SerialNumber"), "0x"
							+ x509cert.getSerialNumber().toString(16));

					append(str, NLS.str("SerialValidFrom"), x509cert.getNotBefore()
							.toString());
					append(str, NLS.str("SerialValidUntil"), x509cert.getNotAfter()
							.toString());
					str.append("\n");

					append(str, NLS.str("SerialSignature"), "");
					append(str, NLS.str("SerialSigVer"),
							((Integer) x509cert.getVersion()).toString());
					append(str, NLS.str("SerialAlgName"), x509cert.getSigAlgName());
					append(str, NLS.str("SerialSigOID"), x509cert.getSigAlgOID());
					// Fingerprint:
					try {
						append(str, NLS.str("SerialMD5"),getThumbPrint(x509cert, "MD5"));
						append(str, NLS.str("SerialSHA1"),getThumbPrint(x509cert, "SHA-1"));
						append(str, NLS.str("SerialSHA256"),getThumbPrint(x509cert, "SHA-256"));
					} catch (CertificateEncodingException
							| NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					str.append("\n");

					RSAPublicKey pub = (RSAPublicKey) cert.getPublicKey();

					append(str, NLS.str("SerialPubKey"), "");
					append(str, NLS.str("SerialAlgName"), pub.getAlgorithm());
					append(str, NLS.str("SerialPubKeyExponent"), pub.getPublicExponent().toString(16));
					append(str, NLS.str("SerialPubKeyModulus"), pub.getModulus().toString(10));

				}

			}
		}

		return str.toString();
	}

	static void append(StringBuffer str, String name, String value) {
		str.append(name + ": " + value + "\n");
	}

	public static String getThumbPrint(X509Certificate cert, String type)
			throws NoSuchAlgorithmException, CertificateEncodingException {
		MessageDigest md = MessageDigest.getInstance(type);
		byte[] der = cert.getEncoded();
		md.update(der);
		byte[] digest = md.digest();
		return hexify(digest);

	}

	public static String hexify(byte bytes[]) {

		char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'A', 'B', 'C', 'D', 'E', 'F' };

		StringBuffer buf = new StringBuffer(bytes.length * 3);

		for (int i = 0; i < bytes.length; ++i) {
			buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
			buf.append(hexDigits[bytes[i] & 0x0f]);
			buf.append(' ');
		}

		return buf.toString();
	}

}
