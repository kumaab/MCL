package org.mcl.model;

import lombok.Data;

@Data
public class File {

    private String fileName;
    private String content;
    private long linesAdded;
    private long linesDeleted;

    public File(String fileName, String content, long linesAdded, long linesDeleted){
        this.fileName = fileName;
        this.content = content;
        this.linesAdded = linesAdded;
        this.linesDeleted = linesDeleted;
    }
}
