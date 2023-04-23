package org.mcl.utils;

public class Constants {
    public static final String SOURCE_ALIAS       = "source.alias";
    public static final String SOURCE_LOCAL_PATH  = "source.local.path";
    public static final String SOURCE_GIT_URL     = "source.git.url";
    public static final String SOURCE_BRANCH_NAME = "source.branch.name";
    public static final String TARGET_ALIAS       = "target.alias";
    public static final String TARGET_LOCAL_PATH  = "target.local.path";
    public static final String TARGET_GIT_URL     = "target.git.url";
    public static final String TARGET_BRANCH_NAME = "target.branch.name";
    public static final String EXCLUDED_MODS           = "excluded.mods";
    public static final String REVERSE_EXCLUDED_MODS   = "reverse.excluded.mods";
    public static final Integer COMMIT_MSG_PREFIX_LEN  = 11; // length of RANGER-JIRA
    public static final String SYSTEM_USER             = "Jenkins User";
    public static final String FOUND              = "FOUND";
    public static final String NOT_FOUND          = "NOT FOUND";
    public static final String BEST_MATCH         = "BEST MATCH";
    public static final String CLOSEST_MATCH      = "CLOSEST MATCH";
    public static final String propertiesFile     = "src/main/resources/config.properties";
    public static final String dumpFile           = "dump.txt";

    static class Thresholds {
        public static final Double STRICT_THRESHOLD       = 0.98;
        public static final Double PRODUCT_THRESHOLD      = 0.90;
        public static final Double WEAK_THRESHOLD         = 0.80;
        public static final Double COMMIT_MSG_SIMILARITY  = 0.70;
    }
}
