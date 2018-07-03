package jadx.gui.utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.security.cert.Certificate;
import java.util.Collection;


public class CertificateManagerTest  {
    private static final String DSA =  "CERT.DSA";
    private static final String RSA =  "CERT.RSA";
    private static final String EMPTY =  "EMPTY.txt";
    private String emptyPath;
    CertificateManager certificateManagerRSA;
    CertificateManager certificateManagerDSA;

    private CertificateManager getCertificateManger(String resName)
    {
        String sertPath = getClass().getClassLoader().getResource(resName).getPath();
        try (InputStream in = new FileInputStream(sertPath)) {
            Collection<? extends Certificate> certificates = CertificateManager.readCertificates(in);
            Certificate cert =  (Certificate)certificates.toArray()[0];
            return  new CertificateManager(cert);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Before
    public void setUp() {
        emptyPath = getClass().getClassLoader().getResource(EMPTY).getPath();
        certificateManagerRSA = getCertificateManger(RSA);
        certificateManagerDSA = getCertificateManger(DSA);
    }


    @Test
    public void decodeNotCertificateFile() {
        try (InputStream in = new FileInputStream(emptyPath)) {
            String result = CertificateManager.decode(in);
            Assert.assertEquals(result, "");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void decodeRSAKeyHeader() {
        String string = certificateManagerRSA.generateHeader();
        Assert.assertTrue(string.contains("X.509"));
        Assert.assertTrue(string.contains("0x4bd68052"));
        Assert.assertTrue(string.contains("CN=test cert, OU=test unit, O=OOO TestOrg, L=St.Peterburg, ST=Russia, C=123456"));

    }

    @Test
    public void decodeDSAKeyHeader() {
        String string = certificateManagerDSA.generateHeader();
        Assert.assertTrue(string.contains("X.509"));
        Assert.assertTrue(string.contains("0x16420ba2"));
        Assert.assertTrue(string.contains("O=\"UJMRFVV CN=EDCVBGT C=TG\""));

    }



}