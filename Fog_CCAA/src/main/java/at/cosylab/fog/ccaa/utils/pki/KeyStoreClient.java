package at.cosylab.fog.ccaa.utils.pki;

import pki.keystore.KeyStoreManager;

import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class KeyStoreClient {

    private final String tntaCertificateAlias = "ccaaCertificate";
    private final String tntaPrivateKeyAlias = "ccaaPrivateKey";

    private KeyStoreManager keyStoreManager;

    public KeyStoreClient(String keystoreFilename, String keystorePassword) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException, UnrecoverableKeyException {
       this.keyStoreManager = new KeyStoreManager(keystoreFilename, keystorePassword);
    }

    public PrivateKey loadMyPrivateKey() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        return keyStoreManager.getPrivateKey(this.tntaPrivateKeyAlias);
    }

    public X509Certificate loadMyCertificate() throws KeyStoreException, CertificateException {
        return keyStoreManager.getCertificate(this.tntaCertificateAlias);
    }

    public void storeMyPrivateKey(PrivateKey privateKey, X509Certificate tntaX509) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        keyStoreManager.storePrivateKey(tntaPrivateKeyAlias, privateKey, new Certificate[]{tntaX509});
    }
}
