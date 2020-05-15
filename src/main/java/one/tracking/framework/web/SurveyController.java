/**
 *
 */
package one.tracking.framework.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
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
import one.tracking.framework.dto.AuthNTokenResponseDto;
import one.tracking.framework.dto.DtoMapper;
import one.tracking.framework.dto.RegistrationDto;
import one.tracking.framework.dto.SurveyResponseDto;
import one.tracking.framework.dto.SurveyStatusDto;
import one.tracking.framework.dto.VerificationDto;
import one.tracking.framework.dto.meta.SurveyDto;
import one.tracking.framework.service.SurveyManagementService;
import one.tracking.framework.service.SurveyResponseService;
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

  @Autowired
  private SurveyResponseService surveyResponseService;

  @Autowired
  private SurveyManagementService surveyManagementService;

  @RequestMapping(method = RequestMethod.GET, path = "/check")
  public void checkAuthN() {
    // Call this endpoint to evaluate if bearer token is still valid
  }

  @RequestMapping(method = RequestMethod.GET, path = "/survey/{nameId}")
  public SurveyDto getSurvey(
      @PathVariable("nameId")
      final String nameId) {

    return DtoMapper.map(this.surveyService.getReleasedSurvey(nameId));
  }

  @RequestMapping(method = RequestMethod.GET, path = "/survey")
  public List<SurveyStatusDto> getSurveyOverview(
      @ApiIgnore
      final Authentication authentication) {

    return this.surveyService.getSurveyOverview(authentication.getName());
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

    this.surveyResponseService.handleSurveyResponse(authentication.getName(), nameId, surveyResponse);
  }

  @RequestMapping(
      method = RequestMethod.POST,
      path = "/verify")
  public AuthNTokenResponseDto verify(
      @RequestBody
      @Valid
      final VerificationDto verification) throws IOException {

    return AuthNTokenResponseDto.builder().token(this.surveyService.verifyEmail(verification)).build();
  }

  @RequestMapping(
      method = RequestMethod.GET,
      path = "/verify")
  public String handleVerification(
      @RequestParam(name = "token", required = true)
      final String verificationToken,
      @RequestParam(name = "userToken", required = false)
      final String userToken) {

    return this.surveyService.handleVerificationRequest(verificationToken, userToken);
  }

  @RequestMapping(
      method = RequestMethod.POST,
      path = "/register")
  public void registerParticipant(
      @RequestBody
      final RegistrationDto registration) throws IOException {

    this.surveyService.registerParticipant(registration, true);
  }

  @RequestMapping(
      method = RequestMethod.POST,
      path = "/register/import",
      consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public void importParticipants(final InputStream inputStream) throws IOException {

    this.surveyService.importParticipants(inputStream);
  }

}
