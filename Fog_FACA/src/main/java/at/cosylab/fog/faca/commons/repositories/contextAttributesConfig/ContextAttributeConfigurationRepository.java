package at.cosylab.fog.faca.commons.repositories.contextAttributesConfig;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ContextAttributeConfigurationRepository extends MongoRepository<ContextAttributeConfigurationEntity, String> {

    ContextAttributeConfigurationEntity findByAttributeName(String attributeName);

    List<ContextAttributeConfigurationEntity> findAllByAttributeNameIn(List<String> contextAttributeNames);
}
