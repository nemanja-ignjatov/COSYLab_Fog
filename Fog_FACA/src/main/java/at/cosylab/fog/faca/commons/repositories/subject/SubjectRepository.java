package at.cosylab.fog.faca.commons.repositories.subject;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends MongoRepository<Subject, String>{

	Subject findByUserName(String userName);

}
