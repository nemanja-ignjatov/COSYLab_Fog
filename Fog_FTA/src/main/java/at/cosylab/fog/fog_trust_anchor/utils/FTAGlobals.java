package at.cosylab.fog.fog_trust_anchor.utils;

public class FTAGlobals {

    public static final String FTI_CLOUD_FLAG_DB_KEY = "FTI_EXECUTED_WITH_CLOUD";
    public static final String FTA_IDENTITY_KEY = "FTA_IDENTITY";
    public static final String FTA_CERTIFICATE_KEY = "FTA_CERTIFICATE";

    private static boolean shouldRun = true;

    public static boolean isShouldRun() {
        return shouldRun;
    }

    public static void setShouldRun(boolean shouldRun) {
        FTAGlobals.shouldRun = shouldRun;
    }


}
