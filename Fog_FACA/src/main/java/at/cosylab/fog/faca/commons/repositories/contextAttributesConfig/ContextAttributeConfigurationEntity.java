package at.cosylab.fog.faca.commons.repositories.contextAttributesConfig;

import context.payloads.ContextAttributeConfiguration;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

public class ContextAttributeConfigurationEntity extends ContextAttributeConfiguration {

    @Id
    private String id;

    public ContextAttributeConfigurationEntity() {
    }

    public ContextAttributeConfigurationEntity(ContextAttributeConfiguration contextAttributeConfiguration) {
        super(contextAttributeConfiguration);

    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "ContextAttributeConfigurationEntity{" +
                "id='" + id + '\'' +
                "} " + super.toString();
    }
}
