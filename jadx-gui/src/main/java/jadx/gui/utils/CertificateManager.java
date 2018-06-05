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

	/*
	private String info;

	CertificateManager(LinkedList<File> allFiles) {
		info = findCertificates(allFiles);
	}
	public String getInfo()
	{
		return info;
	}

	public static String findCertificates(final LinkedList<File> allFiles) {
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

*/


	static public String decode(InputStream in){
		StringBuilder strBuild = new StringBuilder();
		Collection<? extends Certificate> certificates =readCertificates(in);
		for(Certificate cert:certificates){
			strBuild.append(generateText(cert));
		}
		return strBuild.toString();
	}
	static private Collection<? extends Certificate> readCertificates(InputStream in) {
		CertificateFactory cf;
		try {
			cf = CertificateFactory.getInstance("X.509");
			Collection<? extends Certificate> certs = cf.generateCertificates(in);
			in.close();
			return certs;
		} catch (Exception e) {

			e.printStackTrace();
		}
		return null;
	}


	static String generateText(Certificate cert) {
		StringBuilder str = new StringBuilder();
		String type = cert.getType();
		append(str, NLS.str("certificate.certType"), type);
		if (!type.equals("X.509")) {
			str.append(cert.toString());

		} else {


			if (cert instanceof X509Certificate) {
				X509Certificate x509cert = (X509Certificate) cert;

				// Get subject
				Principal principal = x509cert.getSubjectDN();
				append(str, NLS.str("certificate.сertSubject"), principal.getName());

				// Get issuer
				principal = x509cert.getIssuerDN();
				append(str, NLS.str("certificate.сertIssuer"), principal.getName());

				// seral number
				append(str, NLS.str("certificate.serialNumber"), "0x"
						+ x509cert.getSerialNumber().toString(16));

				append(str, NLS.str("certificate.serialValidFrom"), x509cert.getNotBefore()
						.toString());
				append(str, NLS.str("certificate.serialValidUntil"), x509cert.getNotAfter()
						.toString());
				str.append("\n");

				append(str, NLS.str("certificate.serialSignature"), "");
				append(str, NLS.str("certificate.serialSigVer"),
						((Integer) x509cert.getVersion()).toString());
				append(str, NLS.str("certificate.serialAlgName"), x509cert.getSigAlgName());
				append(str, NLS.str("certificate.serialSigOID"), x509cert.getSigAlgOID());
				// Fingerprint:
				try {
					append(str, NLS.str("certificate.sSerialMD5"), getThumbPrint(x509cert, "MD5"));
					append(str, NLS.str("certificate.serialSHA1"), getThumbPrint(x509cert, "SHA-1"));
					append(str, NLS.str("certificate.serialSHA256"), getThumbPrint(x509cert, "SHA-256"));
				} catch (CertificateEncodingException
						| NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				str.append("\n");

				RSAPublicKey pub = (RSAPublicKey) cert.getPublicKey();

				append(str, NLS.str("certificate.serialPubKey"), "");
				append(str, NLS.str("certificate.serialAlgName"), pub.getAlgorithm());
				append(str, NLS.str("certificate.serialPubKeyExponent"), pub.getPublicExponent().toString(16));
				append(str, NLS.str("certificate.serialPubKeyModulus"), pub.getModulus().toString(10));

			}

		}
		return str.toString();
	}



	static void append(StringBuilder str, String name, String value) {
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

		char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'A', 'B', 'C', 'D', 'E', 'F'};

		StringBuffer buf = new StringBuffer(bytes.length * 3);

		for (int i = 0; i < bytes.length; ++i) {
			buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
			buf.append(hexDigits[bytes[i] & 0x0f]);
			buf.append(' ');
		}

		return buf.toString();
	}

}
