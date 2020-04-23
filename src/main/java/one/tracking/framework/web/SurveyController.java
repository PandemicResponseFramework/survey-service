/**
 *
 */
package one.tracking.framework.web;

import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import one.tracking.framework.dto.DtoMapper;
import one.tracking.framework.dto.meta.SurveyDto;
import one.tracking.framework.service.SurveyService;

/**
 * @author Marko Voß
 *
 */
@RestController
@RequestMapping("/survey")
public class SurveyController {

  @Autowired
  private SurveyService surveyService;

  @RequestMapping(method = RequestMethod.GET, path = "/survey/{nameId}")
  public SurveyDto getSurvey(
      @PathVariable("nameId")
      final String nameId) {

    return DtoMapper.map(this.surveyService.getSurvey(nameId));
  }

  @RequestMapping(
      method = RequestMethod.POST,
      path = "/verify",
      consumes = MediaType.TEXT_PLAIN_VALUE,
      produces = MediaType.TEXT_PLAIN_VALUE)
  public String verify(
      @RequestBody
      final String hash) {

    final String jwt = this.surveyService.verifyEmail(hash);
    return Base64.encodeBase64String(jwt.getBytes(Charset.defaultCharset()));
  }

  @RequestMapping(method = RequestMethod.POST, path = "/register", consumes = MediaType.TEXT_PLAIN_VALUE)
  public void register(
      @RequestBody
      final String email) throws IOException {

    this.surveyService.registerParticipant(email);
  }

}
