package commit.viewer;

import commit.viewer.api.CommitResource;
import commit.viewer.api.HealthResource;
import commit.viewer.git.GitCommitAccess;
import commit.viewer.model.CommitModel;
import io.netty.channel.Channel;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.netty.httpserver.NettyHttpContainerProvider;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.Application;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Main server process.
 *
 * @author Sheng Wang (shenggwangg@gmail.com)
 */
public class Main {

    /**
     * Method called from the operating system.
     */
    public static void main(final String[] args) {
        startRestfulServer();
        
        final Scanner inputScanner = new Scanner(System.in);

        System.out.println("Program started!");
        System.out.println("Insert your input:");

        while (inputScanner.hasNextLine()) {
            final String line = inputScanner.nextLine().trim();
            if (line.equals("exit")) {
                break;
            }

            final String[] commands = line.split("\\s+");
            if (commands.length < 2 || !commands[0].equals("git")) {
                System.out.println("Invalid command");
                continue;
            }
            switch (commands[1]) {
                case "log":
                    if (commands.length != 2) {
                        System.out.println("Command invalid, see valid example below:");
                        System.out.println("$ git log");
                    }
                    if (!GitCommitAccess.INSTANCE.isProjectStarted()) {
                        System.out.println("Please clone a project first.");
                        break;
                    }
                    try {
                        final List<CommitModel> commits = GitCommitAccess.INSTANCE.getCommits();
                        for (final CommitModel commit: commits) {
                            System.out.println("sha: " + commit.getSha());
                            System.out.println("author: " + commit.getAuthor());
                            System.out.println("date: " + commit.getDate());
                            System.out.println("message: " + commit.getMessage());
                            System.out.println("\n-------------------------------\n");
                        }
                    } catch (final Exception e) {
                        System.out.println("Error trying to retrieve commits.");
                    }
                    break;
                case "checkout":
                    if (commands.length != 3) {
                        System.out.println("Command invalid, see valid example below:");
                        System.out.println("$ git checkout master");
                        break;
                    }
                    if (!GitCommitAccess.INSTANCE.isProjectStarted()) {
                        System.out.println("Please clone a project first.");
                        break;
                    }
                    GitCommitAccess.INSTANCE.setBranch(commands[2]);
                    System.out.println("Successfully set URL to: " + commands[2]);
                    break;
                case "branch":
                    if (commands.length != 2) {
                        System.out.println("Command invalid, see valid example below:");
                        System.out.println("$ git checkout https://api.github.com/repos/apache/spark/commits");
                        break;
                    }
                    if (!GitCommitAccess.INSTANCE.isProjectStarted()) {
                        System.out.println("Please clone a project first.");
                        break;
                    }
                    System.out.println("The list below shows the branch cached locally:");
                    for (final String branch: GitCommitAccess.INSTANCE.listBranches()) {
                        if (branch.equals(GitCommitAccess.INSTANCE.getCurrentBranch())) {
                            System.out.println("[current] " + branch);
                            continue;
                        }
                        System.out.println(branch);
                    }
                    break;
                case "clone":
                    if (commands.length != 3) {
                        System.out.println("Command invalid, see valid example below:");
                        System.out.println("$ git clone https://github.com/apache/spark.git");
                        break;
                    }
                    if (GitCommitAccess.INSTANCE.startProjectByURL(commands[2])) {
                        System.out.println("Project started with URL:" + commands[2]);
                        break;
                    }
                    System.out.println("Failed starting project with URL:" + commands[2]);
                    break;
                default: System.out.println("Invalid command, valid commands: 'log' and 'checkout'");
            }
            System.out.println("Insert your input:");
        }
    }

    private static void startRestfulServer() {
        final ResourceConfig resourceConfig = ResourceConfig.forApplication(
                new Application() {
                    public Set getSingletons() {
                        final Set<Object> set = new HashSet<>();
                        set.add(new HealthResource());
                        set.add(new CommitResource());
                        return set;
                    }
                }
        ).register(new JacksonFeature());

        final URI uri = URI.create("http://localhost:8080/");
        final Channel server = NettyHttpContainerProvider.createHttp2Server(uri, resourceConfig, null);
        Runtime.getRuntime().addShutdownHook(new Thread((server::close)));
    }
}

