package org.mcl.utils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Arguments {

    private final Options options;
    private final CommandLineParser  parser;
    private CommandLine cmd;
    Arguments(String[] args){
        options = new Options();
        parser = new DefaultParser();
        build();
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Parsing Error: " + e.getMessage());
        }
    }

    void build(){
        Option authorOption = Option.builder("a")
                .longOpt("author")
                .desc("Author Name")
                .hasArg()
                .build();

        Option committerOption = Option.builder("c")
                .longOpt("committer")
                .desc("Committer Name")
                .hasArg()
                .build();

        Option daysOption = Option.builder("d")
                .longOpt("days")
                        .desc("Number of days")
                                .hasArg()
                                        .build();

        Option monthsOption = Option.builder("m")
                .longOpt("months")
                        .desc("Number of Months")
                                .hasArg()
                                        .build();

        Option sortOption = Option.builder("s")
                .longOpt("sort")
                        .desc("Sort the commits")
                                .build();
        Option missingOption = Option.builder()
                .longOpt("missing")
                        .desc("Missing Commits")
                                .build();
        Option dumpOption = Option.builder()
                .longOpt("dump")
                        .desc("Dump results to file")
                                .build();
        Option statsOption = Option.builder()
                .longOpt("stats")
                        .desc("Show statistics of the results")
                                .build();

        // Add the options to the Options object
        options.addOption(authorOption);
        options.addOption(committerOption);
        options.addOption(daysOption);
        options.addOption(monthsOption);
        options.addOption(sortOption);
        options.addOption(missingOption);
        options.addOption(dumpOption);
        options.addOption(statsOption);
    }

    public String getAuthorName(){
        return cmd.getOptionValue("a");
    }
    public String getCommitterName(){
        return cmd.getOptionValue("c");
    }
    public int getNumberOfDays(){
        String d = cmd.getOptionValue("d");
        if (d == null)
            return 0;
        else return Integer.parseInt(d);
    }
    public int getNumberOfMonths(){
        String m = cmd.getOptionValue("m");
        if (m == null)
            return 0;
        else return Integer.parseInt(m);
    }
    public boolean isSortEnabled(){
        return cmd.hasOption("sort");
    }
    public boolean isMissingEnabled(){
        return cmd.hasOption("missing");
    }
    public boolean isDumpEnabled(){
        return cmd.hasOption("dump");
    }
    public boolean isStatsEnabled(){
        return cmd.hasOption("stats");
    }
}
