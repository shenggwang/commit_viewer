package commit.viewer.git;

import commit.viewer.model.CommitModel;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GitCommitAccessTest {

    @Test
    public void ensureRequestWorks() throws IOException, URISyntaxException, InterruptedException, ParseException {

        final String ownerName = "apache";
        final String repositoryName = "spark";

        GitCommitAccess.INSTANCE.setUrl(String.format(
                "https://api.github.com/repos/%s/%s/commits",
                ownerName,
                repositoryName
        ));

        final List<CommitModel> commits1 = GitCommitAccess.INSTANCE.getCommitsByURL();
        assertThat(commits1)
                .as("Must be able to retrieve 30 commits.")
                .hasSize(30);

        final List<CommitModel> commits2 = GitCommitAccess.INSTANCE.getCommits(ownerName, repositoryName);
        assertThat(commits2)
                .as("Must be able to retrieve 30 commits.")
                .hasSize(30);

        assertThat(commits1)
                .as("Both request should have the same result.")
                .doesNotContainSequence(commits2);
    }

    @Test
    public void ensureRequestWithPageWorkds() throws URISyntaxException, ParseException, InterruptedException, IOException {
        final List<CommitModel> commitsWithPage1 = GitCommitAccess.INSTANCE.getCommitsByPage("apache", "spark", 1);
        final List<CommitModel> commitsWithPage2 = GitCommitAccess.INSTANCE.getCommitsByPage("apache", "spark", 1);
        assertThat(commitsWithPage1)
                .as("Request page 1 should be different from page 2.")
                .doesNotContainSequence(commitsWithPage2);
    }
}
