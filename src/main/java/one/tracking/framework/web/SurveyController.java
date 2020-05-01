/**
 *
 */
package one.tracking.framework.web;

import java.io.IOException;
import java.io.InputStream;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import one.tracking.framework.dto.DtoMapper;
import one.tracking.framework.dto.SurveyResponseDto;
import one.tracking.framework.dto.meta.SurveyDto;
import one.tracking.framework.service.SurveyService;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @author Marko Voß
 *
 */
@RestController
@RequestMapping
public class SurveyController {

  @Autowired
  private SurveyService surveyService;

  @RequestMapping(method = RequestMethod.GET, path = "/check")
  public void checkAuthN() {
    // Call this endpoint to evaluate if bearer token is still valid
  }

  @RequestMapping(method = RequestMethod.GET, path = "/survey/{nameId}")
  public SurveyDto getSurvey(
      @PathVariable("nameId")
      final String nameId) {

    return DtoMapper.map(this.surveyService.getSurvey(nameId));
  }

  @RequestMapping(method = RequestMethod.POST, path = "/survey/{nameId}/answer")
  public void handleSurveyResponse(
      @PathVariable("nameId")
      final String nameId,
      @RequestBody
      @Valid
      final SurveyResponseDto surveyResponse,
      @ApiIgnore
      final Authentication authentication) {

    this.surveyService.handleSurveyResponse(authentication.getName(), nameId, surveyResponse);
  }

  @RequestMapping(
      method = RequestMethod.POST,
      path = "/verify",
      consumes = MediaType.TEXT_PLAIN_VALUE,
      produces = MediaType.TEXT_PLAIN_VALUE)
  public String verify(
      @RequestBody
      final String hash) {

    return this.surveyService.verifyEmail(hash);
  }

  @RequestMapping(
      method = RequestMethod.GET,
      path = "/verify")
  public String handleVerification(
      @RequestParam("token")
      final String token) {

    return this.surveyService.handleVerificationRequest(token);
  }

  @RequestMapping(method = RequestMethod.POST, path = "/register", consumes = MediaType.TEXT_PLAIN_VALUE)
  public void registerParticipant(
      @RequestBody
      final String email) throws IOException {

    this.surveyService.registerParticipant(email, true);
  }

  @RequestMapping(
      method = RequestMethod.POST,
      path = "/register/import",
      consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public void importParticipants(final InputStream inputStream) throws IOException {

    this.surveyService.importParticipants(inputStream);
  }

}
