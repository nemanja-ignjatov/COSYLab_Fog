package at.cosylab.fog.faca.commons.repositories.contextAttributesValues;


import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ContextAttributeValueRepository extends MongoRepository<ContextAttributeValueEntity, String> {

    List<ContextAttributeValueEntity> findAllByAttributeNameAndIsCurrent(String attributeName, boolean isCurrent);

    List<ContextAttributeValueEntity> findAllByAttributeNameStartsWithAndIsCurrent(String attributeName, boolean isCurrent);

    List<ContextAttributeValueEntity> findAllByAttributeNameInAndIsCurrent(List<String> contextPolicyAttributesName, boolean current);

    void deleteAllByTimestampLessThan(LocalDateTime timestamp);
}
