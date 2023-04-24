package org.mcl.model;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.PullResult;
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

    private Git git;
    private Repository repo;
    private final String gitUrl;
    private final String repoPath;
    private final String branchName;
    private final boolean clone;
    private final String repoAlias;
    private static final TextProgressMonitor consoleProgressMonitor = new TextProgressMonitor(new PrintWriter(System.out));
    private static final Logger LOG = LogManager.getLogger(GitRepo.class);

    public GitRepo(String repoPath, String gitUrl, String branchName, String alias, boolean clone){
        this.repoPath   = repoPath;
        this.gitUrl     = gitUrl;
        this.branchName = branchName;
        this.repoAlias  = alias;
        this.clone      = clone;
        init();
    }

    public Git getGit(){ return git; }

    public Repository getRepo() { return repo; }

    public String getRepoAlias() { return repoAlias; }

    public String getRepoPath() { return repoPath; }

    public Repository clone(File path, String uri) throws GitAPIException {
        if(LOG.isDebugEnabled())
            LOG.debug("Cloning repository");
        return Git.cloneRepository()
                .setProgressMonitor(consoleProgressMonitor)
                .setDirectory(path)
                .setURI(uri)
                .call().getRepository();
    }

    public Git checkout(Repository repo, String branchName, String remote) throws GitAPIException {
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

    public void init() {
        File repoPath = new File(this.repoPath);
        try {
            if (this.clone) {
                this.repo = clone(repoPath, this.gitUrl);
            } else {
                this.repo = Git.init().setDirectory(repoPath).call().getRepository();
            }
            this.git = checkout(this.repo, "fresh_" + this.branchName + "_", "origin/" + this.branchName);
        } catch (GitAPIException exp){
            LOG.error("Error checking out repo: " + exp);
        }
    }

    public void pull() throws GitAPIException {
        assert git != null;
        // Pull changes from upstream
        PullResult pullResult = git.pull()
                .setRemote("origin")
                .setRemoteBranchName(branchName)
                .call();
        if(LOG.isDebugEnabled()){
            LOG.debug("Pull result: " + pullResult.getMergeResult().toString());
        }

    }
}
