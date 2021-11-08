package at.cosylab.fog.faca.commons.repositories.contextAttributesValues;

import context.payloads.AttributeValueChangeNotification;
import org.springframework.data.annotation.Id;

public class ContextAttributeValueEntity extends AttributeValueChangeNotification {

    @Id
    private String id;

    private boolean isCurrent;

    public ContextAttributeValueEntity() {
    }

    public ContextAttributeValueEntity(AttributeValueChangeNotification attributeValueChangeNotification) {
        super(attributeValueChangeNotification);
        this.isCurrent = true;
    }

    public String getId() {
        return id;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean current) {
        isCurrent = current;
    }

    @Override
    public String toString() {
        return "ContextAttributeValueEntity{" +
                "id='" + id + '\'' +
                ", isCurrent=" + isCurrent +
                "} " + super.toString();
    }
}
