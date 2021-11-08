package at.cosylab.fog.faca.commons.repositories.policy;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRepository extends MongoRepository<Policy, String> {

    Policy findByDeviceNameAndFunctionAndPriority(String deviceName, String function, int priority);

    Policy findByDeviceNameAndPriority(String deviceName, int priority);

    List<Policy> findAllByDeviceName(String devName);

    List<Policy> findAllBycloudDeviceTypeIdAndFunction(String cloudDeviceTypeId, String function);

    List<Policy> findAllByAttributeNamesStartsWith(String attributeName);
}
