package at.cosylab.fog.ccaa.utils;

public class ConnectivityEngineGlobals {

    public static final String FTI_FLAG_DB_KEY = "FTI_EXECUTED";
    public static final String CCAA_IDENTITY_KEY = "CCAA_IDENTITY";
    public static final String CCAA_CERTIFICATE_KEY = "CCAA_CERTIFICATE";

    public static final String CONNECTIVITY_ATTRIBUTE_NAME = "ConnectedToCloud";
    public static final int  CONNECTIVITY_CHECK_PERIOD_SECONDS = 10;
    public static final long  CONNECTIVITY_ATTR_VALUE_EXPIRATION_SECONDS = 10 * CONNECTIVITY_CHECK_PERIOD_SECONDS;

    public static final float MAX_CERTAINTY = 100;
    public static final float MIN_CERTAINTY = 0;
    public static final float CERTAINTY_STEP = 10;

    public static final String EXCHANGE_NAME = "ccaa.rpc";
    public static final String ATTR_EVAL_QUEUE_NAME = "ccaa.rpc.attr.eval_attr_value";

    private static boolean shouldRun = true;

    public static boolean isShouldRun() {
        return shouldRun;
    }

    public static void setShouldRun(boolean shouldRun) {
        ConnectivityEngineGlobals.shouldRun = shouldRun;
    }
}
