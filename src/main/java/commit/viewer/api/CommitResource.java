package commit.viewer.api;

import commit.viewer.git.GitCommitAccess;
import commit.viewer.model.CommitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * A class that provides commit endpoint.
 * @author Sheng Wang (shenggwangg@gmail.com)
 */
@Path("/commit")
public class CommitResource {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(CommitResource.class);

    /**
     * Gets the text by given line from endpoint API.
     *
     * @param ownerName      The name of the owner of repository.
     * @param repositoryName The name of repository.
     * @param page           The page number.
     * @param size           The size number.
     * @return The response for corresponding request.
     */
    @GET
    public Response getCommits(@QueryParam("owner") String ownerName,
                               @QueryParam("repository") String repositoryName,
                               @QueryParam("page") Integer page,
                               @QueryParam("size") Integer size) {

        try {
            final List<CommitModel> commits = GitCommitAccess.INSTANCE.getCommitsFromAPI(ownerName, repositoryName, page, size);

            return Response.status(OK).type(APPLICATION_JSON).entity(commits).build();
        } catch (final Exception e) {
            logger.error("Failed to retrieve commits from {} by {}", repositoryName, ownerName);
            return Response.status(BAD_REQUEST).build();
        }
    }
}

