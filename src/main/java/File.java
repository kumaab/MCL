
public class File {

    public final String fileName;
    public final String content;
    public final long linesAdded;
    public final long linesDeleted;

    public String getFileName() {
        return fileName;
    }

    public String getContent() {
        return content;
    }

    public long getLinesAdded() {
        return linesAdded;
    }

    public long getLinesDeleted() {
        return linesDeleted;
    }

    File(String fileName, String content, long linesAdded, long linesDeleted){
        this.fileName = fileName;
        this.content = content;
        this.linesAdded = linesAdded;
        this.linesDeleted = linesDeleted;
    }
}
