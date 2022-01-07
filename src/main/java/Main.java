

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static java.time.temporal.ChronoUnit.DAYS;

public class Main {
    private static GitRepo first;
    private static GitRepo second;

    private static final String FIRST_NAME    = "first.name";
    private static final String FIRST_DIR     = "first.dir";
    private static final String FIRST_URI     = "first.uri";
    private static final String FIRST_BRANCH  = "first.branch";

    private static final String SECOND_NAME   = "second.name";
    private static final String SECOND_DIR    = "second.dir";
    private static final String SECOND_URI    = "second.uri";
    private static final String SECOND_BRANCH = "second.branch";

    private static final String EXCLUDED_MODS           = "excluded.mods";
    private static final String REVERSE_EXCLUDED_MODS   = "reverse.excluded.mods";
    private static final Logger LOG                     = LogManager.getLogger(Main.class);

    public static LocalDate endDate         = LocalDate.now(); //LocalDate.of(2021, 10, 8);
    public static LocalDate searchEndDate   = endDate.plus(1, ChronoUnit.MONTHS);

    private static int matches = 0, commits = 0, nMonths = 12;
    private static List<String> excludedMods        = Collections.emptyList();
    private static List<String> reverseExcludedMods = Collections.emptyList();
    private static Properties props = null;
    private static final List<Match> resultSet = new ArrayList<>();


    public static void init() throws GitAPIException {
        boolean cloneFirst = false, cloneSecond = false;
        String firstRepo  = props.getProperty(FIRST_URI);
        String secondRepo = props.getProperty(SECOND_URI);
        if ( firstRepo.equals(secondRepo)){
            LOG.info("Initiating branch comparison  ..");
        } else {
            LOG.info("Initiating repo comparison ..");
        }
        if(!Config.isDir(props.getProperty(FIRST_DIR)))
            cloneFirst = true;
        if(!Config.isDir(props.getProperty(SECOND_DIR)))
            cloneSecond = true;

        if (props.getProperty(EXCLUDED_MODS) != null)
            excludedMods        = Arrays.asList(props.getProperty(EXCLUDED_MODS).split(","));
        if (props.getProperty(REVERSE_EXCLUDED_MODS) != null)
            reverseExcludedMods = Arrays.asList(props.getProperty(REVERSE_EXCLUDED_MODS).split(","));
        first  = new GitRepo().init(props.getProperty(FIRST_DIR),firstRepo,
                props.getProperty(FIRST_BRANCH), props.getProperty(FIRST_NAME), cloneFirst);
        second = new GitRepo().init(props.getProperty(SECOND_DIR),secondRepo,
                props.getProperty(SECOND_BRANCH), props.getProperty(SECOND_NAME), cloneSecond);
    }

    public static ArrayList<GitCommit> process(GitRepo gitRepo, LocalDate start, LocalDate end) throws GitAPIException {
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
                if (gitCommit.commitMessage.startsWith("Merge")){ // exclude merge commits
                    mergedCommits += 1;
                    continue;
                }
                gitCommit.setFileMeta(Utils.parse(curCommit));
                gitCommit.setFileNames();
                result.add(gitCommit);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.info(gitRepo.getRepoAlias() + " Total Commits : " + totalCommits);
            LOG.info(gitRepo.getRepoAlias() + " Merge Commits : " + mergedCommits + " (Excluded)!");
        }
        return result;
    }

    public static void search(ArrayList<GitCommit> originalCommits, ArrayList<GitCommit> replicatedCommits) {
        for(GitCommit originalCommit: originalCommits){
            if (Rules.isSystemUser(originalCommit))    continue;
            double maxCosine = 0.0;
            ArrayList<Double> closestRatios = new ArrayList<>();
            GitCommit closest = null;
            GitCommit originalFCommit = Rules.filter(originalCommit, reverseExcludedMods);

            Match match = new Match(originalFCommit);
            if (originalFCommit.getFileNames().size() > 0) {
                for (GitCommit replicatedCommit : replicatedCommits) {
                    if (Rules.isSystemUser(replicatedCommit))    continue;

                    long daysBetween           = DAYS.between(originalFCommit.commitDate, replicatedCommit.commitDate);
                    GitCommit replicatedFCommit = Rules.filter(replicatedCommit, excludedMods);
                    ArrayList<Double> ratios   = Rules.compareChangeSet(originalFCommit, replicatedFCommit);
                    boolean equalSets          = Rules.equal(originalFCommit.getFileNames(), replicatedFCommit.getFileNames());

                    if(Rules.singletonMismatch(originalFCommit, replicatedFCommit) || !Rules.IQR(originalFCommit, replicatedFCommit)){
                        continue;
                    }

                    Double sumCosine = ratios.stream().reduce(0.0, Double::sum);
                    if (sumCosine > maxCosine) {
                        closestRatios = ratios;
                        maxCosine = sumCosine;
                        closest = replicatedFCommit;
                    }

                    if (ratios.size() == 1 && equalSets && Rules.weakThreshold(ratios)){
                        match.found(replicatedFCommit, ratios);
                    } else if (equalSets && Rules.product(ratios) && ratios.size() > 0) {
                        match.found(replicatedFCommit, ratios);
                    } else if (Rules.allButOne(ratios)){
                        match.found(replicatedFCommit, ratios);
                    } else if (Rules.majority(ratios) && Rules.commitMsgCheck(originalFCommit.commitMessage, replicatedFCommit.commitMessage)){
                        match.found(replicatedFCommit, ratios);
                    } else if (Rules.subset(originalFCommit, replicatedFCommit)){
                        match.found(replicatedFCommit, ratios);
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

    public static void main(String[] args) throws GitAPIException {
        LOG.info("Hey, Welcome to the MCL Library!");
        props = Config.readProperties("src/main/resources/config.properties");
        String author = null, committer = null;
        for(int i=0; i<args.length; i++){
            String arg = args[i];
            if(arg.equals("-a") || arg.equals("--author"))
                author = args[i+1];
            if(arg.equals("-c") || arg.equals("--committer"))
                committer = args[i+1];
            if(arg.equals("-m") || arg.equals("--months"))
                nMonths = Integer.parseInt(args[i+1]);
        }
        init();
        LocalDate startDate       = endDate.minus(nMonths, ChronoUnit.MONTHS);
        LocalDate searchStartDate = startDate;
        if(LOG.isDebugEnabled())
            LOG.debug("The Start Date is " + startDate);
        ArrayList<GitCommit> originalCommits   = process(first, startDate, endDate);
        ArrayList<GitCommit> replicatedCommits = process(second, searchStartDate, searchEndDate);
        originalCommits.sort(new Comparator<GitCommit>() {
            @Override
            public int compare(GitCommit o1, GitCommit o2) {
                return o2.commitDate.compareTo(o1.commitDate); // descending order
            }
        });
        replicatedCommits.sort(new Comparator<GitCommit>() {
            @Override
            public int compare(GitCommit o1, GitCommit o2) {
                return o2.commitDate.compareTo(o1.commitDate); // descending order
            }
        });
        search(originalCommits, replicatedCommits);
        Query query = new Query(resultSet);
        if (author != null)
            query.printResultForAuthor(author);
        else if (committer != null)
            query.printResultForCommitter(committer);
        else {
            query.printAllResults();
        }
    }
}
