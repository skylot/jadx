package jadx.gui.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CertificateManagerTest {
	private static final String CERTIFICATE_TEST_DIR = "certificate-test/";
	private static final String DSA = "CERT.DSA";
	private static final String RSA = "CERT.RSA";
	private static final String EMPTY = "EMPTY.txt";

	private String emptyPath;
	private CertificateManager certificateManagerRSA;
	private CertificateManager certificateManagerDSA;

	private CertificateManager getCertificateManger(String resName) {
		String certPath = getResourcePath(resName);
		try (InputStream in = new FileInputStream(certPath)) {
			Collection<? extends Certificate> certificates = CertificateManager.readCertificates(in);
			Certificate cert = certificates.iterator().next();
			return new CertificateManager(cert);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create CertificateManager");
		}
	}

	@BeforeEach
	public void setUp() {
		emptyPath = getResourcePath(EMPTY);
		certificateManagerRSA = getCertificateManger(RSA);
		certificateManagerDSA = getCertificateManger(DSA);
	}

	@Test
	public void decodeNotCertificateFile() throws IOException {
		try (InputStream in = new FileInputStream(emptyPath)) {
			String result = CertificateManager.decode(in);
			assertEquals("", result);
		}
	}

	@Test
	public void decodeRSAKeyHeader() {
		String string = certificateManagerRSA.generateHeader();
		assertTrue(string.contains("X.509"));
		assertTrue(string.contains("0x4bd68052"));
		assertTrue(string.contains("CN=test cert, OU=test unit, O=OOO TestOrg, L=St.Peterburg, ST=Russia, C=123456"));
	}

	@Test
	public void decodeDSAKeyHeader() {
		String string = certificateManagerDSA.generateHeader();
		assertTrue(string.contains("X.509"));
		assertTrue(string.contains("0x16420ba2"));
		assertTrue(string.contains("O=\"UJMRFVV CN=EDCVBGT C=TG\""));
	}

	@Test
	public void decodeRSAKeySignature() {
		String string = certificateManagerRSA.generateSignature();
		assertTrue(string.contains("SHA256withRSA"));
		assertTrue(string.contains("1.2.840.113549.1.1.11"));
	}

	@Test
	public void decodeDSAKeySignature() {
		String string = certificateManagerDSA.generateSignature();
		assertTrue(string.contains("SHA1withDSA"));
		assertTrue(string.contains("1.2.840.10040.4.3"));
	}

	@Test
	public void decodeRSAFingerprint() {
		String string = certificateManagerRSA.generateFingerprint();
		assertTrue(string.contains("61 18 0A 71 3F C9 55 16 4E 04 E3 C5 45 08 D9 11"));
		assertTrue(string.contains("A0 6E A6 06 DB 2C 6F 3A 16 56 7F 75 97 7B AE 85 C2 13 09 37"));
		assertTrue(string.contains("12 53 E8 BB C8 AA 27 A8 49 9B F8 0D 6E 68 CE 32 35 50 DE 55 A7 E7 8C 29 51 00 96 D7 56 F4 54 " +
				"44"));
	}

	@Test
	public void decodeDSAFingerprint() {
		String string = certificateManagerDSA.generateFingerprint();
		assertTrue(string.contains("D9 06 A6 2D 1F 79 8C 9D A6 EF 40 C7 2E C2 EA 0B"));
		assertTrue(string.contains("18 E9 9C D4 A1 40 8F 63 FA EC 2E 62 A0 F2 AE B7 3F C3 C2 04"));
		assertTrue(string.contains("74 F9 48 64 EE AC 92 26 53 2C 7A 0E 55 BE 5E D8 2F A7 D9 A9 99 F5 D5 21 2C 51 21 C4 31 AD 73 " +
				"40"));
	}

	@Test
	public void decodeRSAPubKey() {
		String string = certificateManagerRSA.generatePublicKey();
		assertTrue(string.contains("RSA"));
		assertTrue(string.contains("65537"));
		assertTrue(string.contains(
				"16819531290318044625546437357099080306019392752925688951114880688329201213180109168890384305768067101521914473763638669503560977521269328582980060332888147680193318231260043189411794465899645633586173494259691101582064441956032924396850221679489313043628562082670183392670094163371858684118480409374749790551473773845213427476236147328434427272177623018935282929152308753854314219987617604037468769472089902090243358285991739642170211970862773121939911777280101937073243006335384636193260583579409760790138329893534549366882523130765297472656435892831796545149793228897111760122091442123535919361963075454640516520743"));
	}

	@Test
	public void decodeDSAPubKey() {
		String string = certificateManagerDSA.generatePublicKey();
		assertTrue(string.contains("DSA"));
		assertTrue(string.contains(
				"19323367605058154682563301282345453222279312104889899001698209626254725581511375469963812461090495963838615773832867364330457010553974237985991904800958394169421485070378434746792379708805563793253282995274293621162504943287538455944652344378242226897507369146942411692220922477368782490423187845815262510366"));
	}

	private String getResourcePath(String resName) {
		URL resource = getClass().getClassLoader().getResource(CERTIFICATE_TEST_DIR + resName);
		if (resource == null) {
			throw new RuntimeException("Resource not found: " + resName);
		}
		return resource.getPath();
	}
}
