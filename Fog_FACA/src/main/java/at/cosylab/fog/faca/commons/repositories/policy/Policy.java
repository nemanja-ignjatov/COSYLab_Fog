package at.cosylab.fog.faca.commons.repositories.policy;

import fog.faca.access_rules.IAccessRule;
import org.springframework.data.annotation.Id;

import java.util.List;

/**
 * The Policy class is just a Java representation of the persistently saved policies in .xml
 * files.
 * 
 * @author Ivo Vidovic, 1406309
 *
 */
public class Policy {

	@Id
    private String id;

	private String deviceName;
	private String cloudDeviceTypeId;
    private String deviceTypeName;
	private String function;
    private IAccessRule rule;
	private int priority;

	private boolean enabled;

	private List<String> attributeNames;

    public Policy() {
    }

    public Policy(String deviceName, String cloudDeviceTypeId, String deviceTypeName, String function, IAccessRule rule, int priority, boolean enabled) {
        this.deviceName = deviceName;
        this.cloudDeviceTypeId = cloudDeviceTypeId;
        this.deviceTypeName = deviceTypeName;
        this.function = function;
        this.rule = rule;
        this.priority = priority;
        this.enabled = enabled;
    }

    public Policy(String deviceName, IAccessRule rule, int priority, boolean enabled) {
        this.deviceName = deviceName;
        this.rule = rule;
        this.priority = priority;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getFunction() {
		return function;
	}

	public void setFunction(String function) {
		this.function = function;
	}

    public IAccessRule getRule() {
        return rule;
    }

    public void setRule(IAccessRule rule) {
        this.rule = rule;
	}

	public int getPriority() {
		return priority;
	}

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getCloudDeviceTypeId() {
        return cloudDeviceTypeId;
    }

    public void setCloudDeviceTypeId(String cloudDeviceTypeId) {
        this.cloudDeviceTypeId = cloudDeviceTypeId;
    }

    public String getDeviceTypeName() {
        return deviceTypeName;
    }

    public void setDeviceTypeName(String deviceTypeName) {
        this.deviceTypeName = deviceTypeName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getAttributeNames() {
        return attributeNames;
    }

    public void setAttributeNames(List<String> attributeNames) {
        this.attributeNames = attributeNames;
    }

    @Override
    public String toString() {
        return "Policy{" +
                "id='" + id + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", cloudDeviceTypeId='" + cloudDeviceTypeId + '\'' +
                ", deviceTypeName='" + deviceTypeName + '\'' +
                ", function='" + function + '\'' +
                ", rule=" + rule +
                ", priority=" + priority +
                '}';
    }
}
