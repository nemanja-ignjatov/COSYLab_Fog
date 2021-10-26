package at.cosylab.fog.fog_trust_provider.services;

import at.cosylab.fog.fog_trust_provider.repositories.thing_credentials.ThingCredentials;
import at.cosylab.fog.fog_trust_provider.repositories.thing_credentials.ThingCredentialsRepository;
import at.cosylab.fog.fog_trust_provider.utils.fti_agent.CryptoCredentialsManager;
import fog.error_handling.custom_exceptions.CertificateIssuanceException;
import fog.error_handling.custom_exceptions.ThingKeyNotExchangedException;
import fog.payloads.ftp.InitializeIdentityRequest;
import fog.payloads.ftp.InitializeIdentityResponse;
import fog.payloads.ftp.RegisterThingRequest;
import fog.payloads.ftp.RegisterThingResponse;
import identities.IdentityGenerator;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pki.PublicKeyGenerator;
import pki.certificate.CSRHandler;
import pki.certificate.CertificateKeyConverter;
import utils.CryptoUtilFunctions;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Service
@Slf4j
public class ThingsRegistryService {

    @Autowired
    private CryptoCredentialsManager cryptoManager;

    @Autowired
    private ThingCredentialsRepository thingCredentialsRepository;

    public InitializeIdentityResponse initializeThingIdentity(InitializeIdentityRequest request) throws IOException {

        log.info("Initializing Thing Identity {} ", request);
        String ftpId = IdentityGenerator.getFCComponentIdFromFogServiceIdentifier(cryptoManager.getIdentity());
        String generatedId = IdentityGenerator.generateThingIdentifier(request.getDeviceType(), cryptoManager.getFtaIdentity(), ftpId);

        thingCredentialsRepository.save(new ThingCredentials(request.getId(), generatedId, request.getDeviceType(),
                request.getSecurityProfile(), request.getEncryptionAlgorithm()));
        return new InitializeIdentityResponse(generatedId, request.getSecurityProfile(), CertificateKeyConverter.convertX509ToPEM(cryptoManager.getMyCert()));
    }

    public RegisterThingResponse registerThing(RegisterThingRequest request) {

        log.info("Register Thing {} ", request);
        ThingCredentials credentials = thingCredentialsRepository.findByGeneratedId(request.getGeneratedId());
        if (!credentials.isKeyExchanged()) {
            throw new ThingKeyNotExchangedException();
        }

        try {
            // generate key pair
            KeyPair keyPair = PublicKeyGenerator.generatePublicKeyPair();
            PKCS10CertificationRequest csr = CSRHandler.generateCSR(keyPair.getPrivate(), keyPair.getPublic(),
                    CryptoUtilFunctions.generateCertCommonName(request.getGeneratedId()));
            X509Certificate thingCertificate = cryptoManager.signCertificate(csr);

            return new RegisterThingResponse(CertificateKeyConverter.convertX509ToPEM(thingCertificate),
                    CertificateKeyConverter.convertPrivateKeyToPEM(keyPair.getPrivate()));

        } catch (NoSuchProviderException | OperatorCreationException | CertificateException |
                NoSuchAlgorithmException | InvalidAlgorithmParameterException | IOException e) {
            throw new CertificateIssuanceException(e.toString(), e);
        }
    }
}
