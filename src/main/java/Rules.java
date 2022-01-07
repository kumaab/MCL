

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class Rules {
    private static final Double STRICT_THRESHOLD       = 0.98;
    private static final Double PRODUCT_THRESHOLD      = 0.90;
    private static final Double WEAK_THRESHOLD         = 0.80;
    private static final Double COMMIT_MSG_SIMILARITY  = 0.70;
    private static final Integer COMMIT_MSG_PREFIX_LEN = 11; // length of RANGER-JIRA
    private static final String SYSTEM_USER            = "Jenkins User";
    private static final Logger LOG                    = LogManager.getLogger(Rules.class);

    Rules(){}

    public static GitCommit filter(GitCommit commit, List<String> excludedModules){
        for (String module : excludedModules) {  // remove files which belong to excludedModules
            if (!module.isEmpty())
                commit.getFileNames().removeIf(fileName -> fileName.contains(module));
        }
        return commit;
    }

    public static boolean isSystemUser(GitCommit gitCommit){
        return gitCommit.getAuthor().equals(SYSTEM_USER);
    }

    public static boolean singletonMismatch(GitCommit gitCommit1, GitCommit gitCommit2){
        return gitCommit1.getFileNames().size() == 1 && (!containsAll(gitCommit2.getFileNames(), gitCommit1.getFileNames()));
    }

    public static boolean IQR(GitCommit gitCommit1, GitCommit gitCommit2){
        double len1 = gitCommit1.getFileNames().size() * 1.0;
        double len2 = gitCommit2.getFileNames().size() * 1.0;
        return (len2 <= 1.5*len1) && (len2 >= 0.5*len1);
    }

    public static boolean equal(List<String> A, List<String> B){
        return new HashSet<>(A).equals(new HashSet<>(B));
    }

    public static boolean containsAll(List<String> A, List<String> B){
        return new HashSet<>(A).containsAll(new HashSet<>(B));
    }

    public static ArrayList<Double> compareChangeSet(GitCommit commit1, GitCommit commit2){
        ArrayList<Double> ratios = new ArrayList<>();
        for (File file1 : commit1.getChangeSet()) {
            for (File file2 : commit2.getChangeSet()) {
                if (file1.fileName.equals(file2.fileName)) {
                    ratios.add(Utils.similarity(file1.content, file2.content));
                }
            }
        }
        return ratios;
    }

    private static long count(ArrayList<Double> ratios){
        long count = 0;
        for(Double ratio: ratios){
            if (ratio > STRICT_THRESHOLD ){
                count++;
            }
        }
        return count;
    }

    public static boolean allButOne(ArrayList<Double> ratios){
        return (ratios.size() - count(ratios)) <= 1 && ratios.size() > 1;
    }

    public static boolean majority(ArrayList<Double> ratios){
        return count(ratios) >= (ratios.size()/2 + 1) && ratios.size() > 1;
    }

    public static boolean product(ArrayList<Double> ratios){
        return ratios.stream().reduce(1.0, (a, b) -> a * b) > PRODUCT_THRESHOLD;
    }

    public static boolean commitMsgCheck(String commitMsg1, String commitMsg2){
        if (commitMsg2.contains(commitMsg1.substring(0,COMMIT_MSG_PREFIX_LEN)))
            return true;
        else{
            return Utils.similarity(commitMsg1.substring(COMMIT_MSG_PREFIX_LEN),
                    commitMsg2.substring(COMMIT_MSG_PREFIX_LEN)) > COMMIT_MSG_SIMILARITY;
        }
    }

    public static boolean subset(GitCommit gitCommit1, GitCommit gitCommit2){
        return containsAll(gitCommit2.getFileNames(), gitCommit1.getFileNames())
                && author(gitCommit1.getAuthor(), gitCommit2.getAuthor())
                && commitMsgCheck(gitCommit1.getCommitMessage(), gitCommit2.getCommitMessage());
    }

    public static String firstName(String name){
        String firstName = "";
        if (name.contains(".")){
            firstName = name.toLowerCase(Locale.ROOT).substring(0, name.indexOf("."));
        } else if (name.contains(" ")){
            firstName = name.toLowerCase(Locale.ROOT).split(" ")[0];
        }
        return firstName;
    }

    public static boolean author(String author1, String author2) {
        return author1.equals(author2) || firstName(author1).equals(firstName(author2));
    }

    public static boolean weakThreshold(ArrayList<Double> ratios){
        return ratios.stream().reduce(1.0, (a, b) -> a * b) > WEAK_THRESHOLD;
    }
}
