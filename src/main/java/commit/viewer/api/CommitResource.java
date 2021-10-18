package commit.viewer.api;

import commit.viewer.git.GitCommitAccess;
import commit.viewer.model.CommitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * A class that provides commit endpoint.
 * @author Sheng Wang (shenggwangg@gmail.com)
 */
@Path("/commits")
public class CommitResource {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(CommitResource.class);

    /**
     * Gets {@link List} of {@link CommitModel} by given query param from endpoint API.
     *
     * @param url   The given URL.
     * @param page  The page number.
     * @param size  The size number.
     * @return The response for corresponding request.
     */
    @GET
    @Produces(APPLICATION_JSON)
    public Response getCommits(@QueryParam("url") String url,
                               @QueryParam("page") Integer page,
                               @QueryParam("size") Integer size) {

        try {
            final boolean serverStarted = GitCommitAccess.INSTANCE.startProjectByURL(url);

            if (!serverStarted) {
                return Response.status(BAD_REQUEST).build();
            }
            final List<CommitModel> commits;
            if (page != null || size != null) {
                commits = GitCommitAccess.INSTANCE.getCommits(page, size);
            } else {
                commits = GitCommitAccess.INSTANCE.getCommits();
            }

            return Response.status(OK).type(APPLICATION_JSON).entity(commits).build();
        } catch (final Exception e) {
            logger.error("Failed to retrieve commits from {}", url);
            return Response.status(BAD_REQUEST).build();
        }
    }
}

