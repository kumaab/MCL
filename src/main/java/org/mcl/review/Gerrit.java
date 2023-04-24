package org.mcl.review;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.CherryPickResult;
import org.eclipse.jgit.api.Git;
import org.mcl.model.GitRepo;

import java.io.File;

public class Gerrit {
    private final Logger LOG = LogManager.getLogger(Gerrit.class);
    private GitRepo sourceGitRepo;
    private GitRepo targetGitRepo;
    private String runAsUser;

    Gerrit(GitRepo source, GitRepo target, String user){
        this.sourceGitRepo = source;
        this.targetGitRepo = target;
        this.runAsUser = user;
    }

    void cherryPick(String commitId){
        try {
            // Open source repository
            Git sourceGit = Git.open(new File(sourceGitRepo.getRepoPath()));

            // Set up credentials provider for authentication (if required)
            //UsernamePasswordCredentialsProvider sourceCredentialsProvider = new UsernamePasswordCredentialsProvider(username, password);

            // Fetch changes from source repository
            sourceGit.fetch()
                    .setRemote("origin")
                    //.setCredentialsProvider(sourceCredentialsProvider)
                    .call();

            // Open target repository
            Git targetGit = Git.open(new File(targetGitRepo.getRepoPath()));

            // Set up credentials provider for authentication (if required)
            //UsernamePasswordCredentialsProvider targetCredentialsProvider = new UsernamePasswordCredentialsProvider(username, password);

            // Fetch changes from target repository
            targetGit.fetch()
                    .setRemote("origin")
                    //.setCredentialsProvider(targetCredentialsProvider)
                    .call();

            // Cherry-pick the commit from source repository
            CherryPickResult cherryPickResult = targetGit.cherryPick()
                    .include(sourceGit.getRepository().resolve(commitId))
                    .call();

            // Print cherry-pick result
            LOG.info("Cherry pick result: " + cherryPickResult.getStatus().toString());

            // Close Git repositories
            sourceGit.close();
            targetGit.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




}
