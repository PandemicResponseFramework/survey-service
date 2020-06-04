/**
 *
 */
package one.tracking.framework.service;

import static one.tracking.framework.entity.DataConstants.TOKEN_CONFIRM_LENGTH;
import static one.tracking.framework.entity.DataConstants.TOKEN_VERIFY_LENGTH;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import one.tracking.framework.config.TimeoutConfig;
import one.tracking.framework.dto.RegistrationDto;
import one.tracking.framework.dto.VerificationDto;
import one.tracking.framework.entity.DeviceToken;
import one.tracking.framework.entity.User;
import one.tracking.framework.entity.Verification;
import one.tracking.framework.repo.DeviceTokenRepository;
import one.tracking.framework.repo.UserRepository;
import one.tracking.framework.repo.VerificationRepository;
import one.tracking.framework.util.JWTHelper;

/**
 * @author Marko Voß
 *
 */
@Service
public class AuthService {

  private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private VerificationRepository verificationRepository;

  @Autowired
  private DeviceTokenRepository deviceTokenRepository;

  @Autowired
  private SendGridService emailService;

  @Autowired
  private ServiceUtility utility;

  @Autowired
  private TemplateEngine templateEngine;

  @Autowired
  private JWTHelper jwtHelper;

  @Autowired
  private TimeoutConfig timeoutConfig;

  @Value("${app.public.url}")
  private String publicUrl;

  @Value("${app.custom.uri.prefix}")
  private String customUriPrefix;

  private UriComponentsBuilder publicUrlBuilder;

  @PostConstruct
  public void init() {

    // throws IllegalArgumentException on invalid URIs -> startup will fail if URL is invalid
    this.publicUrlBuilder = UriComponentsBuilder.fromUriString(this.publicUrl);
  }

  public String verifyEmail(final VerificationDto verificationDto) throws IOException {

    final Optional<Verification> verificationOp =
        this.verificationRepository.findByHashAndVerified(verificationDto.getVerificationToken(), false);

    if (verificationOp.isEmpty())
      throw new IllegalArgumentException();

    final Verification verification = verificationOp.get();

    // Validate if verification is still valid

    final Instant instant =
        verification.getUpdatedAt() == null ? verification.getCreatedAt() : verification.getUpdatedAt();
    if (instant.plusSeconds(this.timeoutConfig.getVerification().toSeconds()).isBefore(Instant.now())) {

      LOG.info("Expired email verification requested.");
      throw new IllegalArgumentException(); // keep silent about it
    }

    // Update verification - do not delete hash to avoid other users receiving the same hash later
    verification.setVerified(true);
    verification.setUpdatedAt(Instant.now());
    this.verificationRepository.save(verification);

    final Optional<User> userOp = this.userRepository.findByUserToken(verificationDto.getConfirmationToken());
    User user = null;

    final String newUserToken = getValidConfirmationToken();

    if (userOp.isEmpty()) {
      // Generate new User ID
      user = this.userRepository.save(User.builder().userToken(newUserToken).build());
    } else {
      final User existingUser = userOp.get();
      existingUser.setUserToken(newUserToken);
      user = this.userRepository.save(existingUser);
    }

    sendConfirmationEmail(verification.getEmail(), user.getUserToken());

    return this.jwtHelper.createJWT(user.getId(), this.timeoutConfig.getAccess().toSeconds());
  }

  public void registerParticipant(final RegistrationDto registration, final boolean autoUpdateInvitation)
      throws IOException {

    final String verificationToken = getValidVerificationToken();

    final Optional<Verification> verificationOp = this.verificationRepository.findByEmail(registration.getEmail());

    boolean continueInivitation = false;

    if (verificationOp.isEmpty()) {
      // add new entity
      final Verification verification = Verification.builder()
          .email(registration.getEmail())
          .hash(verificationToken)
          .verified(false)
          .build();
      this.verificationRepository.save(verification);
      continueInivitation = true;

    } else if (autoUpdateInvitation) {
      // update entity
      final Verification verification = verificationOp.get();
      verification.setUpdatedAt(Instant.now());
      verification.setVerified(false);
      verification.setHash(verificationToken);
      this.verificationRepository.save(verification);
      continueInivitation = true;
    }

    if (continueInivitation)
      sendRegistrationEmail(registration.getEmail(), verificationToken, registration.getConfirmationToken());
  }

  public void importParticipants(final InputStream inputStream) throws IOException {

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

      reader.lines().forEach(line -> {
        try {
          registerParticipant(RegistrationDto.builder().email(line).build(), false);
        } catch (final IOException e) {
          LOG.error("IMPORT: Unable to register participant: {}", line);
        }
      });
    }
  }

  public String handleVerificationRequest(final String verificationToken, final String userToken) {

    final String path =
        userToken == null || userToken.isBlank() ? verificationToken : verificationToken + "/" + userToken;

    final Context context = new Context();
    context.setVariable("customURI", this.customUriPrefix + "://verify/" + path);

    return this.templateEngine.process("verifyTemplate", context);
  }

  private String getValidConfirmationToken() {

    final String hash = this.utility.generateString(TOKEN_CONFIRM_LENGTH);
    if (this.userRepository.existsByUserToken(hash))
      return getValidConfirmationToken(); // repeat

    return hash;
  }

  /**
   *
   * @return
   */
  private String getValidVerificationToken() {

    final String hash = this.utility.generateString(TOKEN_VERIFY_LENGTH);
    if (this.verificationRepository.existsByHash(hash))
      return getValidVerificationToken(); // repeat

    return hash;
  }

  /**
   * @param email
   * @param verificationToken
   * @throws IOException
   */
  private void sendRegistrationEmail(final String email, final String verificationToken, final String userToken)
      throws IOException {

    final UriComponentsBuilder builder = this.publicUrlBuilder.cloneBuilder()
        .path("/auth/verify")
        .queryParam("token", verificationToken);

    if (userToken != null && !userToken.isBlank())
      builder.queryParam("userToken", userToken);

    final String publicLink = builder
        .build()
        .encode()
        .toString();

    LOG.debug("Sending email to '{}' with verification link: '{}'", email, publicLink);

    final Context context = new Context();
    context.setVariable("link", publicLink);
    final String message = this.templateEngine.process("registrationTemplate", context);
    final boolean success = this.emailService.sendHTML(email, "Registration", message);

    if (!success)
      throw new IOException("Sending email to recipient '" + email + "' was not successful.");
  }

  /**
   *
   * @param email
   * @param hash
   * @throws IOException
   */
  private void sendConfirmationEmail(final String email, final String hash) throws IOException {

    LOG.debug("Sending email to '{}' with confirmation token: '{}'", email, hash);

    final Context context = new Context();
    context.setVariable("token", hash);
    final String message = this.templateEngine.process("confirmationTemplate", context);
    final boolean success = this.emailService.sendHTML(email, "Confirmation", message);

    if (!success)
      throw new IOException("Sending email to recipient '" + email + "' was not successful.");
  }

  /**
   * @param name
   * @param deviceToken
   */
  public void registerDeviceToken(final String userId, final String deviceToken) {

    final User user = this.userRepository.findById(userId).get();

    final Optional<DeviceToken> deviceTokenOp = this.deviceTokenRepository.findByUserAndToken(user, deviceToken);

    if (deviceTokenOp.isPresent())
      return;

    this.deviceTokenRepository.save(DeviceToken.builder()
        .user(user)
        .token(deviceToken)
        .build());
  }
}
