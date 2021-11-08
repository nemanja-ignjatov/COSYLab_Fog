package at.cosylab.fog.faca.commons.repositories.configuration_attribute;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

public class ConfigurationAttribute {

    @Id
    private String id;
    @Indexed(unique = true)
    private String name;

    private String value;

    public ConfigurationAttribute() {
    }

    public ConfigurationAttribute(String name, String value) {
        this.name = name;
        this.value = value;
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "ConfigurationAttribute{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
