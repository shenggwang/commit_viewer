package commit.viewer.api;

import commit.viewer.model.CommitModel;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GitHub API commit access tests via REST API. Note that the tests uses real Github API,
 * which might lead to exceed <i>API rate limit</i>, hence all tests start failing.
 *
 * @author Sheng Wang (shenggwangg@gmail.com)
 */
public class CommitResourceTest extends JerseyTest {

    /**
     * The maximum number of clients.
     */
    private static final int MAX_CLIENTS = 10;

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        // Find first available port.
        forceSet(TestProperties.CONTAINER_PORT, "0");
        return new Application () {
            public Set getSingletons() {
                final Set<Object> set = new HashSet<>();
                set.add(new CommitResource());
                set.add(new JacksonFeature());
                return set;
            }
        };
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        config.property(ClientProperties.ASYNC_THREADPOOL_SIZE, MAX_CLIENTS + 2);
    }

    /**
     * Check if the get commits responds with status 200 with a list of commit in JSON format.
     */
    @Test
    public void ensureRequestWorks() {

        final Response response = target("/commits")
                .queryParam("url", "https://github.com/apache/spark.git")
                .queryParam("page", 1)
                .queryParam("size", "5")
                .request().get();

        assertThat(response.getStatus())
                .as("Http Response should be 200.")
                .isEqualTo(Response.Status.OK.getStatusCode());

        assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE))
                .as("HHttp Content-Type should be JSON.")
                .isEqualTo(MediaType.APPLICATION_JSON);

        final List<CommitModel> commits1 = response.readEntity(List.class);

        assertThat(commits1)
                .as("API must be able to retrieve 5 commits.")
                .hasSize(5);
    }
}
