package at.cosylab.fog.fog_trust_anchor.utils.fti_agent;

import at.cosylab.fog.fog_trust_anchor.repositories.configuration_attribute.ConfigurationAttribute;
import at.cosylab.fog.fog_trust_anchor.repositories.configuration_attribute.ConfigurationAttributeRepository;
import at.cosylab.fog.fog_trust_anchor.utils.FTAGlobals;
import at.cosylab.fog.fog_trust_anchor.utils.certificates.KeyStoreClient;
import at.cosylab.fog.fog_trust_anchor.utils.cloud.CloudBackendHTTPClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import fog.amqp_utils.routes.FTARoutesConstants;
import fog.error_handling.global_exceptions.InternalServerErrorException;
import identities.FogServiceType;
import identities.IdentityGenerator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.ResourceAccessException;
import payloads.tnta.certificate.FogNodeCSR;
import payloads.tnta.certificate.FogNodeCSRResponse;
import payloads.tnta.certificate.RegisterFogNodeRequest;
import payloads.tnta.certificate.RegisterFogNodeResponse;
import pki.PublicKeyGenerator;
import pki.certificate.CSRHandler;
import pki.certificate.CertificateCreationData;
import pki.certificate.CertificateHelper;
import pki.certificate.CertificateKeyConverter;
import utils.AESUtil;
import utils.CryptoUtilFunctions;
import utils.ECUtils;

import javax.annotation.PostConstruct;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.FileReader;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

import static at.cosylab.fog.fog_trust_anchor.utils.FTAGlobals.FTA_CERTIFICATE_KEY;
import static at.cosylab.fog.fog_trust_anchor.utils.FTAGlobals.FTA_IDENTITY_KEY;


@Configuration
@Slf4j
public class CryptoCredentialsManager {

    @Value("${cosylab.fta.secrets.application-secret}")
    private String appSecret;

    @Value("${cosylab.fta.secrets.instance-secret}")
    private String instanceSecret;

    @Value("${cosylab.fta.secrets.user-secret}")
    private String userSecret;

    @Value("${cosylab.fta.keystore.filename}")
    private String keystoreFilename;

    @Value("${cosylab.fta.keystore.password}")
    private String keystorePassword;

    @Value("${cosylab.fta.certificate.tnta.filename}")
    private String tntaCertFilename;

    @Autowired
    private ConfigurationAttributeRepository configAttrRepo;

    @Autowired
    private CloudBackendHTTPClientService cloudClient;

    private KeyStoreClient keyStoreClient;

    private Provider bcProvider;

    @Getter
    private PrivateKey myPrivateKey;
    @Getter
    private X509Certificate myCert;
    @Getter
    private X509Certificate tntaCert;
    @Getter
    private String myIdentity;

    @PostConstruct
    public void init() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException, UnrecoverableKeyException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeySpecException {

        this.bcProvider = CryptoUtilFunctions.initializeSecurityProvider();
        this.keyStoreClient = new KeyStoreClient(keystoreFilename, keystorePassword);

        FileReader keyReader = new FileReader(tntaCertFilename);
        this.tntaCert = CertificateKeyConverter.convertPEMToX509(keyReader);

        this.executeFTIProcedure();

    }

    public X509Certificate signCertificate(PKCS10CertificationRequest csr) throws OperatorCreationException, CertificateException {
        return new CSRHandler(this.myPrivateKey).generateCertificateFromCSR(this.bcProvider, csr, this.myIdentity, null,
                "amqp://" + FTARoutesConstants.FTA_QUEUE_NAME_OCSP,
                String.valueOf(this.myCert.getSerialNumber()));
    }


    private void executeFTIProcedure() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException {

        this.myPrivateKey = keyStoreClient.loadMyPrivateKey();
        ConfigurationAttribute idAttr = configAttrRepo.findByName(FTA_IDENTITY_KEY);
        ConfigurationAttribute configCert = configAttrRepo.findByName(FTAGlobals.FTA_CERTIFICATE_KEY);

        if (configCert != null) {
            this.myCert = CertificateKeyConverter.convertPEMToX509(configCert.getValue());
        }

        if (idAttr != null) {
            this.myIdentity = idAttr.getValue();
        }

        ConfigurationAttribute configAttr = configAttrRepo.findByName(FTAGlobals.FTI_CLOUD_FLAG_DB_KEY);
        if (configAttr == null) {
            configAttr = new ConfigurationAttribute(FTAGlobals.FTI_CLOUD_FLAG_DB_KEY, Boolean.toString(false));
        }

        log.info("Private key {}", this.myPrivateKey);
        log.info("Cert {}", this.myCert);
        log.info("ID {}", this.myIdentity);
        if ((this.myPrivateKey == null) || (this.myCert == null) || (this.myIdentity == null)) {

            // generate identity
            Long start = System.currentTimeMillis();
            String ftaId = CryptoUtilFunctions.generateUUID();
            this.myIdentity = IdentityGenerator.generateFogServiceIdentifier(ftaId, FogServiceType.FTA);
            log.info("[PERF] Generated identity in {} ms", (System.currentTimeMillis() - start));
            start = System.currentTimeMillis();
            // generate public key
            KeyPair keyPair = PublicKeyGenerator.generatePublicKeyPair();
            this.myPrivateKey = keyPair.getPrivate();

            keyStoreClient.storeMyPrivateKey(this.myPrivateKey, this.tntaCert);
            log.info("[PERF] Generated and stored pubkey in {} ms", (System.currentTimeMillis() - start));
            start = System.currentTimeMillis();

            this.myCert = this.registerCertificateInCloud(this.myIdentity, keyPair.getPrivate(), keyPair.getPublic());

            log.info("[PERF] Get cert from cloud in {} ms", (System.currentTimeMillis() - start));
            start = System.currentTimeMillis();

            if (this.myCert == null) {
                start = System.currentTimeMillis();
                CertificateCreationData certData = new CertificateCreationData(this.myIdentity, this.myIdentity, keyPair.getPublic());

                Certificate ftaSSCert = CertificateHelper.generateCertificate(bcProvider, keyPair.getPrivate(), certData);
                this.myCert = CertificateKeyConverter.convertCertToX509(ftaSSCert);
                configAttr.setValue(Boolean.toString(false));
                log.info("[PERF] Generated and stored SS cert in {} ms", (System.currentTimeMillis() - start));

            } else {
                configAttr.setValue(Boolean.toString(true));
            }

            if (configCert == null) {
                configCert = new ConfigurationAttribute(FTA_CERTIFICATE_KEY, CertificateKeyConverter.convertX509ToPEM(this.myCert));
            } else {
                configCert.setValue(CertificateKeyConverter.convertX509ToPEM(this.myCert));
            }
            configAttrRepo.save(configCert);
            log.info("[PERF] Generated and stored TNTA cert in {} ms", (System.currentTimeMillis() - start));

            if (idAttr == null) {
                idAttr = new ConfigurationAttribute(FTA_IDENTITY_KEY, this.myIdentity);
            } else {
                idAttr.setValue(this.myIdentity);
            }
            configAttrRepo.save(idAttr);

            configAttrRepo.save(configAttr);

            log.info("FTI was successful, keys generated");
        }
        if (configAttr.getValue().equals(Boolean.toString(false))) {
            log.info("FTI with cloud required");
            X509Certificate certFromTNTA = registerCertificateInCloud(idAttr.getValue(), this.myPrivateKey, this.myCert.getPublicKey());
            configAttr.setValue(Boolean.toString(true));
            configAttrRepo.save(configAttr);

            if (certFromTNTA != null) {
                this.myCert = certFromTNTA;
                if (configCert == null) {
                    configCert = new ConfigurationAttribute(FTA_CERTIFICATE_KEY, CertificateKeyConverter.convertX509ToPEM(this.myCert));
                } else {
                    configCert.setValue(CertificateKeyConverter.convertX509ToPEM(this.myCert));
                }
                configAttrRepo.save(configCert);
            }
        }
    }

    private X509Certificate registerCertificateInCloud(String identity, PrivateKey privateKey, PublicKey publicKey) {

        try {
            Long start = System.currentTimeMillis();

            ObjectMapper objectMapper = new ObjectMapper();

            // get secrets from image
            String hashFogSecret = DigestUtils.sha256Hex(appSecret + instanceSecret + userSecret);
            String hashFogCredentials = DigestUtils.sha256Hex(appSecret + instanceSecret);

            RegisterFogNodeRequest registerRequest = new RegisterFogNodeRequest(hashFogCredentials, hashFogSecret, identity);

            String cipherRequest = ECUtils.encryptText(objectMapper.writeValueAsString(registerRequest), this.tntaCert.getPublicKey());
            log.info("[PERF][CLOUD] 1. Generated hashes and registration req in {} ms ", (System.currentTimeMillis() - start));
            start = System.currentTimeMillis();
            // Send register FTA
            RegisterFogNodeResponse registerResponse = cloudClient.registerFTACredentials(cipherRequest);
            log.info("[PERF][CLOUD] 2. Received cloud response in {} ms ", (System.currentTimeMillis() - start));
            start = System.currentTimeMillis();

            // decrypt CSR token
            IvParameterSpec ivspec = new IvParameterSpec(registerResponse.getSalt().getBytes(StandardCharsets.UTF_8));

            SecretKey secretKey = AESUtil.generateAESKey(hashFogSecret, registerResponse.getSalt());

            PKCS10CertificationRequest request = CSRHandler.generateCSR(privateKey, publicKey, CryptoUtilFunctions.generateCertCommonName(identity));
            String csrContent = CertificateKeyConverter.convertPKCS10CertificationRequestToPEM(request);
            String csrKey = AESUtil.decryptString(registerResponse.getCsrKey(), secretKey, ivspec);

            FogNodeCSR csr = new FogNodeCSR(csrContent, csrKey);
            log.info("[PERF][CLOUD] 3. Created CSR in {} ms ", (System.currentTimeMillis() - start));
            start = System.currentTimeMillis();

            // send CSR to cloud
            FogNodeCSRResponse csrResponse = cloudClient.requestCertificateFromTNTA(csr);
            log.info("[PERF][CLOUD] 4. Obtained cert in {} ms ", (System.currentTimeMillis() - start));

            // convert Certificate
            return CertificateKeyConverter.convertPEMToX509(csrResponse.getCertificateContent());
        } catch (ConnectException | ResourceAccessException e) {
            e.printStackTrace();
            return null;
        } catch (BadPaddingException | NoSuchAlgorithmException | CertificateException | InvalidKeyException | IOException | IllegalBlockSizeException | NoSuchPaddingException | InvalidKeySpecException | InvalidAlgorithmParameterException | OperatorCreationException | InternalServerErrorException e) {
            e.printStackTrace();
            return null;
        }
    }
}
