package at.cosylab.fog.faca.commons.repositories.deviceType;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceTypeRepository extends MongoRepository<DeviceType, String>{

	DeviceType findDeviceTypeByCloudDeviceTypeId(String cloudId);
}
