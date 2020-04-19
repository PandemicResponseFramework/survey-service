/**
 *
 */
package one.tracking.framework.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import one.tracking.framework.dto.DtoMapper;
import one.tracking.framework.dto.meta.SurveyDto;
import one.tracking.framework.repo.SurveyRepository;

/**
 * @author Marko Voß
 *
 */
@RestController
@RequestMapping("/survey")
public class SurveyController {

  @Autowired
  private SurveyRepository surveyRepository;

  @RequestMapping(method = RequestMethod.GET, path = "/{nameId}")
  public SurveyDto getSurvey(
      @PathVariable("nameId")
      final String nameId) {

    return DtoMapper.map(this.surveyRepository.findByNameId(nameId).get());
  }
}
