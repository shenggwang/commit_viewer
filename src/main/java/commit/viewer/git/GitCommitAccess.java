package commit.viewer.git;

import commit.viewer.model.CommitModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Singleton class to retrieve Commits from remote git repository.
 *
 * @author Sheng Wang (shenggwangg@gmail.com)
 */
public enum GitCommitAccess {

    INSTANCE;

    private static final Logger logger = LoggerFactory.getLogger(GitCommitAccess.class);

    final HttpClient client = HttpClient.newHttpClient();

    private String url;

    GitCommitAccess() {
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    /**
     * Request commits {@code url}.
     *
     * @return {@link List} of {@link CommitModel}.
     */
    public List<CommitModel> getCommitsByURL() throws IOException, URISyntaxException, InterruptedException, ParseException {
        logger.debug("Request: {}", url);
        return getCommitsByUrl(new URL(url));
    }

    /**
     * Request commits by given {@code ownerName} and {@code repositoryName}.
     * Note that it only retrieves the latest 30 commits.
     *
     * @return {@link List} of {@link CommitModel}.
     */
    public List<CommitModel> getCommits(final String ownerName, final String repositoryName) throws IOException, URISyntaxException, InterruptedException, ParseException {
        final URL url = new URL(
                String.format(
                        "https://api.github.com/repos/%s/%s/commits",
                        ownerName,
                        repositoryName
                )
        );
        logger.debug("Request: {}", url.getPath());
        return getCommitsByUrl(url);
    }

    /**
     * Similar to {@link #getCommits}, but retrieve commits by given page.
     *
     * @return {@link List} of {@link CommitModel}.
     */
    public List<CommitModel> getCommitsByPage(final String ownerName, final String repositoryName, final int pageNumber) throws IOException, URISyntaxException, InterruptedException, ParseException {
        final URL url = new URL(
                String.format(
                        "https://api.github.com/repos/%s/%s/commits?page=%d",
                        ownerName,
                        repositoryName,
                        pageNumber
                        )
                );
        logger.debug("Request: {}", url.getPath());
        return getCommitsByUrl(url);
    }

    private List<CommitModel> getCommitsByUrl(final URL url) throws IOException, InterruptedException, URISyntaxException, ParseException {

        final List<CommitModel> list = new LinkedList<>();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(url.toURI())
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());
        JSONParser parser = new JSONParser();

        logger.debug("Parsing the message body: {}", response.body());
        JSONArray array = (JSONArray) parser.parse(response.body());

        for (final Object object: array){
            final JSONObject obj = (JSONObject) object;
            logger.debug("Creating for object: {}", obj.toJSONString());

            final CommitModel.Builder builder = new CommitModel.Builder();
            builder.sha((String) obj.getOrDefault("sha", "n/a"));

            final JSONObject commit = (JSONObject) obj.get("commit");
            builder.message((String) commit.getOrDefault("message", "n/a"));
            final JSONObject committer = (JSONObject) commit.get("committer");

            builder.author((String) committer.getOrDefault("name", "n/a"));
            builder.date((String) committer.getOrDefault("date", "n/a"));

            list.add(builder.build());
        }

        return list;
    }
}
