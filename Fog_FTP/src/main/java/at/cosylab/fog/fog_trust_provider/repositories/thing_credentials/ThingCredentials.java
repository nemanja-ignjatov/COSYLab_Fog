package at.cosylab.fog.fog_trust_provider.repositories.thing_credentials;

import fog.payloads.ftp.ThingSecurityProfile;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Document
public class ThingCredentials {

    @Id
    private String id;
    @Indexed(unique = true)
    private String registeredId;
    @Indexed(unique = true)
    private String generatedId;

    private ThingSecurityProfile securityProfile;

    private String encryptionAlgorithm;

    private String securityKey;

    private String keySalt;

    private boolean keyExchanged;

    private String deviceType;

    public ThingCredentials(String registeredId, String generatedId, String deviceType,
                            ThingSecurityProfile securityProfile, String encryptionAlgorithm) {
        this.registeredId = registeredId;
        this.generatedId = generatedId;
        this.deviceType = deviceType;
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.securityProfile = securityProfile;
        this.keyExchanged = false;
    }
}
