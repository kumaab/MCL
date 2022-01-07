

import java.util.HashMap;
import java.util.List;

public class Match {

    private final GitCommit originalCommit;
    private final HashMap<GitCommit, List<Double>> replicatedCommits;
    private GitCommit closestMatch;
    private List<Double> closestRatios;

    Match(GitCommit originalCommit){
        this.originalCommit = originalCommit;
        this.replicatedCommits = new HashMap<>();
    }

    public GitCommit getOriginalCommit() {
        return originalCommit;
    }

    public HashMap<GitCommit, List<Double>> getReplicatedCommits() {
        return replicatedCommits;
    }

    public GitCommit getBestMatch(){
        Double maxSum = 0.0;
        GitCommit bestMatch = null;
        if(!replicatedCommits.isEmpty()){
            for(GitCommit commit: replicatedCommits.keySet()){
                Double sum = replicatedCommits.get(commit).stream().reduce(0.0, Double::sum);
                if (sum >= maxSum){
                    maxSum = sum;
                    bestMatch = commit;
                }
            }
        }
        return bestMatch;
    }

    public GitCommit getClosestMatch(){ return closestMatch; }

    public List<Double> getClosestRatios(){ return closestRatios; }

    public void setClosestMatch(GitCommit gitCommit){ this.closestMatch = gitCommit; }

    public void setClosestRatios(List<Double> ratios){ this.closestRatios = ratios; }

    public int getMatchCount(){ return replicatedCommits.size(); }

    public void setReplicatedCommit(GitCommit replicatedCommit, List<Double> ratios) {
        this.replicatedCommits.put(replicatedCommit, ratios);
    }

    public void found(GitCommit commit, List<Double> ratios){
        setReplicatedCommit(commit, ratios);
    }
}
