# Tricentis Tosca API CLI

The Tricentis Tosca API CLI is a command-line interface tool that allows you to interact with the Tricentis Tosca Execution API using a simple and intuitive CLI. With this tool, you can trigger the execution of events, check the status of ongoing executions, and retrieve the results of completed executions.

## Requirements

- Java 17 or higher
- Gradle 7.3 or higher
- Tricentis Tosca Execution API access token
- Tricentis Tosca server URL

## Dependencies

- Spring Boot 2.7.1
- Spring Web
- Spring Boot Test
- Spring Boot Starter Validation
- JUnit Jupiter
- Mockito Core

## Usage

To run the Tricentis Tosca API CLI, follow the instructions below:

1. Clone the project repository to your local machine.
2. Open a command prompt or terminal window and navigate to the project directory.
3. Build the project using the following command:

```
./gradlew build
```

4. Set ENVIRONMENT VARIABLES: 

Windows:
```
set TOSCA_SERVER="value";TOSCA_SERVER_CLIENT_ID="value";TOSCA_SERVER_CLIENT_SECRET="value";TOSCA_SERVER_PORT="value";CONFIGURATION_FILE_PATH="value";EDP_LOCK_GROUP_NAME="value";
```

Mac:
```
export TOSCA_SERVER="value"
export TOSCA_SERVER_CLIENT_ID="value"
export TOSCA_SERVER_CLIENT_SECRET="value"
export TOSCA_SERVER_PORT="value"
export CONFIGURATION_FILE_PATH="value"
export EDP_LOCK_GROUP_NAME="value";
```

5. You can now use the CLI to trigger the execution of events, check the status of ongoing executions, and retrieve the results of completed executions. For detailed usage instructions, run the following command:

```
java -jar docker/app.jar --help
```

```
Usage:
  toscactl test <projectName> <testEvent> [-h][-o=<outputPath>] [-r=<releaseExecution>] [-t=<testType>] [-s=<String=String>]... [-g=<String=String>]... [-c=<String=String>]...
Runs a test suite with the specified project name and test event.
  <projectName>   Root name of the Tosca project containing the test event.
  <testEvent>     Name of the test event to execute.
  -h, --help [Optional] Show this help message and exit.
  -o, --output-path=<outputPath> [Optional] Path to save the test results. Default is build/test-results.
  -r, --release=<boolean> [Optional] Specify if the execution is part of a release. Default is false.
  -t, --test-type=<testType> [Optional] Type of test to run (acceptance, integration, installation, or all). Refer to the tosca-configuration.json file for configuration details. Default is all.
  -s, --suite-parameter=<String=String> [Optional] Test configuration parameters for the test suite to define environmental settings.
  -g, --git-parameter=<String=String> [Optional] Git parameters to include in the PDF report.
  -c, --characteristics=<String=String> [Optional] Characteristics to define which Agents should execute the tests.
```

## Configuration

The `tosca-configuration.json` file contains the configuration for different test types:

```json
{
  "all": {
    "executionWaitTimeOut": 60,
    "statusSleepTime": 30000,
    "reportCreationTimeOut": 15000,
    "pdfReportName": "ToscaIntegrationReport"
  },
  "acceptance": {
    "executionWaitTimeOut": 60,
    "statusSleepTime": 60000,
    "reportCreationTimeOut": 15000,
    "pdfReportName": "ToscaIntegrationReport"
  },
  "installation": {
    "executionWaitTimeOut": 60,
    "statusSleepTime": 50000,
    "reportCreationTimeOut": 15000,
    "pdfReportName": "ToscaIntegrationReport"
  },
  "integration": {
    "executionWaitTimeOut": 60,
    "statusSleepTime": 40000,
    "reportCreationTimeOut": 15000,
    "pdfReportName": "ToscaIntegrationReport"
  }
}
```

## Contributing

If you'd like to contribute to this project, please follow the steps below:

1. Fork this repository.
2. Create a new branch with your changes: `git checkout -b my-branch`
3. Make your changes and commit them: `git commit -m "my changes"`
4. Push your changes to your fork: `git push origin my-branch`
5. Create a pull request for your changes.

## License

This project is licensed under the [MIT License](https://opensource.org/licenses/MIT).

## Links

* [Tricentis Tosca](https://www.tricentis.com/products/automate-continuous-testing-tosca/)
* [tosca-service](https://github.com/Boehringer-Ingelheim/tosca-service/)