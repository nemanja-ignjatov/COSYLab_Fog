package at.cosylab.fog.faca.commons.configuration;

import at.cosylab.fog.faca.commons.repositories.attributesConfig.AttributeConfiguration;
import at.cosylab.fog.faca.commons.repositories.attributesConfig.AttributeConfigurationRepository;
import fog.faca.enums.ATTRIBUTE;
import fog.faca.utils.AttributeConstraint;
import fog.faca.utils.FACAProjectConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
public class AttributesConfigBean {

    private static final Logger logger = LoggerFactory.getLogger(AttributesConfigBean.class);

    @Autowired
    private AttributeConfigurationRepository attrRepo;

    @PostConstruct
    public void init() {
        logger.info("[ATTR_CONFIG] Setting attributes configuration");
        List<AttributeConfiguration> attrs = attrRepo.findAll();
        if ((attrs == null) || (attrs.size() == 0)) {
            AttributeConfiguration attrStore = new AttributeConfiguration(ATTRIBUTE.USERNAME.toString(), ATTRIBUTE.USERNAME.getArType());
            attrRepo.save(attrStore);

            List<String> roleValues = new ArrayList<String>();
            for (FACAProjectConstants.Role role : FACAProjectConstants.Role.values()) {
                if (!role.equals(FACAProjectConstants.Role.UNDEFINED)) {
                    roleValues.add(role.getRoleName());
                }
            }
            AttributeConstraint roleConstraint = new AttributeConstraint(roleValues);
            attrStore = new AttributeConfiguration(ATTRIBUTE.ROLE.toString(), ATTRIBUTE.ROLE.getArType(), roleConstraint);
            attrRepo.save(attrStore);

            attrStore = new AttributeConfiguration(ATTRIBUTE.ORGANIZATION.toString(), ATTRIBUTE.ORGANIZATION.getArType());
            attrRepo.save(attrStore);

            attrStore = new AttributeConfiguration(ATTRIBUTE.POSITION.toString(), ATTRIBUTE.POSITION.getArType());
            attrRepo.save(attrStore);

            attrStore = new AttributeConfiguration(ATTRIBUTE.AGE.toString(), ATTRIBUTE.AGE.getArType(), new AttributeConstraint(0, null));
            attrRepo.save(attrStore);

            attrStore = new AttributeConfiguration(ATTRIBUTE.HANDICAP.toString(), ATTRIBUTE.HANDICAP.getArType());
            attrRepo.save(attrStore);

        }

    }
}
