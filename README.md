![Maven Package & Integration Tests](https://github.com/OneTrackingFramework/survey-service/workflows/Maven%20Package%20&%20Integration%20Tests/badge.svg)

# Survey Service
Survey management and evaluation service

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

You need to have [Maven](https://maven.apache.org/) installed on your machine. You will also need to have a [SendGrid](https://sendgrid.com/) account setup with the desired [sender authentication](https://app.sendgrid.com/settings/sender_auth).

This project depends on the [commons-boot](https://github.com/OneTrackingFramework/commons-boot) project. Therefore you need to [create an access token](https://help.github.com/en/packages/publishing-and-managing-packages/about-github-packages) in order to be able to access the package. You will also need to setup maven to use this access token by creating or modifying your `settings.xml`, which is usually located at `<USER_HOME>/.m2`. Add the following server-element to the `settings.xml`.

```
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">

  <servers>
    <server>
      <id>com.github.OneTrackingFramework.commons-boot</id>
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

<table>
<thead>
  <tr>
    <th>Property</th>
    <th>Description</th>
    <th>Example</th>
  </tr>
</thead>
<tbody>
  <tr>
    <td>app.sendgrid.api.key</td>
    <td>The SendGrid API key to use for sending e-mails to the participants.<br/>:warning: This property is subject to be removed and replaced by the built-in property spring.sendgrid.api-key</td>
    <td>-</td>
  </tr>
  <tr>
    <td>app.email.reply.to</td>
    <td>The reply-to e-mail address to be used. If single sender authentication is setup in SendGrid, please make sure, to use this reply-to e-mail address here.</td>
    <td>-</td>
  </tr>
  <tr>
    <td>app.email.from</td>
    <td>The e-mail address to use as the sender of the e-mails sent to the participants. If single sender authentication is setup in SendGrid, please make sure, to use this e-mail address here.</td>
    <td>-</td>
  </tr>
  <tr>
    <td>app.timeout.verification</td>
    <td>The timeout in seconds, how long the verification token stays valid.</td>
    <td>-</td>
  </tr>
  <tr>
    <td>app.timeout.access</td>
    <td>The timeout in seconds, how long the access token stays valid.</td>
    <td>-</td>
  </tr>
  <tr>
    <td>app.custom.uri.prefix</td>
    <td>The custom URI scheme to be used for redirection to trigger the App on mobile devices.</td>
    <td>-</td>
  </tr>
  <tr>
    <td>app.public.url</td>
    <td>The public URL this server is available on. This will be used in the e-mails send to the participants in order to provide a verification link, which points to the server instance.</td>
    <td>-</td>
  </tr>
  <tr>
    <td>app.token.secret</td>
    <td>The secret to use for JWT signature generation and validation. This is required in order to create the access tokens for the participants.</td>
    <td>-</td>
  </tr>
  <tr>
    <td>logging.level.one.tracking.framework</td>
    <td>Setup of the LOG level for the entire service.</td>
    <td>INFO | DEBUG | WARN | ERROR</td>
  </tr>
</tbody>
</table>

## Database

This service is currently utilizing a H2 in-memory database. This will be adjusted in the future of course.

## API Documentation

The API documentation will be available at runtime on the path `/swagger-ui.html#`.
You can also view the current API documentation on [SwaggerHub](https://app.swaggerhub.com/apis/mk01/survey-service-api/1.0).

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
