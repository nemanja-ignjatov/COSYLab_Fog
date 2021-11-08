package at.cosylab.fog.faca.commons.repositories.attributesConfig;

import fog.faca.access_rules.AccessRuleType;
import fog.faca.utils.AttributeConstraint;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

public class AttributeConfiguration {

    @Id
    private String id;
    @Indexed(unique = true)
    private String name;

    private AccessRuleType accessRuleType;

    private AttributeConstraint constraint;

    public AttributeConfiguration() {
    }

    public AttributeConfiguration(String name, AccessRuleType accessRuleType) {
        this.name = name;
        this.accessRuleType = accessRuleType;
    }

    public AttributeConfiguration(String name, AccessRuleType accessRuleType, AttributeConstraint constraint) {
        this.name = name;
        this.accessRuleType = accessRuleType;
        this.constraint = constraint;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AccessRuleType getAccessRuleType() {
        return accessRuleType;
    }

    public void setAccessRuleType(AccessRuleType accessRuleType) {
        this.accessRuleType = accessRuleType;
    }

    public AttributeConstraint getConstraint() {
        return constraint;
    }

    public void setConstraint(AttributeConstraint constraint) {
        this.constraint = constraint;
    }

    @Override
    public String toString() {
        return "AttributeConfiguration{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", accessRuleType=" + accessRuleType +
                ", constraint=" + constraint +
                '}';
    }
}
