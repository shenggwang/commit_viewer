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
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

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

    private List<CommitModel> cache;

    GitCommitAccess() {
    }

    public void setUrl(final String url) {
        if (!this.url.equals(url)) {
            logger.debug("Updated URL to {}.", url);
            cache = new LinkedList<>();
            this.url = url;
        }
        logger.debug("URL is already set with same parameter.");
    }

    public String getUrl() {
        return this.url;
    }

    /**
     * Request commits {@code url}.
     *
     * @return {@link List} of {@link CommitModel}.
     */
    public List<CommitModel> getCommitsByURL() throws IOException, URISyntaxException, InterruptedException, ParseException {
        logger.debug("Request: {}", this.url);
        return getCommitsByUrl(new URL(this.url));
    }

    /**
     * Request commits by given {@code ownerName} and {@code repositoryName}.
     * Note that it only retrieves the latest 30 commits.
     *
     * @return {@link List} of {@link CommitModel}.
     */
    public List<CommitModel> getCommits(final String ownerName, final String repositoryName)
            throws IOException, URISyntaxException, InterruptedException, ParseException {
        setUrl(String.format(
                "https://api.github.com/repos/%s/%s/commits",
                ownerName,
                repositoryName
        ));
        logger.debug("Request: {}", this.url);
        return getCommitsByUrl(new URL(this.url));
    }

    /**
     * Similar to {@link #getCommits}, but retrieve commits by given page.
     *
     * @return {@link List} of {@link CommitModel}.
     */
    public List<CommitModel> getCommitsByPage(final String ownerName, final String repositoryName, final int pageNumber)
            throws IOException, URISyntaxException, InterruptedException, ParseException {
        setUrl(String.format(
                "https://api.github.com/repos/%s/%s/commits?page=%d",
                ownerName,
                repositoryName,
                pageNumber
        ));
        logger.debug("Request: {}", this.url);
        return getCommitsByUrl(new URL(this.url));
    }

    public List<CommitModel> getCommitsFromAPI(final String ownerName, final String repositoryName, final int page, final int size)
            throws IOException, InterruptedException, URISyntaxException, ParseException {

        setUrl(String.format(
                "https://api.github.com/repos/%s/%s/commits",
                ownerName,
                repositoryName
        ));

        updateCache();

        final int untilCommit = (page * size) + size;
        if (cache.size() < untilCommit) {
            fetchOldestCommits(untilCommit);
        }
        return cache.subList(untilCommit - size, untilCommit);
    }

    private void updateCache() throws URISyntaxException, ParseException, InterruptedException, IOException {
        if (cache.size() == 0) {
            logger.debug("There is no cache to update");
            return;
        }
        final List<CommitModel> commits = getCommitsByURL();

        int i = 0;
        // I'm avoiding for (int i = 0) to avoid list.get(i), because the get is expansive on linked list.
        for (final CommitModel commit : commits) {
            if (commit.equals(cache.get(0))) {
                if (i == 0) {
                    logger.debug("Cache is updated.");
                    return;
                }
                break;
            }
            i++;
        }

        if (i == 30) {
            logger.debug("Cache is too old, let's remove cache and start again");
            cache = new LinkedList<>();
            return;
        }
        cache = Stream.concat(commits.subList(0, i-1).stream(), cache.stream()).collect(toList());
    }

    private void fetchOldestCommits(int untilCommit) throws InterruptedException, URISyntaxException, ParseException, IOException {
        final int commitsFetched = cache.size();
        final int commitsToBeFetched = untilCommit - commitsFetched;

        final int currentPage = (int) Math.ceil(commitsFetched / 30);
        final int pageAway = (int) Math.ceil(commitsToBeFetched / 30);

        // n elements that already exists on the cache, e.g., there are 37 on cache, 7 elements should be skipped.
        int nElements = commitsFetched % 30;

        for (int page = currentPage + 1; page <= pageAway; page++) {
            final URL url = new URL(
                    String.format(
                            "%s?page=%d",
                            this.url,
                            page
                    )
            );
            final List<CommitModel> commits = getCommitsByUrl(url);
            if (commits.size() == 0) {
                logger.debug("No more commits for page {}", page);
                return;
            }

            for (final CommitModel commit: commits) {
                while (nElements != 0) {
                    nElements--;
                    continue;
                }
                cache.add(commit);
            }
        }
    }

    /**
     * Retrieves {@link List} of {@link CommitModel}. The list will be 30 Commits, unless there is less than 30 commits
     * or the given page has less than 30 commits which is the last page.
     *
     * @return {@link List} of {@link CommitModel}.
     * @throws IOException          If http request fails sending data.
     * @throws InterruptedException If http request is interrupted.
     * @throws URISyntaxException   If URL is invalid.
     * @throws ParseException       If message body is not a {@link JSONArray}.
     */
    private List<CommitModel> getCommitsByUrl(final URL url)
            throws IOException, InterruptedException, URISyntaxException, ParseException {

        final List<CommitModel> list = new LinkedList<>();

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(url.toURI())
                .build();

        final HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());
        final JSONParser parser = new JSONParser();

        logger.debug("Parsing the message body: {}", response.body());
        final JSONArray array = (JSONArray) parser.parse(response.body());

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
