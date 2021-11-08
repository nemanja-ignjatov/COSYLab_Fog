package at.cosylab.fog.faca.commons.repositories.attributesConfig;

import at.cosylab.fog.faca.commons.repositories.attributesConfig.AttributeConfiguration;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AttributeConfigurationRepository extends MongoRepository<AttributeConfiguration, String> {

}
