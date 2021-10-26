package at.cosylab.fog.fog_trust_provider.utils.fti_agent;

import at.cosylab.fog.fog_trust_provider.repositories.configuration_attribute.ConfigurationAttribute;
import at.cosylab.fog.fog_trust_provider.repositories.configuration_attribute.ConfigurationAttributeRepository;
import at.cosylab.fog.fog_trust_provider.utils.FTPGlobals;
import at.cosylab.fog.fog_trust_provider.utils.amqp.FogAMQPClient;
import at.cosylab.fog.fog_trust_provider.utils.certificates.KeyStoreClient;
import at.cosylab.fog.fog_trust_provider.utils.cloud.CloudBackendHTTPClientService;
import fog.amqp_utils.routes.FTARoutesConstants;
import fog.error_handling.global_exceptions.InternalServerErrorException;
import fog.payloads.fta.CertificateValidationHolder;
import fog.payloads.fta.FogComponentCSR;
import identities.FogServiceType;
import identities.IdentityGenerator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import payloads.tnta.certificate.FogNodeCSRResponse;
import payloads.tnta.ticket.TicketSignature;
import pki.PublicKeyGenerator;
import pki.certificate.CSRHandler;
import pki.certificate.CertificateHelper;
import pki.certificate.CertificateKeyConverter;
import utils.AESUtil;
import utils.CloudConstants;
import utils.CryptoUtilFunctions;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;


@Configuration
@Slf4j
public class CryptoCredentialsManager {

    @Autowired
    private FogAMQPClient fogAmqpClient;

    @Autowired
    private ConfigurationAttributeRepository configAttrRepo;

    @Autowired
    private CloudBackendHTTPClientService cloudClient;

    @Value("${cosylab.ftp.keystore.filename}")
    private String keystoreFilename;

    @Value("${cosylab.ftp.keystore.password}")
    private String keystorePassword;

    @Value("${cosylab.ftp.certificate.tnta.filename}")
    private String tntaCertFilename;

    private KeyStoreClient keyStoreClient;

    private Provider bcProvider;

    @Getter
    private PrivateKey myPrivateKey;
    @Getter
    private X509Certificate myCert;

    private X509Certificate tntaCert;
    @Getter
    private String identity;

    @Getter
    private String ftaIdentity;

    @PostConstruct
    public void init() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException, OperatorCreationException, InternalServerErrorException, InvalidAlgorithmParameterException, InvalidKeyException, SignatureException, InvalidKeySpecException {

        this.bcProvider = CryptoUtilFunctions.initializeSecurityProvider();
        this.keyStoreClient = new KeyStoreClient(keystoreFilename, keystorePassword);

        FileReader keyReader = new FileReader(tntaCertFilename);
        this.tntaCert = CertificateKeyConverter.convertPEMToX509(keyReader);

        this.executeFTIProcedure();
        log.info("Exiting init");
    }

    private void executeFTIProcedure() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException, NoSuchProviderException, InvalidAlgorithmParameterException, SignatureException, InvalidKeyException, InternalServerErrorException, OperatorCreationException {

        int iterCounter = 1;
        this.myPrivateKey = keyStoreClient.loadMyPrivateKey();
        ConfigurationAttribute idAttr = configAttrRepo.findByName(FTPGlobals.FTP_IDENTITY_KEY);
        ConfigurationAttribute certAttr = configAttrRepo.findByName(FTPGlobals.FTP_CERTIFICATE_KEY);

        if ((this.myPrivateKey == null) || (certAttr == null) || (idAttr == null)) {
            this.myPrivateKey = null;
            idAttr = null;
            certAttr = null;
            configAttrRepo.deleteByName(FTPGlobals.FTP_IDENTITY_KEY);
            configAttrRepo.deleteByName(FTPGlobals.FTP_CERTIFICATE_KEY);
            while ((iterCounter < 10 && ((this.myPrivateKey == null) || (certAttr == null) || (idAttr == null)))) {
                try {
                    Long start = System.currentTimeMillis();
                    // generate public key
                    KeyPair keyPair = PublicKeyGenerator.generatePublicKeyPair();
                    keyStoreClient.storeMyPrivateKey(keyPair.getPrivate(), this.tntaCert);
                    log.info("[PERF] Generated pubkey in {} ms", (System.currentTimeMillis() - start));
                    start = System.currentTimeMillis();

                    //get cert from FTA

                    CertificateValidationHolder resp =
                            fogAmqpClient.sendAndReceiveAMQPRequest(FTARoutesConstants.FTA_QUEUE_NAME_CERTIFICATE_VALIDATE, null, CertificateValidationHolder.class);
                    log.info("[PERF] Got FTA cert in {} ms", (System.currentTimeMillis() - start));
                    start = System.currentTimeMillis();

                    ftaIdentity = IdentityGenerator.getFTAIdFromFogServiceIdentifier(
                            CertificateKeyConverter.convertPEMToX509(
                                    resp.getCertificate()).getSubjectDN().getName());


                    // generate identity
                    this.identity = IdentityGenerator.generateFogServiceIdentifier(ftaIdentity, FogServiceType.FTP);
                    log.info("[PERF] generated identity in {} ms", (System.currentTimeMillis() - start));
                    start = System.currentTimeMillis();
                    // validate certificates signature using tnta certificate
                    CertificateHelper.verifyCertificateSignature(tntaCert.getPublicKey(), CertificateKeyConverter.convertPEMToX509(resp.getCertificate()));
                    log.info("[PERF] Validated FTA cert signature in {} ms", (System.currentTimeMillis() - start));
                    start = System.currentTimeMillis();
                    // check fta's certificate in cloud
                    cloudClient.validateFTATicket(new TicketSignature(resp.getTicketToken(), "", resp.getTicketIssuer(), CloudConstants.TNTA_ROUTE_VALIDATE_TICKET));

                    log.info("[PERF] Validated FTA cert revocation in {} ms", (System.currentTimeMillis() - start));
                    start = System.currentTimeMillis();

                    PKCS10CertificationRequest request = CSRHandler.generateCSR(keyPair.getPrivate(), keyPair.getPublic(), CryptoUtilFunctions.generateCertCommonName(identity));
                    String csrContent = CertificateKeyConverter.convertPKCS10CertificationRequestToPEM(request);
                    log.info("[PERF] Created CSR in {} ms", (System.currentTimeMillis() - start));
                    start = System.currentTimeMillis();

                    FogNodeCSRResponse respCert = fogAmqpClient.sendAndReceiveAMQPRequest(FTARoutesConstants.FTA_QUEUE_NAME_CSR,
                            new FogComponentCSR(identity, csrContent), FogNodeCSRResponse.class);
                    log.info("[PERF] Received cert in {} ms", (System.currentTimeMillis() - start));
                    start = System.currentTimeMillis();
                    this.myCert = CertificateKeyConverter.convertPEMToX509(respCert.getCertificateContent());

                    this.myPrivateKey = keyPair.getPrivate();

                    keyStoreClient.storeMyPrivateKey(this.myPrivateKey,
                            this.myCert);

                    log.info("[PERF] Received cert in {} ms", (System.currentTimeMillis() - start));
                    start = System.currentTimeMillis();

                    if (certAttr == null) {
                        certAttr = new ConfigurationAttribute(FTPGlobals.FTP_CERTIFICATE_KEY, respCert.getCertificateContent());
                    } else {
                        certAttr.setValue(respCert.getCertificateContent());
                    }
                    configAttrRepo.save(certAttr);
                    log.info("[PERF] Stored private key and cert in {} ms", (System.currentTimeMillis() - start));

                    if (idAttr == null) {
                        idAttr = new ConfigurationAttribute(FTPGlobals.FTP_IDENTITY_KEY, identity);
                    } else {
                        idAttr.setValue(identity);
                    }
                    configAttrRepo.save(idAttr);

                } catch (Exception e) {
                    log.warn("Failed crypto initialization, iteration " + iterCounter, e);
                    iterCounter++;
                }
            }
        } else {
            this.identity = idAttr.getValue();
            this.myCert = CertificateKeyConverter.convertPEMToX509(certAttr.getValue());

            CertificateValidationHolder resp =
                    fogAmqpClient.sendAndReceiveAMQPRequest(FTARoutesConstants.FTA_QUEUE_NAME_CERTIFICATE_VALIDATE, null, CertificateValidationHolder.class);

            this.ftaIdentity = IdentityGenerator.getFTAIdFromFogServiceIdentifier(
                    CertificateKeyConverter.convertPEMToX509(
                            resp.getCertificate()).getSubjectDN().getName());
        }

        if ((this.myPrivateKey == null) || (this.myCert == null) || (this.identity == null)) {
            throw new UnrecoverableKeyException("Crypto initilization failed!");
        } else {
            log.info("Fog Trust init successful");
        }
    }

    public X509Certificate signCertificate(PKCS10CertificationRequest csr) throws OperatorCreationException, CertificateException {
        return new CSRHandler(this.myPrivateKey).generateCertificateFromCSR(this.bcProvider, csr, this.identity, null,
                FTPGlobals.AMQP_PROTOCOL_PREFIX + FTPGlobals.FTP_QUEUE_NAME_OCSP,
                String.valueOf(this.myCert.getSerialNumber()));
    }
}
