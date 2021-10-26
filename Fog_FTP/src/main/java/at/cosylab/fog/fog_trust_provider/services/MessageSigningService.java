package at.cosylab.fog.fog_trust_provider.services;

import at.cosylab.fog.fog_trust_provider.repositories.thing_credentials.ThingCredentials;
import at.cosylab.fog.fog_trust_provider.repositories.thing_credentials.ThingCredentialsRepository;
import at.cosylab.fog.fog_trust_provider.utils.fti_agent.CryptoCredentialsManager;
import fog.globals.FogComponentsConstants;
import fog.payloads.ftp.ThingMessageSigningRequest;
import jws.JWSTokenHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import utils.AESUtil;
import utils.ECUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class MessageSigningService {

    public static String PLAIN = "PLAIN";
    public static String INTEGRITY = "SHA256";
    public static String SYMM = "AES-256";
    public static String ASYMM = "ECC";

    @Autowired
    private ThingCredentialsRepository thingCredentialsRepository;

    @Autowired
    private CryptoCredentialsManager cryptoManager;

    public String signThingMessage(ThingMessageSigningRequest request) {
        long start = System.currentTimeMillis();
        String retMessage = null;
        ThingCredentials thingData = thingCredentialsRepository.findByGeneratedId(request.getGeneratedId());
        log.info("Signing message for {} " + request.getGeneratedId());
        if ((thingData != null) && (thingData.isKeyExchanged())) {

            JWSTokenHandler jwsTokenHandler = new JWSTokenHandler(cryptoManager.getMyCert().getPublicKey(), cryptoManager.getMyPrivateKey(), cryptoManager.getIdentity(), thingData.getGeneratedId());

            Map<String, String> claims = new HashMap<>();
            String decryptedMessage = decryptThingsMessage(thingData.getGeneratedId(), request.getMessageContent(), thingData.getEncryptionAlgorithm(), thingData.getSecurityKey(), thingData.getKeySalt(), request.getMessageHash());
            claims.put(FogComponentsConstants.THING_SIGNATURE_FIELD_MESSAGE, decryptedMessage);
            claims.put(FogComponentsConstants.THING_SIGNATURE_FIELD_FUNCTIONALITY, request.getFunctionality());
            claims.put(FogComponentsConstants.THING_SIGNATURE_FIELD_SECURITY_PROFILE, thingData.getSecurityProfile().toString());

            retMessage = jwsTokenHandler.generateJWSToken(claims);
        }
        long duration = System.currentTimeMillis() - start;
        return duration + "#" + retMessage;
    }

    private String decryptThingsMessage(String devId, String messageContent, String encryptionAlgorithm, String securityKey, String keySalt, String messageHash) {
        String decryptedMessage = "";

        try {
            if (encryptionAlgorithm.equals(PLAIN)) {
                decryptedMessage = messageContent;
            } else if (encryptionAlgorithm.equals(INTEGRITY)) {
                String msgHash = DigestUtils.sha256Hex(messageContent);
                if (msgHash.equals(messageHash)) {
                    decryptedMessage = messageContent;
                }
            }
            if (encryptionAlgorithm.equals(SYMM)) {
                SecretKey aesKey = AESUtil.generateAESKey(securityKey, keySalt);
                IvParameterSpec ivspec = new IvParameterSpec(securityKey.getBytes(StandardCharsets.UTF_8));

                decryptedMessage = AESUtil.decryptString(messageContent, aesKey, ivspec);
            }
            if (encryptionAlgorithm.equals(ASYMM)) {
                decryptedMessage = ECUtils.decryptText(messageContent, cryptoManager.getMyPrivateKey());
            }
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException |
                InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return decryptedMessage;
    }
}
