package at.cosylab.fog.fog_trust_anchor.repositories.fogNode;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface FogComponentRepository extends MongoRepository<FogComponentEntity, String> {

    FogComponentEntity findByIdentity(String identity);

}
