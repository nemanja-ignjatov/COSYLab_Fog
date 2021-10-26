package at.cosylab.fog.fog_trust_anchor.repositories.fogNode;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document
@Data
@NoArgsConstructor
public class FogComponentEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String identity;
    private String certificate;
    private LocalDateTime certificateRevokedAt;

    private LocalDateTime registeredAt;

    public FogComponentEntity(String identity, String certificate) {
        this.identity = identity;
        this.certificate = certificate;
        this.registeredAt = LocalDateTime.now();
    }
}
