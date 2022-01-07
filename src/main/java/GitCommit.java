
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class GitCommit {

    public final String committer;
    public final String commitMessage;
    public final String commitHash;
    public final String author;
    public final LocalDate commitDate;
    public HashSet<File> changeSet;
    public List<String> fileNames;

    public String getCommitter() { return committer; }

    public String getCommitMessage() { return commitMessage; }

    public String getAuthor() { return author; }

    public LocalDate getCommitDate() { return commitDate; }

    public HashSet<File> getChangeSet() { return changeSet; }

    public List<String> getFileNames() { return fileNames; }

    public void setFileMeta(HashSet<File> files){
        changeSet = files;
    }

    public void setFileNames(){
        this.fileNames = new ArrayList<>();
        for(File file: this.changeSet)
            fileNames.add(file.fileName);
    }

    public GitCommit(String committer, String commitMessage, String commitHash, String author, LocalDate commitDate){
        this.committer     = committer;
        this.commitMessage = commitMessage;
        this.commitHash    = commitHash;
        this.author        = author;
        this.commitDate    = commitDate;
    }
}
