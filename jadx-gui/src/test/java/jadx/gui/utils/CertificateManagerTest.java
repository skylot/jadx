package jadx.gui.utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.security.cert.Certificate;

import static org.junit.Assert.assertFalse;


public class CertificateManagerTest  {
    private static final String DSA =  "CERT.DSA";
    private static final String RSA =  "CERT.RSA";
    private static final String EMPTY =  "EMPTY.txt";
    private String sertDSAPath;
    private String sertRSAPath;
    private String emptyPath;
    @Before
    public void setUp() {
        sertDSAPath = getClass().getClassLoader().getResource(DSA).getPath();
        sertRSAPath = getClass().getClassLoader().getResource(RSA).getPath();
        emptyPath = getClass().getClassLoader().getResource(EMPTY).getPath();
    }


    @Test
    public void decodeNotCertificateFile() {
        try( InputStream in = new FileInputStream(emptyPath)){
           String result =  CertificateManager.decode(in);
            Assert.assertEquals(result,"");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

   /*
    @Test
    public void testDSA() {
        try  {
            InputStream inputStream = new FileInputStream(sertDSA);
            String strResult = CertificateManager.decode(inputStream);
            InputStream inputStream = new FileInputStream(sertDSA);



        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
    */


}