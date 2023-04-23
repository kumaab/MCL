package org.mcl.utils;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.mcl.config.Config;
import org.mcl.model.GitCommit;
import org.mcl.model.GitRepo;
import org.mcl.model.Match;

import java.time.ZoneId;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Comparator;

public class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);
    private GitRepo source;
    private GitRepo target;
    private String searchAuthorName;
    private String searchCommitterName;
    private int numberOfDays;
    private int numberOfMonths;
    private boolean isSortEnabled;
    private boolean isMissingEnabled;
    private boolean isDumpEnabled;
    private boolean isStatsEnabled;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate searchStartDate;
    private LocalDate searchEndDate;
    private List<String> excludedMods;
    private List<String> reverseExcludedMods;
    private Properties props;
    private boolean cloneSource;
    private boolean cloneTarget;
    private final List<Match> resultSet;

    public Properties getProperties(){
        return props;
    }

    public boolean toCloneSource(){
        return cloneSource;
    }

    public Main(){
        Config config = new Config();
        resultSet = new ArrayList<>();
        props = config.readProperties();
        if (props.getProperty(Constants.SOURCE_GIT_URL).equals(props.getProperty(Constants.TARGET_GIT_URL))){
            LOG.info("Initiating branch comparison  ..");
        } else {
            LOG.info("Initiating repo comparison ..");
        }
        if(!config.isDir(props.getProperty(Constants.SOURCE_LOCAL_PATH)))
            cloneSource = true;
        if(!config.isDir(props.getProperty(Constants.TARGET_LOCAL_PATH)))
            cloneTarget = true;

        if (props.getProperty(Constants.EXCLUDED_MODS) != null)
            excludedMods        = Arrays.asList(props.getProperty(Constants.EXCLUDED_MODS).split(","));
        if (props.getProperty(Constants.REVERSE_EXCLUDED_MODS) != null)
            reverseExcludedMods = Arrays.asList(props.getProperty(Constants.REVERSE_EXCLUDED_MODS).split(","));
    }

    public void initRepo() {
        source  = new GitRepo(
                props.getProperty(Constants.SOURCE_LOCAL_PATH),
                props.getProperty(Constants.SOURCE_GIT_URL),
                props.getProperty(Constants.SOURCE_BRANCH_NAME),
                props.getProperty(Constants.SOURCE_ALIAS),
                cloneSource);
        target = new GitRepo(
                props.getProperty(Constants.TARGET_LOCAL_PATH),
                props.getProperty(Constants.TARGET_GIT_URL),
                props.getProperty(Constants.TARGET_BRANCH_NAME),
                props.getProperty(Constants.TARGET_ALIAS),
                cloneTarget);
    }

    public void parseArgs(String[] args){
        Arguments ag  = new Arguments(args);
        this.searchAuthorName = ag.getAuthorName();
        this.searchCommitterName = ag.getCommitterName();
        this.numberOfDays = ag.getNumberOfDays();
        this.numberOfMonths = ag.getNumberOfMonths();
        this.isSortEnabled = ag.isSortEnabled();
        this.isMissingEnabled = ag.isMissingEnabled();
        this.isDumpEnabled = ag.isDumpEnabled();
        this.isStatsEnabled = ag.isStatsEnabled();

        // sets the dates
        this.endDate = LocalDate.now();

        if (numberOfDays > 0)
            this.startDate = endDate.minus(numberOfDays, ChronoUnit.DAYS);
        else if (numberOfMonths > 0)
            this.startDate = endDate.minus(numberOfMonths, ChronoUnit.MONTHS);
        else // default when nothing is passed
            this.startDate = endDate.minus(12, ChronoUnit.MONTHS);

        this.searchStartDate = startDate.minus(1, ChronoUnit.MONTHS);
        this.searchEndDate   = endDate.plus(1, ChronoUnit.MONTHS);
    }

    public ArrayList<GitCommit> process(GitRepo gitRepo, LocalDate start, LocalDate end) throws GitAPIException {
        Utils.initFormatter(gitRepo.getRepo());
        Iterable<RevCommit> commitLog = gitRepo.getGit().log().call();
        ArrayList<GitCommit> result  = new ArrayList<>();
        long mergedCommits = 0, totalCommits = 0;
        for (RevCommit curCommit : commitLog) {
            String commitHash     = curCommit.getId().getName();
            String author         = curCommit.getAuthorIdent().getName();
            String committer      = curCommit.getCommitterIdent().getName();
            LocalDate commitDate  = curCommit.getCommitterIdent().getWhen().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            GitCommit gitCommit = new GitCommit(committer, curCommit.getShortMessage(), commitHash, author, commitDate);
            if (start.compareTo(commitDate) < 0 && end.compareTo(commitDate) > 0) { // only process commits in the window
                totalCommits++;
                if (gitCommit.getCommitMessage().startsWith("Merge")){ // exclude merge commits
                    mergedCommits += 1;
                    continue;
                }
                gitCommit.setFileMeta(Utils.parse(curCommit));
                gitCommit.setFileNames();
                result.add(gitCommit);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(gitRepo.getRepoAlias() + " Total Commits : " + totalCommits);
            LOG.debug(gitRepo.getRepoAlias() + " Merge Commits : " + mergedCommits + " (Excluded)!");
        }
        return result;
    }

    public void search(ArrayList<GitCommit> sourceCommits, ArrayList<GitCommit> targetCommits) {
        int matches = 0;
        int commits = 0;
        for(GitCommit sourceCommit: sourceCommits){
            if (Rules.isSystemUser(sourceCommit))    continue;
            double maxCosine = 0.0;
            ArrayList<Double> closestRatios = new ArrayList<>();
            GitCommit closest = null;
            GitCommit sourceFCommit = Rules.filter(sourceCommit, reverseExcludedMods);

            Match match = new Match(sourceFCommit);
            if (sourceFCommit.getFileNames().size() > 0) {
                for (GitCommit targetCommit : targetCommits) {
                    if (Rules.isSystemUser(targetCommit))    continue;

                    long daysBetween           = ChronoUnit.DAYS.between(sourceFCommit.getCommitDate(), targetCommit.getCommitDate());
                    GitCommit targetFCommit = Rules.filter(targetCommit, excludedMods);
                    ArrayList<Double> ratios   = Rules.compareChangeSet(sourceFCommit, targetFCommit);
                    boolean equalSets          = Rules.equal(sourceFCommit.getFileNames(), targetFCommit.getFileNames());

                    if(Rules.singletonMismatch(sourceFCommit, targetFCommit) || !Rules.IQR(sourceFCommit, targetFCommit)){
                        continue;
                    }

                    Double sumCosine = ratios.stream().reduce(0.0, Double::sum);
                    if (sumCosine > maxCosine) {
                        closestRatios = ratios;
                        maxCosine = sumCosine;
                        closest = targetFCommit;
                    }

                    if (ratios.size() == 1 && equalSets && Rules.weakThreshold(ratios)){
                        match.found(targetFCommit, ratios);
                    } else if (equalSets && Rules.product(ratios) && ratios.size() > 0) {
                        match.found(targetFCommit, ratios);
                    } else if (Rules.allButOne(ratios)){
                        match.found(targetFCommit, ratios);
                    } else if (Rules.majority(ratios) && Rules.commitMsgCheck(sourceFCommit.getCommitMessage(), targetFCommit.getCommitMessage())){
                        match.found(targetFCommit, ratios);
                    } else if (Rules.subset(sourceFCommit, targetFCommit)){
                        match.found(targetFCommit, ratios);
                    }
                }
                if (match.getMatchCount() > 0) {
                    matches++;
                } else {
                    match.setClosestMatch(closest);
                    match.setClosestRatios(closestRatios);
                }
                resultSet.add(match);
            }
            commits++;
        }
    }

    public void displayResults(){
        Query query = new Query(resultSet, isSortEnabled, isMissingEnabled);
        if (searchAuthorName != null)
            query.printResultForAuthor(searchAuthorName);
        else if (searchCommitterName != null)
            query.printResultForCommitter(searchCommitterName);
        else {
            query.printAllResults();
        }
        if (isDumpEnabled){
            query.writeToFile(Constants.dumpFile);
        }
        if (isStatsEnabled){
            query.printStats();
        }
    }

    public void execute(){
        if(LOG.isDebugEnabled())
            LOG.debug("Searching for commits since " + startDate);
        ArrayList<GitCommit> sourceCommits;
        ArrayList<GitCommit> targetCommits;
        try {
            sourceCommits = process(source, startDate, endDate);
            targetCommits = process(target, searchStartDate, searchEndDate);
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
        sourceCommits.sort(new Comparator<GitCommit>() {
            @Override
            public int compare(GitCommit o1, GitCommit o2) {
                return o2.getCommitDate().compareTo(o1.getCommitDate()); // descending order
            }
        });
        targetCommits.sort(new Comparator<GitCommit>() {
            @Override
            public int compare(GitCommit o1, GitCommit o2) {
                return o2.getCommitDate().compareTo(o1.getCommitDate()); // descending order
            }
        });
        search(sourceCommits, targetCommits);
    }

    public void run(String[] args) {
        parseArgs(args);

        initRepo();

        execute();

        displayResults();
    }

    public static void main(String[] args) {
        LOG.info("Hey, Welcome to the MCL Library!");
        new Main().run(args);
    }
}
