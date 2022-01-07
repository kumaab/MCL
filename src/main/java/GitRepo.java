
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.util.Optional;

public class GitRepo {

    private final Git git;
    private final Repository repo;
    private final String repoAlias;
    private static final TextProgressMonitor consoleProgressMonitor = new TextProgressMonitor(new PrintWriter(System.out));
    private static final Logger LOG = LogManager.getLogger(GitRepo.class);

    GitRepo(){
        this.git = null;
        this.repo = null;
        this.repoAlias = null;
    }

    GitRepo(Git git, Repository repo, String repoAlias){
        this.git = git;
        this.repo = repo;
        this.repoAlias = repoAlias;
    }

    public Git getGit(){ return git; }

    public Repository getRepo() { return repo; }

    public String getRepoAlias() { return repoAlias; }

    public static Repository clone(File path, String uri) throws GitAPIException {
        if(LOG.isDebugEnabled())
            LOG.debug("Cloning repository");
        return Git.cloneRepository()
                .setProgressMonitor(consoleProgressMonitor)
                .setDirectory(path)
                .setURI(uri)
                .call().getRepository();
    }

    public static Git checkout(Repository repo, String branchName, String remote) throws GitAPIException {
        Git git = new Git(repo);
        Optional<String> developBranch = git.branchList()
                .setListMode(ListBranchCommand.ListMode.REMOTE)
                .call()
                .stream()
                .map(Ref::getName)
                .filter(n -> n.contains(remote)).findAny();

        if (developBranch.isPresent()) {
            if(LOG.isDebugEnabled())
                LOG.debug("Checking out " + remote);
            git.checkout()
                    .setProgressMonitor(consoleProgressMonitor)
                    .setCreateBranch(true)
                    .setName(branchName + LocalTime.now().toNanoOfDay())
                    .setStartPoint(developBranch.get())
                    .call();
        }
        return git;
    }

    public GitRepo init(String homeDir, String uri, String branch, String alias, boolean clone) throws GitAPIException {
        File repoPath = new File(homeDir);
        Repository repo;
        if (clone) {
            repo = clone(repoPath, uri);
        } else {
            repo = Git.init().setDirectory(repoPath).call().getRepository();
        }
        Git git = checkout(repo, "fresh_" + branch + "_", "origin/" + branch);
        return new GitRepo(git, repo, alias);
    }
}
