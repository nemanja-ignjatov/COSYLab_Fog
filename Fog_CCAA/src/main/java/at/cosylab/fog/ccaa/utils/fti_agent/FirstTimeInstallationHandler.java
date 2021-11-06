package at.cosylab.fog.ccaa.utils.fti_agent;

import at.cosylab.fog.ccaa.utils.ConnectivityEngineGlobals;
import at.cosylab.fog.ccaa.utils.amqp.FogAMQPClient;
import at.cosylab.fog.ccaa.utils.cloud.CloudBackendHTTPClientService;
import at.cosylab.fog.ccaa.utils.pki.KeyStoreClient;
import at.cosylab.fog.ccaa.utils.repositories.configuration_attribute.ConfigurationAttribute;
import at.cosylab.fog.ccaa.utils.repositories.configuration_attribute.ConfigurationAttributeRepository;
import context.amqp.ContextAMQPRoutingConstants;
import context.attributes.AttributeNameGenerator;
import context.attributes.ContextAttributeConfidenceRange;
import context.attributes.ContextDistributionPattern;
import context.attributes.ContextType;
import context.payloads.ContextAttributeConfiguration;
import context.payloads.RegisterCAAttributesRequest;
import fog.amqp_utils.routes.FTARoutesConstants;
import fog.error_handling.amqp_exceptions.AMQPMessageParsingException;
import fog.error_handling.global_exceptions.InternalServerErrorException;
import fog.faca.access_rules.AccessRuleType;
import fog.globals.payloads.ServiceProcessingResponse;
import fog.payloads.fta.CertificateValidationHolder;
import fog.payloads.fta.FogComponentCSR;
import identities.FogServiceType;
import identities.IdentityGenerator;
import lombok.Getter;
import org.bouncycastle.operator.OperatorCreationException;
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
public class FirstTimeInstallationHandler {

    private static final Logger logger = LoggerFactory.getLogger(FirstTimeInstallationHandler.class);

    @Autowired
    private FogAMQPClient fogAmqpClient;

    @Autowired
    private ConfigurationAttributeRepository configAttrRepo;

    @Autowired
    private CloudBackendHTTPClientService cloudClient;

    @Value("${cosylab.ccaa.keystore.filename}")
    private String keystoreFilename;

    @Value("${cosylab.ccaa.keystore.password}")
    private String keystorePassword;

    @Value("${cosylab.ccaa.certificate.tnta.filename}")
    private String tntaCertFilename;

    private KeyStoreClient keyStoreClient;

    private Provider bcProvider;

    @Getter
    private PrivateKey myPrivateKey;
    @Getter
    private X509Certificate myCert;
    @Getter
    private String identity;

    private X509Certificate tntaCert;

    @PostConstruct
    public void init() throws NoSuchAlgorithmException, NoSuchProviderException, IOException, CertificateException, UnrecoverableKeyException, KeyStoreException {

        this.bcProvider = CryptoUtilFunctions.initializeSecurityProvider();
        this.keyStoreClient = new KeyStoreClient(keystoreFilename, keystorePassword);

        FileReader keyReader = new FileReader(tntaCertFilename);
        this.tntaCert = CertificateKeyConverter.convertPEMToX509(keyReader);

        ConfigurationAttribute configAttr = configAttrRepo.findByName(ConnectivityEngineGlobals.FTI_FLAG_DB_KEY);
        if (configAttr == null) {
            configAttr = new ConfigurationAttribute(ConnectivityEngineGlobals.FTI_FLAG_DB_KEY, Boolean.toString(false));
        }
        if (configAttr.getValue().equals(Boolean.toString(false))) {
            try {
                this.executeFTIProcedure();
                configAttr.setValue(Boolean.toString(true));
                configAttrRepo.save(configAttr);
            } catch (AMQPMessageParsingException e) {
                configAttrRepo.save(configAttr);
                e.printStackTrace();
            }
        }
    }

    private void executeFTIProcedure() throws NoSuchAlgorithmException, IOException, CertificateException, UnrecoverableKeyException, KeyStoreException {

        this.initTrustInFog();

        ContextAttributeConfiguration connectivityAttribute = new ContextAttributeConfiguration(
                AttributeNameGenerator.generateContextAttributeName(ContextType.CONNECTIVITY, ConnectivityEngineGlobals.CONNECTIVITY_ATTRIBUTE_NAME),
                ContextType.CONNECTIVITY, AccessRuleType.BOOLEAN, null, ContextDistributionPattern.PERIODIC,
                ConnectivityEngineGlobals.CONNECTIVITY_CHECK_PERIOD_SECONDS, ConnectivityEngineGlobals.CONNECTIVITY_ATTR_VALUE_EXPIRATION_SECONDS, new ContextAttributeConfidenceRange(ConnectivityEngineGlobals.MIN_CERTAINTY, ConnectivityEngineGlobals.MAX_CERTAINTY),
                ConnectivityEngineGlobals.ATTR_EVAL_QUEUE_NAME);

        logger.info(connectivityAttribute.toString());

        fogAmqpClient.sendAndReceiveContextAMQPRequest(ContextAMQPRoutingConstants.FACA_REGISTER_ATTRIBUTES, new RegisterCAAttributesRequest(connectivityAttribute), ServiceProcessingResponse.class);

        logger.info("FTI to FACA was successful");

    }

    private void initTrustInFog() throws NoSuchAlgorithmException, IOException, CertificateException, KeyStoreException, UnrecoverableKeyException {


        int iterCounter = 1;
        this.myPrivateKey = keyStoreClient.loadMyPrivateKey();
        ConfigurationAttribute idAttr = configAttrRepo.findByName(ConnectivityEngineGlobals.CCAA_IDENTITY_KEY);
        ConfigurationAttribute certAttr = configAttrRepo.findByName(ConnectivityEngineGlobals.CCAA_CERTIFICATE_KEY);


        if ((this.myPrivateKey == null) || (certAttr == null) || (idAttr == null)) {
            this.myPrivateKey = null;
            idAttr = null;
            certAttr = null;
            configAttrRepo.deleteByName(ConnectivityEngineGlobals.CCAA_IDENTITY_KEY);
            configAttrRepo.deleteByName(ConnectivityEngineGlobals.CCAA_CERTIFICATE_KEY);
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
                    //TODO improve certificate validation
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
                        idAttr = new ConfigurationAttribute(ConnectivityEngineGlobals.CCAA_IDENTITY_KEY, identity);
                    } else {
                        idAttr.setValue(identity);
                    }
                    configAttrRepo.save(idAttr);

                    if (certAttr == null) {
                        certAttr = new ConfigurationAttribute(ConnectivityEngineGlobals.CCAA_CERTIFICATE_KEY, respCert.getCertificateContent());
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
