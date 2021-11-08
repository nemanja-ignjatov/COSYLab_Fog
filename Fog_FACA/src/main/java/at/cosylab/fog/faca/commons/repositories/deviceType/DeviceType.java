package at.cosylab.fog.faca.commons.repositories.deviceType;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import payloads.acam.deviceTypes.DeviceTypeDTO;
import payloads.acam.deviceTypes.DeviceTypeFunctionality;
import payloads.acam.deviceTypes.DeviceTypeVersionData;

import java.util.List;

public class DeviceType {

	@Id
	private String id;

	@Indexed(unique = true)
	private String cloudDeviceTypeId;

	@Indexed(unique = true)
	private String typeName;

	private String serviceProvider;

	private List<DeviceTypeFunctionality> functionalities;

	private DeviceTypeVersionData currentVersion;

	public DeviceType() {
	}

	public DeviceType(DeviceTypeDTO devTypeDTO) {
		this.cloudDeviceTypeId = devTypeDTO.getId();
		this.typeName = devTypeDTO.getTypeName();
		this.serviceProvider = devTypeDTO.getServiceProvider();
		this.functionalities = devTypeDTO.getFunctionalities();
		this.currentVersion = devTypeDTO.getCurrentVersion();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCloudDeviceTypeId() {
		return cloudDeviceTypeId;
	}

	public void setCloudDeviceTypeId(String cloudDeviceTypeId) {
		this.cloudDeviceTypeId = cloudDeviceTypeId;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public String getServiceProvider() {
		return serviceProvider;
	}

	public void setServiceProvider(String serviceProvider) {
		this.serviceProvider = serviceProvider;
	}

	public List<DeviceTypeFunctionality> getFunctionalities() {
		return functionalities;
	}

	public void setFunctionalities(List<DeviceTypeFunctionality> functionalities) {
		this.functionalities = functionalities;
	}

	public DeviceTypeVersionData getCurrentVersion() {
		return currentVersion;
	}

	public void setCurrentVersion(DeviceTypeVersionData currentVersion) {
		this.currentVersion = currentVersion;
	}

	public DeviceTypeDTO toDTO() {
		return new DeviceTypeDTO(this.cloudDeviceTypeId, this.typeName, this.serviceProvider, this.functionalities, this.currentVersion);
	}

	@Override
	public String toString() {
		return "DeviceType{" +
				"id='" + id + '\'' +
				", cloudDeviceTypeId='" + cloudDeviceTypeId + '\'' +
				", typeName='" + typeName + '\'' +
				", serviceProvider='" + serviceProvider + '\'' +
				", functionalities=" + functionalities +
				", currentVersion=" + currentVersion +
				'}';
	}
}
