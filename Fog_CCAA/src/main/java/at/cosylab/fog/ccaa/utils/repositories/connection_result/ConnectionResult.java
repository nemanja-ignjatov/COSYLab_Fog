package at.cosylab.fog.ccaa.utils.repositories.connection_result;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class ConnectionResult {

    @Id
    private String id;

    private boolean value;
    private double certainty;

    @JsonDeserialize(
            using = LocalDateTimeDeserializer.class
    )
    @JsonSerialize(
            using = LocalDateTimeSerializer.class
    )
    private LocalDateTime timestamp;

    public ConnectionResult() {
    }

    public ConnectionResult(boolean value,  double certainty) {
        this.value = value;
        this.certainty = certainty;
        this.timestamp = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public double getCertainty() {
        return certainty;
    }

    public void setCertainty(double certainty) {
        this.certainty = certainty;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ConnectionResultAttribute{" +
                "id='" + id + '\'' +
                ", value=" + value +
                ", certainty=" + certainty +
                ", timestamp=" + timestamp +
                '}';
    }
}
