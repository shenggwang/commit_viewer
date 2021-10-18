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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

/**
 * Singleton class to retrieve Commits from remote git repository.
 *
 * @author Sheng Wang (shenggwangg@gmail.com)
 */
public enum GitCommitAccess {

    INSTANCE;

    private static final Logger logger = LoggerFactory.getLogger(GitCommitAccess.class);

    final static Pattern regex = Pattern.compile("^http[s]?://github\\.com/([A-Za-z0-9]\\w+)/([A-Za-z0-9]\\w+).git$");

    /**
     * The HTTP client, used to make request to Github API.
     */
    final HttpClient client = HttpClient.newHttpClient();

    /**
     * The project base URL.
     */
    private final static String BASE_URL = "https://api.github.com/repos";

    /**
     * The project url, i.e., it contains owner name and repository name.
     */
    private String projectUrl;
    /**
     * The current checked out branch.
     */
    String branch;

    /**
     * The cache that maps branch with corresponded list of commit.
     */
    HashMap<String, LinkedList<CommitModel>> cache;

    GitCommitAccess() {
    }

    public boolean startProjectByURL(final String url) {

        final Matcher matcher = regex.matcher(url);
        if (matcher.find()) {
            final String owner = matcher.group(1);
            final String repository = matcher.group(2);
            try {
                startProject(owner, repository);
                return true;
            } catch (final Exception e) {
                logger.debug("Fail to start project.", e);
            }
        }
        return false;
    }

    public void startProject(final String owner, final String repository)
            throws IOException, URISyntaxException, ParseException, InterruptedException {

        final String projectUrl = format("%s/%s/%s", BASE_URL, owner, repository);
        if (isProjectStarted() && this.projectUrl.equals(projectUrl)) {
            logger.debug("The current project is already owner {} with repository {}.", owner, repository);
            return;
        }

        final JSONObject object = getJsonObjectByUrl(new URL(projectUrl));
        if (String.valueOf(object.getOrDefault("name", "null")).equals(repository)) {
            this.projectUrl = projectUrl;
            this.cache = new HashMap<>();
            logger.debug("Cloned to owner {} and repository {} project.", owner, repository);
            final String default_branch = String.valueOf(object.get("default_branch"));
            if (default_branch != null) {
                setBranch(default_branch);
                this.cache.put(default_branch, new LinkedList<>());
            }
            return;
        }

        logger.debug("The given owner {} and repository {} does not exist.", owner, repository);
    }

    public boolean setBranch(final String branch) {
        if (!isProjectStarted()) {
            logger.debug("There is no project url set yet.");
            return false;
        }

        try {
            final URL url = new URL(format(
                    "%s/branches/%s",
                    this.projectUrl,
                    branch
            ));
            final JSONObject object = getJsonObjectByUrl(url);
            if (String.valueOf(object.getOrDefault("name", "null")).equals(branch)) {
                logger.debug("Checkout to branch {}.", branch);
                this.branch = branch;
                if (!this.cache.containsKey(branch)) {
                    this.cache.put(branch, new LinkedList<>());
                }
                return true;
            }

            logger.debug("The given branch {} does not exists.", branch);
            return false;
        } catch (final Exception e) {
            logger.debug("Fail to set branch.", e);
            return false;
        }
    }

    public boolean isProjectStarted() {
        return this.projectUrl != null;
    }

    public String getCurrentBranch() {
        return this.branch;
    }

    public Set<String> listBranches() {
        return this.cache.keySet();
    }

    public List<CommitModel> getCommits()
            throws IOException, InterruptedException, URISyntaxException, ParseException {

        final long startTime = System.nanoTime();

        // default page and size.
        final List<CommitModel> commits = getCommits(1, 30);

        final long endTime = System.nanoTime();
        final long durationInMillisecond = (endTime - startTime) / 1000000;
        logger.debug("Retrieved commits within {}ms.", durationInMillisecond);

        return commits;
    }

    public List<CommitModel> getCommits(final int page, final int size)
            throws IOException, InterruptedException, URISyntaxException, ParseException {

        if (!isProjectStarted()) {
            logger.debug("There is no project url set yet.");
            return emptyList();
        }

        // TODO this is used when executing git fetch, we should add git fetch commands to update cache.
        updateCache();

        final int untilCommit = page * size;
        if (this.cache.get(this.branch).size() < untilCommit) {
            fetchOldestCommits(untilCommit);
        }
        return this.cache.get(this.branch).subList(untilCommit - size, untilCommit);
    }

    /**
     * Fetches the newest commits.
     * The strategy used here is the following:
     * <ol>
     *     <li>Retrieve the first/newest 30 commits in a list.</li>
     *     <li>Iterate the list of commit and see if there is any commit that equals to the first commit on the cache.</li>
     *     <li>Count number of commit that cache doesn't contain and add it at the beginning of the cache list</li>
     * </ol>
     * Note, if none of 30 commits already exists in cache, we are assuming the cache is way too old, so we just delete
     * the cache for the current branch and start over.
     *
     * @throws IOException          If http request fails sending data.
     * @throws InterruptedException If http request is interrupted.
     * @throws URISyntaxException   If URL is invalid.
     * @throws ParseException       If message body is not a {@link JSONArray}.
     */
    private void updateCache() throws URISyntaxException, ParseException, InterruptedException, IOException {
        if (this.cache.get(this.branch).size() == 0) {
            logger.debug("There is no cache to update");
            return;
        }
        final URL url = new URL(format(
                "%s/commits?sha=%s",
                this.projectUrl,
                this.branch
        ));

        final List<CommitModel> commits = getCommitsByUrl(url);

        int i = 0;
        // I'm avoiding for (int i = 0) to avoid list.get(i), because the get is expansive on linked list.
        for (final CommitModel commit : commits) {
            if (commit.equals(this.cache.get(this.branch).get(0))) {
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
            this.cache.put(this.branch, new LinkedList<>());
            return;
        }

        this.cache.get(this.branch).addAll(0, commits.subList(0, i));
    }

    /**
     * Fetches the older commits until the given number of commit.
     * The strategy used here is the following:
     * <ol>
     *     <li>Retrieve the size of updated list of commits of the current branch.</li>
     *     <li>Each page has 30 commits (except the last one that can be less), we can calculate in which page we are.</li>
     *     <li>Subtract the page on the number of commits we want to get by the page of number of commit we already have
     *     on the cache, we get the number of page of commit we want to fetch.</li>
     *     <li>After that, we calculate the number of page we have to fetch to get the number of commits that we want.</li>
     *     <li>Then, by knowing with page we have fetched and until which page we want to fetch, we have the list we wanted.</li>
     * </ol>
     *
     * @param untilCommit The number of commit that we are looking for.
     * @throws IOException          If http request fails sending data.
     * @throws InterruptedException If http request is interrupted.
     * @throws URISyntaxException   If URL is invalid.
     * @throws ParseException       If message body is not a {@link JSONArray}.
     */
    private void fetchOldestCommits(final int untilCommit)
            throws InterruptedException, URISyntaxException, ParseException, IOException {
        final int commitsFetched = this.cache.get(this.branch).size();
        final int currentPage = (int) Math.ceil((float) commitsFetched / 30);

        final int pageAway = (int) Math.ceil((float) untilCommit / 30) - (int) Math.ceil((float) commitsFetched / 30);

        // n elements that already exists on the cache, e.g., there are 37 on cache, so 7 elements of page 2 should be skipped.
        int nElementsToSkip = commitsFetched % 30;

        int page = currentPage == 0 ? 1 : currentPage;
        // if the current page is already full, i.e., 30, 60, 90, ..., we can just skip the current page
        if (currentPage != 0 && commitsFetched % 30 == 0) {
            page++;
        }
        for (; page <= currentPage + pageAway; page++) {
            final URL url = new URL(
                    format(
                            "%s/commits?sha=%s&page=%d",
                            this.projectUrl,
                            this.branch,
                            page
                    )
            );
            final List<CommitModel> commits = getCommitsByUrl(url);
            if (commits.size() == 0) {
                logger.debug("No more commits for page {}", page);
                return;
            }

            for (final CommitModel commit: commits) {
                if (nElementsToSkip != 0) {
                    nElementsToSkip--;
                    continue;
                }
                this.cache.get(this.branch).add(commit);
            }
        }
    }

    /**
     * Retrieves {@link List} of {@link CommitModel}. The list will be 30 Commits, unless there is less than 30 commits
     * or the given page has less than 30 commits which is the last page.
     *
     * @param url The {@link URL} used for the request.
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

    /**
     * Gets the {@link JSONObject} from the given URL.
     *
     * @param url The {@link URL} used for the request.
     * @return The {@link JSONObject} form request.
     * @throws IOException          If http request fails sending data.
     * @throws InterruptedException If http request is interrupted.
     * @throws URISyntaxException   If URL is invalid.
     * @throws ParseException       If message body is not a {@link JSONArray}.
     */
    private JSONObject getJsonObjectByUrl(final URL url)
            throws IOException, InterruptedException, URISyntaxException, ParseException {

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(url.toURI())
                .build();

        final HttpResponse<String> response = this.client.send(request,
                HttpResponse.BodyHandlers.ofString());
        final JSONParser parser = new JSONParser();

        return (JSONObject) parser.parse(response.body());
    }
}
