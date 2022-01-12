
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Locale;

public class Query {

    private static final String FOUND         = "FOUND";
    private static final String NOT_FOUND     = "NOT FOUND";
    private static final String BEST_MATCH    = "BEST MATCH";
    private static final String CLOSEST_MATCH = "CLOSEST MATCH";
    private static final int    PAD           = 15;
    private static final Logger LOG           = LogManager.getLogger(Query.class);
    private static boolean full_search = false, showMissing = false;
    private static int matches = 0, closematches = 0, commits = 0;
    private static List<Match> resultSet;

    Query(List<Match> result, boolean status, boolean showMissing){
        resultSet = result;
        Query.showMissing = showMissing;
        if (status)
            resultSet.sort((o1, o2) -> o2.getMatchCount() - o1.getMatchCount());// asc order sort by status
        else {
            // desc order sort by date
            resultSet.sort((o1, o2) -> o2.getOriginalCommit().getCommitDate().compareTo(o1.getOriginalCommit().getCommitDate()));
        }
    }

    private static boolean authorMatch(String user, Match match){
        return match.getOriginalCommit().getAuthor().toLowerCase(Locale.ROOT).equals(user.toLowerCase(Locale.ROOT));
    }

    private boolean committerMatch(String user, Match match){
        return match.getOriginalCommit().getCommitter().toLowerCase(Locale.ROOT).equals(user.toLowerCase(Locale.ROOT));
    }

    static String addHeader(){
        String res  = Utils.appendSpaces("Commit Message", 112);
        res  = res.concat(Utils.appendSpaces("Status", PAD));
        res  = res.concat(Utils.padding());
        if(full_search){
            res  = res.concat(Utils.appendSpaces("Author", PAD));
            res  = res.concat(Utils.padding());
            res  = res.concat(Utils.appendSpaces("Committer", PAD));
            res  = res.concat(Utils.padding());
        }
        res  = res.concat(Utils.appendSpaces("Commit Date", PAD));
        return res;
    }

    private static String addInfo(GitCommit commit, String message){
        String res = Utils.appendSpaces(commit.getCommitMessage(), 100);
        res  = res.concat(Utils.padding());
        res  = res.concat(Utils.appendSpaces(message, PAD)); // commit status
        res  = res.concat(Utils.padding());
        if(full_search){
            res  = res.concat(Utils.appendSpaces(commit.getAuthor(), PAD));    // author
            res  = res.concat(Utils.padding());
            res  = res.concat(Utils.appendSpaces(commit.getCommitter(), PAD)); // committer
            res  = res.concat(Utils.padding());
        }
        res  = res.concat(Utils.appendSpaces(commit.getCommitDate().toString(), PAD));    // commit date
        return res;
    }

    public void logResult(Match match) {
        if (match.getMatchCount() > 0 && !showMissing) {
            LOG.info(addInfo(match.getOriginalCommit(), FOUND));
            LOG.info(addInfo(match.getBestMatch(), BEST_MATCH));
            matches++;
            LOG.info("");
        } else if (match.getMatchCount() == 0) {
            LOG.info(addInfo(match.getOriginalCommit(), NOT_FOUND));
            if (match.getClosestMatch() != null) {
                LOG.info(addInfo(match.getClosestMatch(), CLOSEST_MATCH));
                closematches++;
            }
            LOG.info("");
        }
        commits++;
    }

    public void printStats(){
        if (commits == 0){
            LOG.info("No results found!");
        } else {
            String summary = "\n--- STATS ---\n" +
                    "Total Commits     : " + commits + "\n" +
                    "Total Matches     : " + matches + "\n" +
                    "Suspected Matches : " + closematches + "\n" +
                    "No Matches        : " + (commits - matches - closematches) + "\n" +
                    "Commits Match %   : " + String.format("%.2f", (matches * 100.00) / commits);
            LOG.info(summary);
        }
    }

    public void printResultForAuthor(String author){
        LOG.info("Generating results for author : " + author);
        LOG.info(addHeader());
        for(Match match : resultSet) {
            if (authorMatch(author, match))
                logResult(match);
        }
        printStats();
    }

    public void printResultForCommitter(String committer){
        LOG.info("Generating results for committer : " + committer);
        LOG.info(addHeader());
        for(Match match : resultSet){
            if (committerMatch(committer, match))
                logResult(match);
        }
        printStats();
    }

    public void printAllResults() {
        full_search = true;
        LOG.info(addHeader());
        for (Match match : resultSet) {
            logResult(match);
        }
        printStats();
    }
}
