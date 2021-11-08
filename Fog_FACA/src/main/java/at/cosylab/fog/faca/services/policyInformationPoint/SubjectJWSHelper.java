package at.cosylab.fog.faca.services.policyInformationPoint;

import at.cosylab.fog.faca.commons.exceptions.UnauthorizedAccessException;
import at.cosylab.fog.faca.commons.fti_agent.CryptoInstallationHandler;
import at.cosylab.fog.faca.commons.repositories.subject.Subject;
import fog.faca.enums.ATTRIBUTE;
import fog.faca.utils.FACAProjectConstants;
import fog.faca.utils.JSONUtilFunctions;
import fog.payloads.faca.PDP.AttributeValueWrapper;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

@Service
public class SubjectJWSHelper {

    private static final Logger logger = LoggerFactory.getLogger(SubjectJWSHelper.class);

    @Autowired
    private CryptoInstallationHandler keyHandler;

    public String generateJWSToken(Subject subject) {
        Map<String, String> attributes = this.mapSubjectToTokenAttributes(subject);
        String jws = keyHandler.getJwsHandler().generateJWSTokenForSubject(attributes, subject.getUserName());
        logger.info(jws);
        return jws;
    }

    private Map<String, String> mapSubjectToTokenAttributes(Subject subject) {
        Map<String, String> retMap = new HashMap<>();
        ZoneId defaultZoneId = ZoneId.systemDefault();

        String[] positions = new String[subject.getProfessions().size()];
        String[] organizations = new String[subject.getProfessions().size()];

        retMap.put(ATTRIBUTE.AGE.toString(), String.valueOf(Calendar.getInstance().get(Calendar.YEAR) - subject.getBirthdate().toInstant().atZone(defaultZoneId).getYear()));

        retMap.put(ATTRIBUTE.HANDICAP.toString(), JSONUtilFunctions.convertListStringToJSON(subject.getHandicaps()));

        for (int i = 0; i < subject.getProfessions().size(); i++) {
            positions[i] = subject.getProfessions().get(i).getPosition();
            organizations[i] = subject.getProfessions().get(i).getOrganization();
        }
        retMap.put(ATTRIBUTE.ORGANIZATION.toString(), JSONUtilFunctions.convertListStringToJSON(Arrays.asList(organizations)));
        retMap.put(ATTRIBUTE.POSITION.toString(), JSONUtilFunctions.convertListStringToJSON(Arrays.asList(positions)));

        retMap.put(ATTRIBUTE.ROLE.toString(), String.valueOf(subject.getRole().getRoleName().name()));
        retMap.put(ATTRIBUTE.USERNAME.toString(), subject.getUserName());
        retMap.put(FACAProjectConstants.USER_PROXY_ID, subject.getUserProxyId());
        return retMap;

    }

    public HashMap<String, AttributeValueWrapper> mapTokenToSubjectAttrs(String jws) {

        Map<String, Object> claims = keyHandler.getJwsHandler().getClaimsFromJWS(jws);
        HashMap<String, AttributeValueWrapper> map = new HashMap<String, AttributeValueWrapper>();

        map.put(ATTRIBUTE.AGE.toString(), new AttributeValueWrapper(String.valueOf(claims.get(ATTRIBUTE.AGE.toString()))));

        map.put(ATTRIBUTE.ORGANIZATION.toString(), new AttributeValueWrapper(String.valueOf(claims.get(ATTRIBUTE.ORGANIZATION.toString()))));
        map.put(ATTRIBUTE.POSITION.toString(), new AttributeValueWrapper(String.valueOf(claims.get(ATTRIBUTE.POSITION.toString()))));

        map.put(ATTRIBUTE.ROLE.toString(), new AttributeValueWrapper(String.valueOf(claims.get(ATTRIBUTE.ROLE.toString()))));
        map.put(ATTRIBUTE.USERNAME.toString(), new AttributeValueWrapper(String.valueOf(claims.get(ATTRIBUTE.USERNAME.toString()))));
        map.put(FACAProjectConstants.USER_PROXY_ID, new AttributeValueWrapper(String.valueOf(claims.get(FACAProjectConstants.USER_PROXY_ID))));

        return map;
    }

    public FACAProjectConstants.Role getRoleFromJWS(String jws) throws UnauthorizedAccessException {
        try {
            String roleString = keyHandler.getJwsHandler().getClaimFromJWS(jws, ATTRIBUTE.ROLE.toString());
            return FACAProjectConstants.Role.valueOf(roleString);
        } catch (SignatureException e) {
            throw new UnauthorizedAccessException();
        }

    }

    public String getUsernameFromJWS(String jws) throws UnauthorizedAccessException {
        try {
            return keyHandler.getJwsHandler().getClaimFromJWS(jws, ATTRIBUTE.USERNAME.toString());
        } catch (SignatureException e) {
            throw new UnauthorizedAccessException();
        }
    }
}
