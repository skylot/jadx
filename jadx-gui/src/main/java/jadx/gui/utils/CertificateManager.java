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

public class CertificateManager {

	static public String decode(InputStream in){
		StringBuilder strBuild = new StringBuilder();
		Collection<? extends Certificate> certificates = readCertificates(in);
		if(certificates!=null) {
			for (Certificate cert : certificates) {
				CertificateManager certificateManager= new CertificateManager(cert);
				strBuild.append(certificateManager.generateText());
			}
		}
		return strBuild.toString();
	}


	 static Collection<? extends Certificate> readCertificates(InputStream in) {
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


	private X509Certificate x509cert;
	private Certificate cert;

	public  CertificateManager(Certificate cert)
	{
		this.cert = cert;
		String type = cert.getType();
		if (type.equals("X.509")) {
			if (cert instanceof X509Certificate) {
				x509cert = (X509Certificate) cert;
			}
		}
	}


	String generateHeader()
	{
		StringBuilder builder = new StringBuilder();
		append(builder, NLS.str("certificate.cert_type"), x509cert.getType());
		append(builder, NLS.str("certificate.serialSigVer"),((Integer) x509cert.getVersion()).toString());
		// seral number
		append(builder, NLS.str("certificate.serialNumber"), "0x" + x509cert.getSerialNumber().toString(16));

		// Get subject
		Principal subjectDN = x509cert.getSubjectDN();
		append(builder, NLS.str("certificate.cert_subject"), subjectDN.getName());

		// Get issuer
//		Principal issuerDN = x509cert.getIssuerDN();
//		append(str, NLS.str("certificate.cert_issuer"), issuerDN.getName());

		append(builder, NLS.str("certificate.serialValidFrom"), x509cert.getNotBefore().toString());
		append(builder, NLS.str("certificate.serialValidUntil"), x509cert.getNotAfter().toString());
		return  builder.toString();

	}

	String generateSignature()
	{
		StringBuilder builder = new StringBuilder();
		append(builder, NLS.str("certificate.serialSigType"), x509cert.getSigAlgName());
		append(builder, NLS.str("certificate.serialSigOID"), x509cert.getSigAlgOID());
		return  builder.toString();
	}

	String generateFingerprint()
	{
		StringBuilder builder = new StringBuilder();
		try {
			append(builder, NLS.str("certificate.serialMD5"), getThumbPrint(x509cert, "MD5"));
			append(builder, NLS.str("certificate.serialSHA1"), getThumbPrint(x509cert, "SHA-1"));
			append(builder, NLS.str("certificate.serialSHA256"), getThumbPrint(x509cert, "SHA-256"));

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateEncodingException e) {
			e.printStackTrace();
		}
		return  builder.toString();
	}

	String generatePublicKey()
	{
		PublicKey publicKey = x509cert.getPublicKey();
		if(publicKey instanceof RSAPublicKey)
		{
			return generateRSAPublicKey();
		}
		if(publicKey instanceof DSAPublicKey)
		{
			return generateDSAPublicKey();
		}
		return "";
	}
	String generateRSAPublicKey()
	{
		RSAPublicKey pub = (RSAPublicKey) cert.getPublicKey();
		StringBuilder builder = new StringBuilder();

		append(builder, NLS.str("certificate.serialPubKeyType"), pub.getAlgorithm());
		append(builder, NLS.str("certificate.serialPubKeyExponent"), pub.getPublicExponent().toString(10));
		append(builder, NLS.str("certificate.serialPubKeyModulus"), pub.getModulus().toString(10));

		return builder.toString();
	}

	String generateDSAPublicKey()
	{
		DSAPublicKey pub = (DSAPublicKey) cert.getPublicKey();
		StringBuilder builder = new StringBuilder();
		append(builder, NLS.str("certificate.serialPubKeyType"), pub.getAlgorithm());
		append(builder, NLS.str("certificate.serialPubKeyY"), pub.getY().toString(10));

		return builder.toString();

	}

	String generateTextForX509()
	{
		StringBuilder builder = new StringBuilder();
		if(x509cert!=null){
			builder.append(generateHeader());
			builder.append("\n");

			builder.append(generatePublicKey());
			builder.append("\n");

			builder.append(generateSignature());
			builder.append("\n");
			builder.append(generateFingerprint());

		}

		return builder.toString();


	}



	 private String generateText() {
		StringBuilder str = new StringBuilder();
		String type = cert.getType();
		if (!type.equals("X.509")) {
			str.append(cert.toString());
		} else {
			str.append(generateTextForX509());
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

		StringBuilder buf = new StringBuilder(bytes.length * 3);

		for (int i = 0; i < bytes.length; ++i) {
			buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
			buf.append(hexDigits[bytes[i] & 0x0f]);
			buf.append(' ');
		}

		return buf.toString();
	}

}
