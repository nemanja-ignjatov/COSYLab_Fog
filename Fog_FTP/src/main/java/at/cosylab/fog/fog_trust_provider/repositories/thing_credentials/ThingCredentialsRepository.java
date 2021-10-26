package at.cosylab.fog.fog_trust_provider.repositories.thing_credentials;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ThingCredentialsRepository extends MongoRepository<ThingCredentials, String> {

    ThingCredentials findByGeneratedId(String attrName);
}
