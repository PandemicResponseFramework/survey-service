/**
 *
 */
package one.tracking.framework.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import one.tracking.framework.dto.SurveyDto;
import one.tracking.framework.repo.SurveyRepository;

/**
 * @author Marko Voß
 *
 */
@RestController
@RequestMapping
public class SurveyController {

  @Autowired
  private SurveyRepository surveyRepository;

  @RequestMapping(method = RequestMethod.GET)
  public SurveyDto getSurvey() {
    return SurveyDto.fromEntity(this.surveyRepository.findAll().iterator().next());
  }
}
