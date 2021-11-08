package at.cosylab.fog.faca.commons.fti_agent;

import at.cosylab.fog.faca.commons.FACAGlobals;
import at.cosylab.fog.faca.commons.cloud.CloudBackendHTTPClientService;
import at.cosylab.fog.faca.commons.fog_amqp.FogAMQPClient;
import at.cosylab.fog.faca.commons.pki.KeyStoreClient;
import at.cosylab.fog.faca.commons.repositories.configuration_attribute.ConfigurationAttribute;
import at.cosylab.fog.faca.commons.repositories.configuration_attribute.ConfigurationAttributeRepository;
import fog.amqp_utils.routes.FTARoutesConstants;
import fog.payloads.fta.CertificateValidationHolder;
import fog.payloads.fta.FogComponentCSR;
import identities.FogServiceType;
import identities.IdentityGenerator;
import jws.JWSTokenHandler;
import lombok.Getter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import payloads.tnta.certificate.FogNodeCSRResponse;
import payloads.tnta.ticket.TicketSignature;
import pki.PublicKeyGenerator;
import pki.certificate.CSRHandler;
import pki.certificate.CertificateHelper;
import pki.certificate.CertificateKeyConverter;
import utils.CloudConstants;
import utils.CryptoUtilFunctions;

import javax.annotation.PostConstruct;
import java.io.FileReader;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Configuration
public class CryptoInstallationHandler {

    private static final Logger logger = LoggerFactory.getLogger(CryptoInstallationHandler.class);

    @Autowired
    private FogAMQPClient fogAmqpClient;

    @Autowired
    private ConfigurationAttributeRepository configAttrRepo;

    @Autowired
    private CloudBackendHTTPClientService cloudClient;

    @Value("${cosylab.faca.keystore.filename}")
    private String keystoreFilename;

    @Value("${cosylab.faca.keystore.password}")
    private String keystorePassword;

    @Value("${cosylab.faca.certificate.tnta.filename}")
    private String tntaCertFilename;

    private KeyStoreClient keyStoreClient;

    private Provider bcProvider;

    @Getter
    private String identity;
    @Getter
    private PrivateKey myPrivateKey;
    @Getter
    private X509Certificate myCert;

    private X509Certificate tntaCert;
    @Getter
    private JWSTokenHandler jwsHandler;

    @PostConstruct
    public void init() throws NoSuchAlgorithmException, NoSuchProviderException, IOException, CertificateException, UnrecoverableKeyException, KeyStoreException {

        this.bcProvider = CryptoUtilFunctions.initializeSecurityProvider();
        this.keyStoreClient = new KeyStoreClient(keystoreFilename, keystorePassword);

        FileReader keyReader = new FileReader(tntaCertFilename);
        this.tntaCert = CertificateKeyConverter.convertPEMToX509(keyReader);

        this.initTrustInFog();

        this.jwsHandler = new JWSTokenHandler(this.myCert.getPublicKey(), this.myPrivateKey, this.identity);

    }

    private void initTrustInFog() throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, IOException, CertificateException {

        int iterCounter = 1;
        this.myPrivateKey = keyStoreClient.loadMyPrivateKey();
        ConfigurationAttribute idAttr = configAttrRepo.findByName(FACAGlobals.FACA_IDENTITY_KEY);
        ConfigurationAttribute certAttr = configAttrRepo.findByName(FACAGlobals.FACA_CERTIFICATE_KEY);


        if ((this.myPrivateKey == null) || (certAttr == null) || (idAttr == null)) {
            this.myPrivateKey = null;
            idAttr = null;
            certAttr = null;
            configAttrRepo.deleteByName(FACAGlobals.FACA_IDENTITY_KEY);
            configAttrRepo.deleteByName(FACAGlobals.FACA_CERTIFICATE_KEY);
            while ((iterCounter < 10 && ((this.myPrivateKey == null) || (certAttr == null) || (idAttr == null)))) {
                try {
                    // generate public key
                    KeyPair keyPair = PublicKeyGenerator.generatePublicKeyPair();
                    keyStoreClient.storeMyPrivateKey(keyPair.getPrivate(), this.tntaCert);
                    String ftaId = "";
                    CertificateValidationHolder resp = null;
                    //get cert from FTA

                    resp =
                            fogAmqpClient.sendAndReceiveAMQPRequest(FTARoutesConstants.FTA_QUEUE_NAME_CERTIFICATE_VALIDATE, null, CertificateValidationHolder.class);

                    ftaId = IdentityGenerator.getFTAIdFromFogServiceIdentifier(
                            CertificateKeyConverter.convertPEMToX509(
                                    resp.getCertificate()).getSubjectDN().getName());


                    // generate identity
                    this.identity = IdentityGenerator.generateFogServiceIdentifier(ftaId, FogServiceType.FACA);

                    // validate certificates signature using tnta certificate
                    CertificateHelper.verifyCertificateSignature(tntaCert.getPublicKey(), CertificateKeyConverter.convertPEMToX509(resp.getCertificate()));

                    // check fta's certificate in cloud
                    cloudClient.validateFTATicket(new TicketSignature(resp.getTicketToken(), "", resp.getTicketIssuer(), CloudConstants.TNTA_ROUTE_VALIDATE_TICKET));

                    PKCS10CertificationRequest request = CSRHandler.generateCSR(keyPair.getPrivate(), keyPair.getPublic(), CryptoUtilFunctions.generateCertCommonName(identity));
                    String csrContent = CertificateKeyConverter.convertPKCS10CertificationRequestToPEM(request);


                    FogNodeCSRResponse respCert = fogAmqpClient.sendAndReceiveAMQPRequest(FTARoutesConstants.FTA_QUEUE_NAME_CSR,
                            new FogComponentCSR(identity, csrContent), FogNodeCSRResponse.class);

                    this.myCert = CertificateKeyConverter.convertPEMToX509(respCert.getCertificateContent());


                    this.myPrivateKey = keyPair.getPrivate();

                    keyStoreClient.storeMyPrivateKey(this.myPrivateKey,
                            this.myCert);

                    if (idAttr == null) {
                        idAttr = new ConfigurationAttribute(FACAGlobals.FACA_IDENTITY_KEY, identity);
                    } else {
                        idAttr.setValue(identity);
                    }
                    configAttrRepo.save(idAttr);

                    if (certAttr == null) {
                        certAttr = new ConfigurationAttribute(FACAGlobals.FACA_CERTIFICATE_KEY, respCert.getCertificateContent());
                    } else {
                        certAttr.setValue(respCert.getCertificateContent());
                    }
                    configAttrRepo.save(certAttr);

                } catch (Exception e) {
                    logger.warn("Failed crypto initialization, iteration " + iterCounter, e);
                    iterCounter++;
                }
            }
        } else {
            this.identity = idAttr.getValue();
            this.myCert = CertificateKeyConverter.convertPEMToX509(certAttr.getValue());
        }

        if ((this.myPrivateKey == null) || (this.myCert == null) || (this.identity == null)) {
            throw new UnrecoverableKeyException("Crypto initilization failed!");
        } else {
            logger.info("Fog Trust init successful");
        }
    }

}
