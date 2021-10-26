package at.cosylab.fog.fog_trust_anchor.services;

import at.cosylab.fog.fog_trust_anchor.repositories.fogNode.FogComponentEntity;
import at.cosylab.fog.fog_trust_anchor.repositories.fogNode.FogComponentRepository;
import at.cosylab.fog.fog_trust_anchor.utils.fti_agent.CryptoCredentialsManager;
import exceptions.CSRHandlingException;
import fog.payloads.fta.CertificateValidationHolder;
import fog.payloads.fta.FogComponentCertificateData;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCSException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import payloads.tnta.ticket.TicketCreationRequest;
import payloads.tnta.ticket.TicketSignature;
import payloads.tnta.ticket.TicketType;
import pki.certificate.CertificateKeyConverter;
import utils.CloudConstants;
import utils.CryptoUtilFunctions;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static utils.CryptoConstants.OID_SHA256_ECDSA;

@Component
@Slf4j
public class CertificateManager {

    @Autowired
    private CryptoCredentialsManager cryptoManager;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private FogComponentRepository fogComponentRepository;

    public String handleCSR(String identity, String csrContent) throws CertificateException, OperatorCreationException, IOException, PKCSException, CSRHandlingException {

        PKCS10CertificationRequest csr = CertificateKeyConverter.convertPemToPKCS10CertificationRequest(csrContent);
        verifyCSR(CryptoUtilFunctions.generateCertCommonName(identity), csr);

        X509Certificate componentCertificate = cryptoManager.signCertificate(csr);
        fogComponentRepository.save(new FogComponentEntity(identity,
                CertificateKeyConverter.convertX509ToPEM(componentCertificate)));

        return CertificateKeyConverter.convertX509ToPEM(componentCertificate);
    }

    public CertificateValidationHolder getFTACertificateValidationData() throws IOException {
        TicketSignature ticket = ticketService.createSignedTicket(new TicketCreationRequest("", CloudConstants.TNTA_ROUTE_VALIDATE_TICKET, TicketType.REQUEST, cryptoManager.getTntaCert().getSubjectDN().getName()));
        return new CertificateValidationHolder(CertificateKeyConverter.convertX509ToPEM(cryptoManager.getMyCert()), ticket.getJwsEncoded(), ticket.getIssuerId());
    }

    public void verifyCSR(String identity, PKCS10CertificationRequest csrObject) throws OperatorCreationException, PKCSException, CSRHandlingException {

        // verify signature algorithm
        if (!csrObject.getSignatureAlgorithm().getAlgorithm().getId().equals(OID_SHA256_ECDSA)) {
            throw new CSRHandlingException("Signature algorithm wrong - " + csrObject.getSignatureAlgorithm().getAlgorithm().getId());
        }

        // verify subject name
        if (!csrObject.getSubject().toString().equals(identity)) {
            throw new CSRHandlingException("Subject name wrong - " + csrObject.getSubject().toString());
        }

        // validate if csr is signed with fn private key
        ContentVerifierProvider prov = new JcaContentVerifierProviderBuilder().build(csrObject.getSubjectPublicKeyInfo());
        if (!csrObject.isSignatureValid(prov)) {
            throw new CSRHandlingException("Signature wrong");

        }
    }


    public FogComponentCertificateData getComponentCertificate(String entityId) {

        FogComponentEntity fogComponentEntity = fogComponentRepository.findByIdentity(entityId);
        return new FogComponentCertificateData(fogComponentEntity.getIdentity(),
                fogComponentEntity.getCertificate(), fogComponentEntity.getCertificateRevokedAt() == null);
    }
}
