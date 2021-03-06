package utils;

/**
 * @author Keesun Baik
 */
public class PullRequestCommit {

    private String projectOwner;

    private String projectName;

    private Long pullRequestId;

    private String commitId;

    public PullRequestCommit(String url) {
        String[] parts = url.split("/");
        this.projectOwner = parts[3];
        this.projectName = parts[4];
        this.pullRequestId = Long.parseLong(parts[6]);
        this.commitId = parts[8];
    }

    public static boolean isValid(String url) {
        if(url == null || url.trim().isEmpty()) {
            return false;
        }
        return url.matches(".*\\/pullRequest\\/[0-9]*\\/commit\\/.*");
    }

    public String getProjectOwner() {
        return projectOwner;
    }

    public String getProjectName() {
        return projectName;
    }

    public Long getPullRequestId() {
        return pullRequestId;
    }

    public String getCommitId() {
        return commitId;
    }
}
