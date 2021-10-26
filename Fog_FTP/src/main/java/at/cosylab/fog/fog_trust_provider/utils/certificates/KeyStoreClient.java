package at.cosylab.fog.fog_trust_provider.utils.certificates;

import pki.keystore.KeyStoreManager;

import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class KeyStoreClient {

    private final String tntaCertificateAlias = "ftpCertificate";
    private final String tntaPrivateKeyAlias = "ftpPrivateKey";

    private KeyStoreManager keyStoreManager;

    public KeyStoreClient(String keystoreFilename, String keystorePassword) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException {
        this.keyStoreManager = new KeyStoreManager(keystoreFilename, keystorePassword);
    }

    public PrivateKey loadMyPrivateKey() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        return keyStoreManager.getPrivateKey(this.tntaPrivateKeyAlias);
    }

    public void storeMyPrivateKey(PrivateKey privateKey, X509Certificate tntaX509) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        keyStoreManager.storePrivateKey(tntaPrivateKeyAlias, privateKey, new Certificate[]{tntaX509});
    }

}
