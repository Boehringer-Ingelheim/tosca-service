package com.edptoscaqs.toscaservice;
import com.edptoscaqs.toscaservice.command.HelpCommand;
import com.edptoscaqs.toscaservice.command.TestCommand;
import com.edptoscaqs.toscaservice.command.TestCommandCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;

@SpringBootApplication
@CommandLine.Command(
        name = "toscactl",
        description = "Command-line tool for managing TOSCA deployments.")
public class ToscaCLI implements CommandLineRunner, ExitCodeGenerator, TestCommandCallback {
    @Autowired
    private final HelpCommand helpCommand;

    private final TestCommand testCommand;

    private int exitCode;
    @Autowired
    public ToscaCLI(HelpCommand helpCommand, TestCommand testCommand) {
        this.helpCommand = helpCommand;
        this.testCommand = testCommand;
    }

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(ToscaCLI.class, args)));    }

    @Override
    public void run(String... args) {
        CommandLine commandLine = new CommandLine(this);
        commandLine.addSubcommand(helpCommand);
        commandLine.addSubcommand(testCommand);
        testCommand.setCallback(this);
        commandLine.execute(args);
    }

    @Override
    public int getExitCode() {
        return this.exitCode;
    }

    @Override
    public void onTestCommandResult(boolean allTestsPassed) {
        this.exitCode = allTestsPassed ? 0 : 1;
    }

    @Override
    public void onTestCommandException() {
        this.exitCode = 2;
    }
}
