![Maven Package & Integration Tests](https://github.com/PandemicResponseFramework/survey-service/workflows/Maven%20Package%20&%20Integration%20Tests/badge.svg)

# Survey Service
Survey service for client interaction

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

You need to have [Maven](https://maven.apache.org/) installed on your machine. You will also need to have a [SendGrid](https://sendgrid.com/) account setup with the desired [sender authentication](https://app.sendgrid.com/settings/sender_auth).

This project depends on the [survey-commons](https://github.com/PandemicResponseFramework/survey-commons) project. Therefore you need to [create an access token](https://help.github.com/en/packages/publishing-and-managing-packages/about-github-packages) in order to be able to access the package. You will also need to setup maven to use this access token by creating or modifying your `settings.xml`, which is usually located at `<USER_HOME>/.m2`. Add the following server-element to the `settings.xml`.

```
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">

  <servers>
    <server>
      <id>com.github.PandemicResponseFramework.survey-commons</id>
      <username>GITHUB_USERID</username>
      <password>GITHUB_TOKEN</password>
    </server>
  </servers>

</settings>
```

### Installing

To build the executable JAR, execute the following command in the root directory of this project.
```
mvn clean package
```

This will create the executable JAR file in the `target` directory, which can be used for executing the server application. Copy this JAR file to the desired location.

In order to use customized configuration, please refer to the [official documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config).
You can place a file named `application.properties` right next to this JAR file.

## Configuration

Next to the common [server properties](https://docs.spring.io/spring-boot/docs/current/reference/html/appendix-application-properties.html#server-properties) and [logging properties](https://docs.spring.io/spring-boot/docs/current/reference/html/appendix-application-properties.html#core-properties), which can be placed on the customized configuration, the application uses the following configuration properties.

TODO

## Database

This service is currently utilizing a H2 in-memory database or a MySql database. The MySQL database will be used by default. You can perform the switch by Maven profiles: mysql, h2

## API Documentation

The API documentation will be available at runtime on the path `/swagger-ui.html#`.

## Running the tests

The unit tests will be performed on the maven package build automatically. In order to perform the integration tests, execute the following command.

```
mvn clean integration-test
```

## Deployment

TODO

## Built With

* [Spring Boot](https://spring.io/projects/spring-boot) - Enterprise Application Framework
* [Maven](https://maven.apache.org) - Dependency Management

## Contributing

TODO

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/OneTrackingFramework/survey-service/tags). 

## Authors

* **Marko Voß** - *Initial work* - [mk0](https://gist.github.com/mk0)

See also the list of [contributors](https://github.com/OneTrackingFramework/survey-service/contributors) who participated in this project.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

## Acknowledgments

* Many thanks to [foerster technologies](https://foerster-technologies.com) for hosting the server during the development
* Many thanks to [PurpleBooth](https://github.com/PurpleBooth) for creating the [README Template](https://gist.github.com/PurpleBooth/109311bb0361f32d87a2)
