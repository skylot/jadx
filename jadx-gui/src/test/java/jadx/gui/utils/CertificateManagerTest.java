package jadx.gui.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
		assertTrue(string.contains("12 53 E8 BB C8 AA 27 A8 49 9B F8 0D 6E 68 CE 32 35 50 DE 55 A7 E7 8C 29 51 00 96 D7 56 F4 54 44"));
	}

	@Test
	public void decodeDSAFingerprint() {
		String string = certificateManagerDSA.generateFingerprint();
		assertTrue(string.contains("D9 06 A6 2D 1F 79 8C 9D A6 EF 40 C7 2E C2 EA 0B"));
		assertTrue(string.contains("18 E9 9C D4 A1 40 8F 63 FA EC 2E 62 A0 F2 AE B7 3F C3 C2 04"));
		assertTrue(string.contains("74 F9 48 64 EE AC 92 26 53 2C 7A 0E 55 BE 5E D8 2F A7 D9 A9 99 F5 D5 21 2C 51 21 C4 31 AD 73 40"));
	}

	@Test
	public void decodeRSAPubKey() {
		String string = certificateManagerRSA.generatePublicKey();
		assertTrue(string.contains("RSA"));
		assertTrue(string.contains("65537"));
		assertTrue(string.contains("1681953129031804462554643735709908030601939275292568895111488068832920121318010916"
				+ "889038430576806710152191447376363866950356097752126932858298006033288814768019331823126004318941179"
				+ "4465899645633586173494259691101582064441956032924396850221679489313043628562082670183392670094163371"
				+ "8586841184804093747497905514737738452134274762361473284344272721776230189352829291523087538543142199"
				+ "8761760403746876947208990209024335828599173964217021197086277312193991177728010193707324300633538463"
				+ "6193260583579409760790138329893534549366882523130765297472656435892831796545149793228897111760122091"
				+ "442123535919361963075454640516520743"));
	}

	@Test
	public void decodeDSAPubKey() {
		String string = certificateManagerDSA.generatePublicKey();
		assertTrue(string.contains("DSA"));
		assertTrue(string.contains("193233676050581546825633012823454532222793121048898990016982096262547255815113"
				+ "7546996381246109049596383861577383286736433045701055397423798599190480095839416942148507037843474"
				+ "67923797088055637932532829952742936211625049432875384559446523443782422268975073691469424116922209"
				+ "22477368782490423187845815262510366"));
	}

	private String getResourcePath(String resName) {
		URL resource = getClass().getClassLoader().getResource(CERTIFICATE_TEST_DIR + resName);
		if (resource == null) {
			throw new RuntimeException("Resource not found: " + resName);
		}
		return resource.getPath();
	}
}
