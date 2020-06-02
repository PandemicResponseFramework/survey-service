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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import one.tracking.framework.dto.AuthNTokenResponseDto;
import one.tracking.framework.dto.DeviceTokenDto;
import one.tracking.framework.dto.RegistrationDto;
import one.tracking.framework.dto.VerificationDto;
import one.tracking.framework.service.AuthService;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @author Marko Voß
 *
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

  @Autowired
  private AuthService authService;

  @RequestMapping(method = RequestMethod.GET, path = "/check")
  public void checkAuthN() {
    // Call this endpoint to evaluate if bearer token is still valid
  }

  @RequestMapping(
      method = RequestMethod.POST,
      path = "/verify")
  public AuthNTokenResponseDto verify(
      @RequestBody
      @Valid
      final VerificationDto verification) throws IOException {

    return AuthNTokenResponseDto.builder().token(this.authService.verifyEmail(verification)).build();
  }

  @RequestMapping(
      method = RequestMethod.GET,
      path = "/verify")
  public String handleVerification(
      @RequestParam(name = "token", required = true)
      final String verificationToken,
      @RequestParam(name = "userToken", required = false)
      final String userToken) {

    return this.authService.handleVerificationRequest(verificationToken, userToken);
  }

  @RequestMapping(
      method = RequestMethod.POST,
      path = "/register")
  public void registerParticipant(
      @RequestBody
      @Valid
      final RegistrationDto registration) throws IOException {

    this.authService.registerParticipant(registration, true);
  }

  @RequestMapping(
      method = RequestMethod.POST,
      path = "/devicetoken")
  public void registerDeviceToken(
      @RequestBody
      @Valid
      final DeviceTokenDto deviceTokenDto,
      @ApiIgnore
      final Authentication authentication) {

    this.authService.registerDeviceToken(authentication.getName(), deviceTokenDto.getDeviceToken());
  }

  @RequestMapping(
      method = RequestMethod.POST,
      path = "/register/import",
      consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public void importParticipants(final InputStream inputStream) throws IOException {

    this.authService.importParticipants(inputStream);
  }
}
