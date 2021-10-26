package at.cosylab.fog.fog_trust_provider.utils;

import lombok.Getter;
import lombok.Setter;

public class FTPGlobals {

    public static final String FTP_IDENTITY_KEY = "FTP_IDENTITY";
    public static final String FTP_CERTIFICATE_KEY = "FTP_CERTIFICATE";
    @Getter
    @Setter
    private static boolean shouldRun = true;

    public static final String EXCHANGE_NAME = "ftp.rpc";

    public static final String INIT_THING_CREDENTIALS_QUEUE_NAME = "ftp.rpc.initialize_identity";
    public static final String REGISTER_THING_QUEUE_NAME = "ftp.rpc.register";
    public static final String SIGN_THING_MESSAGE_QUEUE_NAME = "ftp.rpc.sign_message";

    public static final String FTP_QUEUE_NAME_OCSP = "ftp.rpc.certificate.ocsp";

    public static final String AMQP_PROTOCOL_PREFIX = "amqp://";
}
