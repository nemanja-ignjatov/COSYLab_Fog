package at.cosylab.fog.fog_trust_anchor.services;

import at.cosylab.fog.fog_trust_anchor.repositories.configuration_attribute.ConfigurationAttribute;
import at.cosylab.fog.fog_trust_anchor.repositories.configuration_attribute.ConfigurationAttributeRepository;
import at.cosylab.fog.fog_trust_anchor.utils.FTAGlobals;
import at.cosylab.fog.fog_trust_anchor.utils.fti_agent.CryptoCredentialsManager;
import exceptions.TicketValidationException;
import jws.JWSTokenHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import payloads.tnta.ticket.TicketCreationRequest;
import payloads.tnta.ticket.TicketSignature;
import payloads.tnta.ticket.TicketType;
import utils.CloudConstants;
import utils.CryptoConstants;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class TicketService {

    @Autowired
    private CryptoCredentialsManager cryptoManager;

    @Autowired
    private ConfigurationAttributeRepository configAttrRepo;

    public TicketSignature createSignedTicket(TicketCreationRequest request) {

        String messageHash = DigestUtils.sha256Hex(request.getMessageJson());

        ConfigurationAttribute attr = configAttrRepo.findByName(FTAGlobals.FTA_IDENTITY_KEY);

        JWSTokenHandler jwsHandler = new JWSTokenHandler(cryptoManager.getMyCert().getPublicKey(),
                cryptoManager.getMyPrivateKey(), attr.getValue(), request.getSubjectId());

        Map<String, String> claims = new HashMap<>();
        claims.put(CloudConstants.TICKET_FIELD_TYPE, request.getTicketType().toString());
        claims.put(CloudConstants.TICKET_FIELD_FUNCTIONALITY, request.getFunctionality());
        claims.put(CloudConstants.TICKET_FIELD_MESSAGE_HASH, messageHash);

        return new TicketSignature(jwsHandler.generateJWSToken(claims), request.getMessageJson(), attr.getValue(), request.getFunctionality());

    }

    public void validateCloudComponentTicket(TicketSignature request) throws TicketValidationException {

        ConfigurationAttribute attr = configAttrRepo.findByName(FTAGlobals.FTA_IDENTITY_KEY);

        // validate JWS signature
        // extract data from JWS
        JWSTokenHandler jwsHandler = new JWSTokenHandler(cryptoManager.getTntaCert().getPublicKey(),
                cryptoManager.getMyPrivateKey(), cryptoManager.getTntaCert().getSubjectDN().getName(), attr.getValue());

        // Check claims
        Map<String, Object> claims = jwsHandler.getClaimsFromJWS(request.getJwsEncoded());

        // check issueId
        if (!claims.get(CryptoConstants.ISSUER_JWS_NAME).equals(request.getIssuerId())) {
            throw new TicketValidationException();
        }
        // check expected ticketType
        if (!claims.get(CloudConstants.TICKET_FIELD_TYPE).equals(TicketType.RESPONSE.toString())) {
            throw new TicketValidationException();
        }
        // check expected functionality
        if (!claims.get(CloudConstants.TICKET_FIELD_FUNCTIONALITY).equals(request.getFunctionality())) {
            throw new TicketValidationException();
        }

        // check message hash
        if (!claims.get(CloudConstants.TICKET_FIELD_MESSAGE_HASH).equals(DigestUtils.sha256Hex(request.getMessageJson()))) {
            throw new TicketValidationException();
        }
    }
}
