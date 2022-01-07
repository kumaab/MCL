
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import org.apache.commons.text.similarity.CosineSimilarity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

public class Utils {
    private static ByteArrayOutputStream out;
    private static DiffFormatter diffFormatter;
    private static final int PAD_LENGTH = 12;
    private static final Logger LOG     = LogManager.getLogger(Rules.class);

    public static void initFormatter(Repository repo){
        out           = new ByteArrayOutputStream();
        diffFormatter = new DiffFormatter(out);
        diffFormatter.setRepository(repo);
        diffFormatter.setContext(0);
    }

    public static HashSet<File> parse(RevCommit commit){
        HashSet<File> files = new HashSet<>();
        try {
            List<DiffEntry> entries = diffFormatter.scan(commit.getParent(0).getId(), commit.getId());
            for(DiffEntry entry : entries){
                diffFormatter.format(entry);
                int added = 0, deleted = 0;
                Scanner scanner       = new Scanner(out.toString());
                StringBuilder content = new StringBuilder();
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.startsWith("+") && !line.startsWith("+++")) {
                        added++;
                        content.append(line).append("\n");
                    } else if (line.startsWith("-") && !line.startsWith("---")) {
                        deleted++;
                        content.append(line).append("\n");
                    }
                }
                String path;
                if(entry.getChangeType().equals(DiffEntry.ChangeType.DELETE))
                    path = entry.getOldPath();
                else
                    path = entry.getNewPath();
                files.add(new File(path, content.toString(), added, deleted));
                out.reset();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error occurred parsing commits from git log. Message: " + e.getMessage());
        }
        return files;
    }

    public static Double similarity(String s1, String s2){
        Map<CharSequence, Integer> vector1 = new HashMap<>();
        Map<CharSequence, Integer> vector2 = new HashMap<>();

        for (String token : s1.split(" ")) {
            vector1.put(token, vector1.getOrDefault(token, 0) + 1);
        }

        for (String token : s2.split(" ")) {
            vector2.put(token, vector2.getOrDefault(token, 0) + 1);
        }
        CosineSimilarity cosine = new CosineSimilarity();
        return cosine.cosineSimilarity(vector1, vector2);
    }

    public static String appendSpaces(String message, int maxLength){
        StringBuilder result = new StringBuilder();
        int length = message.length();
        if (length > maxLength){
            return message.substring(0, maxLength);
        } else {
            for(int i=length;i<maxLength;i++)
                result.append(" ");
        }
        return message + result.toString();
    }

    public static String padding(){
        StringBuilder pad = new StringBuilder();
        for(int i=0;i<PAD_LENGTH;i++){
            pad.append(" ");
        }
        return pad.toString();
    }
}
