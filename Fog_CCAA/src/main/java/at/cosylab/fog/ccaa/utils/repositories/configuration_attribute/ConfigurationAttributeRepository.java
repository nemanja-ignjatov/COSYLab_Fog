package at.cosylab.fog.ccaa.utils.repositories.configuration_attribute;

import at.cosylab.fog.ccaa.utils.repositories.configuration_attribute.ConfigurationAttribute;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConfigurationAttributeRepository extends MongoRepository<ConfigurationAttribute, String> {

    ConfigurationAttribute findByName(String attrName);
    void deleteByName(String name);
}
