/**
 *
 */
package one.tracking.framework.web;

import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import one.tracking.framework.dto.StepCountDto;
import one.tracking.framework.service.HealthService;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @author Marko Voß
 *
 */
@RestController
@RequestMapping("/health")
public class HealthController {

  @Autowired
  private HealthService healthService;

  @RequestMapping(method = RequestMethod.POST, path = "/stepcount")
  public void postStepCount(
      @RequestBody
      @Valid
      final StepCountDto stepCountDto,
      @ApiIgnore
      final Authentication authentication) {

    this.healthService.storeStepCount(authentication.getName(), stepCountDto);
  }
}
