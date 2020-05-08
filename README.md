![Maven Package](https://github.com/OneTrackingFramework/survey-service/workflows/Maven%20Package/badge.svg)

# Survey Service
Survey management and evaluation service

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

You need to have [Maven](https://maven.apache.org/) installed on your machine. You will also need to have a [SendGrid](https://sendgrid.com/) account setup with the desired [sender authentication](https://app.sendgrid.com/settings/sender_auth).

### Installing

A step by step series of examples that tell you how to get a development env running

Run

```
mvn clean package
```

This will create the executable JAR file in the `target` directory, which can be used for executing the server application. Copy this JAR file to the desired location.

In order to use customized configuration, please refer to the [official documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config).
You can place a file named `application.properties` right next to this JAR file.

## Configuration

Next to the common [server properties](https://docs.spring.io/spring-boot/docs/current/reference/html/appendix-application-properties.html#server-properties)and [logging properties](https://docs.spring.io/spring-boot/docs/current/reference/html/appendix-application-properties.html#core-properties), which can be placed on the customized configuration, the application uses the following configuration properties.

<table class="tg">
<thead>
  <tr>
    <th class="tg-c3ow">Property</th>
    <th class="tg-c3ow">Description</th>
    <th class="tg-c3ow">Example</th>
  </tr>
</thead>
<tbody>
  <tr>
    <td class="tg-0pky">app.sendgrid.api.key</td>
    <td class="tg-0pky">The SendGrid API key to use for sending e-mails to the participants.</td>
    <td class="tg-0pky">-</td>
  </tr>
  <tr>
    <td class="tg-0pky">app.email.reply.to</td>
    <td class="tg-0pky">The reply-to e-mail address to be used. If single sender authentication is setup in SendGrid, please make sure, to use this reply-to e-mail address here.</td>
    <td class="tg-0pky">-</td>
  </tr>
  <tr>
    <td class="tg-0pky">app.email.from</td>
    <td class="tg-0pky">The e-mail address to use as the sender of the e-mails sent to the participants. If single sender authentication is setup in SendGrid, please make sure, to use this e-mail address here.</td>
    <td class="tg-0pky">-</td>
  </tr>
  <tr>
    <td class="tg-0pky">app.verification.timeout</td>
    <td class="tg-0pky">The timeout in seconds, how long the verification token stays valid.</td>
    <td class="tg-0pky">-</td>
  </tr>
  <tr>
    <td class="tg-0pky">app.custom.uri.prefix</td>
    <td class="tg-0pky">The custom URI scheme to be used for redirection to trigger the App on mobile devices.</td>
    <td class="tg-0pky">-</td>
  </tr>
  <tr>
    <td class="tg-0pky">app.public.url</td>
    <td class="tg-0pky">The public URL this server is available on. This will be used in the e-mails send to the participants in order to provide a verification, which points to the server instance.</td>
    <td class="tg-0pky">-</td>
  </tr>
  <tr>
    <td class="tg-0pky">app.token.secret</td>
    <td class="tg-0pky">The secret to use for JWT signature generation and validation. This is required in order to create the access tokens for the participants.</td>
    <td class="tg-0pky">-</td>
  </tr>
  <tr>
    <td class="tg-0pky">logging.level.one.tracking.framework</td>
    <td class="tg-0pky">Setup of the LOG level for the entire service.</td>
    <td class="tg-0pky">INFO | DEBUG | WARN | ERROR</td>
  </tr>
</tbody>
</table>

## API Documentation

The API documentation will be available at runtime on the path `/swagger-ui.html#`.
You can also view the current API documentation on [SwaggerHub](https://app.swaggerhub.com/apis/mk01/survey-service-api/1.0).

## Running the tests

The tests will be performed on the maven package build automatically.

## Deployment

TODO

## Built With

* [Spring Boot](https://spring.io/projects/spring-boot) - Enterprise Application Framework
* [Maven](https://maven.apache.org) - Dependency Management
* [Tables Generator](https://www.tablesgenerator.com) - Creating beautiful tables for this document

## Contributing

Please read [CONTRIBUTING.md](https://gist.github.com/PurpleBooth/b24679402957c63ec426) for details on our code of conduct, and the process for submitting pull requests to us.

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/OneTrackingFramework/survey-service/tags). 

## Authors

* **Marko Voß** - *Initial work* - [mk0](https://gist.github.com/mk0)

See also the list of [contributors](https://github.com/OneTrackingFramework/survey-service/contributors) who participated in this project.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

## Acknowledgments

* Many thanks to [PurpleBooth](https://github.com/PurpleBooth) for creating the [README Template](https://gist.github.com/PurpleBooth/109311bb0361f32d87a2)