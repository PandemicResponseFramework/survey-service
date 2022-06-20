/**
 *
 */
package one.tracking.framework.web;

import java.util.Collection;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Parameter;
import one.tracking.framework.dto.DtoMapper;
import one.tracking.framework.dto.SurveyResponseDto;
import one.tracking.framework.dto.SurveyStatusDto;
import one.tracking.framework.dto.meta.SurveyDto;
import one.tracking.framework.exception.SurveyResponseConflictException;
import one.tracking.framework.service.SurveyResponseService;
import one.tracking.framework.service.SurveyService;

/**
 * @author Marko Voß
 *
 */
@RestController
@RequestMapping
public class SurveyController {

  @Autowired
  private SurveyService surveyService;

  @Autowired
  private SurveyResponseService surveyResponseService;

  @RequestMapping(method = RequestMethod.GET, path = "/survey/{nameId}")
  public SurveyDto getSurvey(
      @PathVariable("nameId")
      final String nameId) {

    return DtoMapper.map(this.surveyService.getReleasedSurvey(nameId));
  }

  @RequestMapping(method = RequestMethod.GET, path = "/overview")
  public Collection<SurveyStatusDto> getSurveyOverviews(
      @Parameter(hidden = true)
      final Authentication authentication) {

    return this.surveyService.getSurveyOverview(authentication.getName());
  }

  @RequestMapping(method = RequestMethod.GET, path = "/overview/{nameId}")
  public SurveyStatusDto getSurveyOverview(
      @PathVariable("nameId")
      final String nameId,
      @Parameter(hidden = true)
      final Authentication authentication) {

    return this.surveyService.getSurveyOverview(nameId, authentication.getName());
  }

  @RequestMapping(method = RequestMethod.POST, path = "/survey/{nameId}/answer")
  public void postSurveyResponse(
      @PathVariable("nameId")
      final String nameId,
      @RequestBody
      @Valid
      final SurveyResponseDto surveyResponse,
      @Parameter(hidden = true)
      final Authentication authentication) throws SurveyResponseConflictException {

    this.surveyResponseService.handleSurveyResponse(authentication.getName(), nameId, surveyResponse);
  }

}
