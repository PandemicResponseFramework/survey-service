/**
 *
 */
package one.tracking.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.fasterxml.classmate.TypeResolver;
import one.tracking.framework.dto.meta.AnswerDto;
import one.tracking.framework.dto.meta.container.ContainerDto;
import one.tracking.framework.web.SurveyController;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author Marko Voß
 *
 */
@Configuration
@EnableSwagger2
public class SpringFoxConfig {

  @Bean
  @Primary
  public Docket api(final TypeResolver typeResolver) {
    return new Docket(DocumentationType.SWAGGER_2)
        .select()
        .apis(RequestHandlerSelectors.basePackage(SurveyController.class.getPackageName()))
        .paths(PathSelectors.any())
        .build()
        .additionalModels(
            typeResolver.resolve(ContainerDto.class),
            typeResolver.resolve(AnswerDto.class));
  }

}
