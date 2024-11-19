package com.edptoscaqs.toscaservice.command;

import com.edptoscaqs.toscaservice.ToscaCLI;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "help",
        description = "Displays help information.")
public class HelpCommand implements Runnable {

    @CommandLine.ParentCommand
    private ToscaCLI parent;

    @Override
    public void run() {
        CommandLine.usage(parent, System.out);
    }
    public void showHelpIfRequested(String[] args) {
        CommandLine cmd = new CommandLine(this);
        if (args.length > 0 && ("-h".equals(args[0]) || "--help".equals(args[0]))) {
            cmd.usage(System.out);
            System.exit(0);
        }
    }
}
