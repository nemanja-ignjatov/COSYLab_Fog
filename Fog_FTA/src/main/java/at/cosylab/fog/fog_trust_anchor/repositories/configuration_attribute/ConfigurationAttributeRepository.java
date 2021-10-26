package at.cosylab.fog.fog_trust_anchor.repositories.configuration_attribute;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConfigurationAttributeRepository extends MongoRepository<ConfigurationAttribute, String> {

    ConfigurationAttribute findByName(String attrName);
}
