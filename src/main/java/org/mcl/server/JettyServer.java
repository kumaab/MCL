
package org.mcl.server;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.mcl.model.GitCommit;
import org.mcl.model.GitRepo;
import org.mcl.utils.Constants;
import org.mcl.utils.Main;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.List;
import java.util.Properties;
import java.util.TimerTask;

@Slf4j
public class JettyServer {
    // Periodic interval in milliseconds
    private static final long FETCH_INTERVAL = 30 * 60 * 1000; // 30 minutes
    private static final long N_COMMITS = 20;
    private static Main main;
    private static GitRepo sourceRepo;
    private static List<GitCommit> sourceCommits;

    // Start the Jetty server and fetchCommits periodically
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080); // Port number to run Jetty server
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Add a servlet to handle requests
        context.addServlet(new ServletHolder(new FetchCommitsServlet()), "/*");

        server.start();

        initRepo();

        // Schedule a timer task to periodically fetchCommits
        //Timer timer = new Timer();
        //timer.scheduleAtFixedRate(new FetchCommitsTask(), FETCH_INTERVAL, FETCH_INTERVAL);
    }

    // FetchCommits servlet to handle requests
    static class FetchCommitsServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            // Call fetchCommits and write the response to the client
            log.info("caller");
            String commits = fetchCommits(N_COMMITS);
            response.setContentType("text/plain");
            PrintWriter writer = response.getWriter();
            writer.write(commits);
            writer.flush();
        }
    }

    // FetchCommits task to be scheduled by timer
    static class FetchCommitsTask extends TimerTask {
        @Override
        public void run() {
            // Call fetchCommits and do something with the response
            String commits = fetchCommits(N_COMMITS);
            System.out.println("Fetched commits: " + commits);
        }
    }

    static void initRepo() throws GitAPIException {
        main = new Main();
        Properties props = main.getProperties();
        sourceRepo  = new GitRepo(
                props.getProperty(Constants.SOURCE_LOCAL_PATH),
                props.getProperty(Constants.SOURCE_GIT_URL),
                props.getProperty(Constants.SOURCE_BRANCH_NAME),
                props.getProperty(Constants.SOURCE_ALIAS),
                main.toCloneSource());
        LocalDate endDate = LocalDate.now();
        sourceCommits = main.process(sourceRepo, endDate.minus(15, ChronoUnit.MONTHS), endDate);
    }

    // Method to fetch last N commits
    static String fetchCommits(long N) {
        // Implement your logic to fetch commits here
        // and return the response as a string
        Random random = new Random();
        int randomNumber = random.nextInt(10);
        N = N + randomNumber;

        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < N && i < sourceCommits.size(); i++){
            GitCommit gitCommit = sourceCommits.get(i);
            String message = gitCommit.getCommitMessage() + " --- " + gitCommit.getAuthor() + "\n";
            stringBuilder.append(message);
        }
        return stringBuilder.toString();
    }
}
