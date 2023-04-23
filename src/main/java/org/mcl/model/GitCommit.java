package org.mcl.model;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Data
public class GitCommit {

    private final String committer;
    private final String commitMessage;
    private final String commitHash;
    private final String author;
    private final LocalDate commitDate;
    private HashSet<File> changeSet;
    private List<String> fileNames;

    public HashSet<File> getChangeSet() { return changeSet; }

    public List<String> getFileNames() { return fileNames; }

    public void setFileMeta(HashSet<File> files){
        changeSet = files;
    }

    public void setFileNames(){
        this.fileNames = new ArrayList<>();
        for(File file: this.changeSet)
            fileNames.add(file.getFileName());
    }

    public GitCommit(String committer, String commitMessage, String commitHash, String author, LocalDate commitDate){
        this.committer     = committer;
        this.commitMessage = commitMessage;
        this.commitHash    = commitHash;
        this.author        = author;
        this.commitDate    = commitDate;
    }
}
