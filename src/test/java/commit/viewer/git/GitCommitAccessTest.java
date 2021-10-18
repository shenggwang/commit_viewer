package commit.viewer.git;

import commit.viewer.model.CommitModel;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GitHub API commit access tests. Note that the tests uses real Github API, which might lead to
 * exceed <i>API rate limit</i>, hence all tests start failing.
 *
 * @author Sheng Wang (shenggwangg@gmail.com)
 */
public class GitCommitAccessTest {

    /**
     * Before class that starts the singleton {@link GitCommitAccess#INSTANCE} with expected project.
     *
     * @throws IOException          If http request fails sending data.
     * @throws InterruptedException If http request is interrupted.
     * @throws URISyntaxException   If URL is invalid.
     * @throws ParseException       If message body is not a {@link JSONArray}.
     */
    @BeforeClass
    public static void beforeClass() throws IOException, URISyntaxException, ParseException, InterruptedException {
        final String owner = "apache";
        final String repository = "spark";
        GitCommitAccess.INSTANCE.startProject(owner, repository);
    }

    /**
     * Ensures that request commits works.
     *
     * @throws IOException          If http request fails sending data.
     * @throws InterruptedException If http request is interrupted.
     * @throws URISyntaxException   If URL is invalid.
     * @throws ParseException       If message body is not a {@link JSONArray}.
     */
    @Test
    public void ensureRequestWorks() throws IOException, URISyntaxException, InterruptedException, ParseException {

        final List<CommitModel> commits1 = GitCommitAccess.INSTANCE.getCommits();
        assertThat(commits1)
                .as("First get commits must be able to retrieve 30 commits.")
                .hasSize(30);

        final List<CommitModel> commits2 = GitCommitAccess.INSTANCE.getCommits();
        assertThat(commits2)
                .as("Second get commits must be able to retrieve 30 commits.")
                .hasSize(30);

        assertThat(commits1)
                .as("Both request should have the same result.")
                .containsSequence(commits2);
    }

    /**
     * Ensures that pagination works.
     *
     * @throws IOException          If http request fails sending data.
     * @throws InterruptedException If http request is interrupted.
     * @throws URISyntaxException   If URL is invalid.
     * @throws ParseException       If message body is not a {@link JSONArray}.
     */
    @Test
    public void ensureRequestWithPageWorks() throws URISyntaxException, ParseException, InterruptedException, IOException {
        final List<CommitModel> commitsWithPage1 = GitCommitAccess.INSTANCE.getCommits(2, 5);
        final List<CommitModel> commitsWithPage2 = GitCommitAccess.INSTANCE.getCommits(3, 5);
        assertThat(commitsWithPage1)
                .as("Request page 1 should be different from page 2.")
                .doesNotContainSequence(commitsWithPage2);
        final List<CommitModel> commitsWithPage3 = GitCommitAccess.INSTANCE.getCommits(2, 5);
        assertThat(commitsWithPage1)
                .as("Request page 1 should be equal to page 3.")
                .containsSequence(commitsWithPage3);
    }

    /**
     * Ensures that cache updates correctly with new commits.
     *
     * @throws IOException          If http request fails sending data.
     * @throws InterruptedException If http request is interrupted.
     * @throws URISyntaxException   If URL is invalid.
     * @throws ParseException       If message body is not a {@link JSONArray}.
     */
    @Test
    public void ensureUpdateCacheWorks() throws URISyntaxException, ParseException, InterruptedException, IOException {
        final List<CommitModel> commitsWithPage1 = GitCommitAccess.INSTANCE.getCommits();

        final CommitModel newestCommit = commitsWithPage1.get(0);

        // Removes locally the first/newest commit of the given branch.
        GitCommitAccess.INSTANCE.cache.get(GitCommitAccess.INSTANCE.branch).removeFirst();

        assertThat(newestCommit)
                .as("The newest commit is removed.")
                .isNotEqualTo(GitCommitAccess.INSTANCE.cache.get(GitCommitAccess.INSTANCE.branch).getFirst());

        final List<CommitModel> commitsWithPage2 = GitCommitAccess.INSTANCE.getCommits();
        assertThat(commitsWithPage2.get(0))
                .as("Update cache should fetch the newest commit.")
                .isEqualTo(newestCommit);
    }


    /**
     * Ensures that when asking for oldest commits works.
     *
     * @throws IOException          If http request fails sending data.
     * @throws InterruptedException If http request is interrupted.
     * @throws URISyntaxException   If URL is invalid.
     * @throws ParseException       If message body is not a {@link JSONArray}.
     */
    @Test
    public void ensureFetchOldestCommitsWorks() throws URISyntaxException, ParseException, InterruptedException, IOException {
        final List<CommitModel> commits1 = GitCommitAccess.INSTANCE.getCommits(1, 37);
        assertThat(commits1)
                .as("Request 1 should have 37 commits.")
                .hasSize(37);
        assertThat(GitCommitAccess.INSTANCE.cache.get(GitCommitAccess.INSTANCE.branch))
                .as("Cache of current branch should have 60 commits.")
                .hasSize(60);

        // Removes 3 oldest commits of the commits of current branch on cache.
        GitCommitAccess.INSTANCE.cache.get(GitCommitAccess.INSTANCE.branch).removeLast();
        GitCommitAccess.INSTANCE.cache.get(GitCommitAccess.INSTANCE.branch).removeLast();
        GitCommitAccess.INSTANCE.cache.get(GitCommitAccess.INSTANCE.branch).removeLast();

        assertThat(GitCommitAccess.INSTANCE.cache.get(GitCommitAccess.INSTANCE.branch))
                .as("Cache of current branch should have 57 commits.")
                .hasSize(57);

        final List<CommitModel> commits2 = GitCommitAccess.INSTANCE.getCommits(1, 37);
        assertThat(commits2)
                .as("Request 2 should have 37 commits.")
                .hasSize(37);

        assertThat(GitCommitAccess.INSTANCE.cache.get(GitCommitAccess.INSTANCE.branch))
                .as("Cache of current branch should have 57 commits.")
                .hasSize(57);

        final List<CommitModel> commits3 = GitCommitAccess.INSTANCE.getCommits(1, 58);

        assertThat(commits3)
                .as("Request 3 should have 58 commits.")
                .hasSize(58);
        assertThat(GitCommitAccess.INSTANCE.cache.get(GitCommitAccess.INSTANCE.branch))
                .as("Cache of current branch should have 60 commits.")
                .hasSize(60);

        // Removes 3 oldest commits of the commits of current branch on cache.
        GitCommitAccess.INSTANCE.cache.get(GitCommitAccess.INSTANCE.branch).removeLast();
        GitCommitAccess.INSTANCE.cache.get(GitCommitAccess.INSTANCE.branch).removeLast();
        GitCommitAccess.INSTANCE.cache.get(GitCommitAccess.INSTANCE.branch).removeLast();

        final List<CommitModel> commits4 = GitCommitAccess.INSTANCE.getCommits(1, 61);

        assertThat(commits4)
                .as("Request 4 should have 61 commits.")
                .hasSize(61);
        assertThat(GitCommitAccess.INSTANCE.cache.get(GitCommitAccess.INSTANCE.branch))
                .as("Cache of current branch should have 90 commits.")
                .hasSize(90);
    }

    /**
     * Ensures that regex expression works as expected.
     */
    @Test
    public void ensureUrlRegexWorks() {

        assertCorrect("https://github.com/shenggwang/commit_viewer.git");
        assertCorrect("http://github.com/shenggwang/commit_viewer.git");

        assertIncorrect("http://github/shenggwang/commit_viewer.git");
        assertIncorrect("http://api.github.com/shenggwang/commit_viewer.git");
        assertIncorrect("http://github.com/shenggwang/commit_viewer");
    }

    private void assertCorrect(final String url) {
        final Matcher matcher = GitCommitAccess.regex.matcher(url);
        if (matcher.find()) {
            final String owner = matcher.group(1);
            final String repository = matcher.group(2);
            assertThat(owner)
                    .as("The owner should be.")
                    .isEqualTo("shenggwang");
            assertThat(repository)
                    .as("The repository should be.")
                    .isEqualTo("commit_viewer");
        }
    }

    private void assertIncorrect(final String url) {
        final Matcher matcher = GitCommitAccess.regex.matcher(url);
        assertThat(matcher.matches())
            .as("Should not match")
            .isFalse();
    }
}
