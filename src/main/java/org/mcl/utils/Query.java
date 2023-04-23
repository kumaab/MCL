package org.mcl.utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mcl.model.GitCommit;
import org.mcl.model.Match;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Query {
    private static final Logger LOG = LogManager.getLogger(Query.class);
    private final int pad = 15;
    private final boolean showMissing;
    private final List<Match> resultSet;
    private final List<String> linesToDump;
    private int matches = 0;
    private int closeMatches = 0;
    private int commits = 0;
    private boolean fullSearch;

    Query(List<Match> result, boolean status, boolean showMissing){
        resultSet = result;
        this.showMissing = showMissing;
        this.linesToDump = new ArrayList<>();
        if (status)
            resultSet.sort((o1, o2) -> o2.getMatchCount() - o1.getMatchCount());// asc order sort by status
        else {
            // desc order sort by date
            resultSet.sort((o1, o2) -> o2.getOriginalCommit().getCommitDate().compareTo(o1.getOriginalCommit().getCommitDate()));
        }
    }

    private boolean authorMatch(String user, Match match){
        return match.getOriginalCommit().getAuthor().toLowerCase(Locale.ROOT).equals(user.toLowerCase(Locale.ROOT));
    }

    private boolean committerMatch(String user, Match match){
        return match.getOriginalCommit().getCommitter().toLowerCase(Locale.ROOT).equals(user.toLowerCase(Locale.ROOT));
    }

    String addHeader(){
        String res  = Utils.appendSpaces("Commit Message", 112);
        res  = res.concat(Utils.appendSpaces("Status", pad));
        res  = res.concat(Utils.padding());
        if(fullSearch){
            res  = res.concat(Utils.appendSpaces("Author", pad));
            res  = res.concat(Utils.padding());
            res  = res.concat(Utils.appendSpaces("Committer", pad));
            res  = res.concat(Utils.padding());
        }
        res  = res.concat(Utils.appendSpaces("Commit Date", pad));
        return res;
    }

    private String addInfo(GitCommit commit, String message){
        String res = Utils.appendSpaces(commit.getCommitMessage(), 100);
        res  = res.concat(Utils.padding());
        res  = res.concat(Utils.appendSpaces(message, pad)); // commit status
        res  = res.concat(Utils.padding());
        if(fullSearch){
            res  = res.concat(Utils.appendSpaces(commit.getAuthor(), pad));    // author
            res  = res.concat(Utils.padding());
            res  = res.concat(Utils.appendSpaces(commit.getCommitter(), pad)); // committer
            res  = res.concat(Utils.padding());
        }
        res  = res.concat(Utils.appendSpaces(commit.getCommitDate().toString(), pad));    // commit date
        return res;
    }

    public void logResult(Match match) {
        if (match.getMatchCount() > 0 && !showMissing) {
            LOG.info(addInfo(match.getOriginalCommit(), Constants.FOUND));
            LOG.info(addInfo(match.getBestMatch(), Constants.BEST_MATCH));
            matches++;
            LOG.info("");
        } else if (match.getMatchCount() == 0) {
            LOG.info(addInfo(match.getOriginalCommit(), Constants.NOT_FOUND));
            GitCommit commit = match.getOriginalCommit();
            String separator = ",";
            linesToDump.add(commit.getCommitMessage() + separator +
                            commit.getCommitDate().toString() + separator +
                            commit.getAuthor() + separator +
                            commit.getCommitHash() + separator);
            if (match.getClosestMatch() != null) {
                LOG.info(addInfo(match.getClosestMatch(), Constants.CLOSEST_MATCH));
                closeMatches++;
            }
            LOG.info("");
        }
        commits++;
    }

    protected void writeToFile(String fileName){
        try {
            Path file = Paths.get(fileName);
            LOG.info(linesToDump.size());
            Files.write(file, linesToDump, StandardCharsets.UTF_8);
        } catch (IOException e){
            LOG.info("Error in writing to file!");
        }
    }

    public void printStats(){
        if (commits == 0){
            LOG.info("No results found!");
        } else {
            String summary = "\n--- STATS ---\n" +
                    "Total Commits     : " + commits + "\n" +
                    "Total Matches     : " + matches + "\n" +
                    "Suspected Matches : " + closeMatches + "\n" +
                    "No Matches        : " + (commits - matches - closeMatches) + "\n" +
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
    }

    public void printResultForCommitter(String committer){
        LOG.info("Generating results for committer : " + committer);
        LOG.info(addHeader());
        for(Match match : resultSet){
            if (committerMatch(committer, match))
                logResult(match);
        }
    }

    public void printAllResults() {
        fullSearch = true;
        LOG.info(addHeader());
        for (Match match : resultSet) {
            logResult(match);
        }
    }
}
