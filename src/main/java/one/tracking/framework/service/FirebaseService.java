/**
 *
 */
package one.tracking.framework.service;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import one.tracking.framework.domain.NotificationParameter;
import one.tracking.framework.domain.PushNotificationRequest;

/**
 * @author Marko Voß
 *
 */
@Service
public class FirebaseService {

  private static final Logger LOG = LoggerFactory.getLogger(FirebaseService.class);

  @Autowired
  private ResourceLoader resourceLoader;

  @Value("${app.fcm-config:#{null}}")
  private String firebaseConfigPath;

  @PostConstruct
  public void initialize() {

    if (this.firebaseConfigPath == null) {
      LOG.warn("Firebase config file not set. Skipping FCM setup.");
      return;
    }

    try {

      final FirebaseOptions options = new FirebaseOptions.Builder()
          .setCredentials(GoogleCredentials.fromStream(
              this.resourceLoader.getResource(this.firebaseConfigPath).getInputStream()))
          .build();

      if (FirebaseApp.getApps().isEmpty()) {
        FirebaseApp.initializeApp(options);
        LOG.info("Firebase application has been initialized.");
      }

    } catch (final Exception e) {
      LOG.error(e.getMessage(), e);
    }
  }

  public boolean sendMessageToUser(final PushNotificationRequest request)
      throws InterruptedException, ExecutionException {

    if (FirebaseApp.getApps().isEmpty())
      return false;

    final Message message = prepareMessage(request);
    final String response = sendMessage(message);
    LOG.info("Push notification response: {}", response);
    return true;
  }

  private Message prepareMessage(final PushNotificationRequest request) {

    return getMessageBuilder(request)
        // .setTopic(request.getTopic())
        .setToken(request.getToken())
        .putAllData(request.getData())
        .build();
  }

  private String sendMessage(final Message message) throws InterruptedException, ExecutionException {

    return FirebaseMessaging.getInstance().sendAsync(message).get();
  }

  private AndroidConfig getAndroidConfig(final String topic) {

    return AndroidConfig.builder()
        .setTtl(Duration.ofMinutes(2).toMillis())
        .setCollapseKey(topic)
        .setPriority(AndroidConfig.Priority.HIGH)
        .setNotification(AndroidNotification.builder()
            .setSound(NotificationParameter.SOUND.getValue())
            .setColor(NotificationParameter.COLOR.getValue())
            .setTag(topic)
            .build())
        .build();
  }

  private ApnsConfig getApnsConfig(final String topic) {

    return ApnsConfig.builder()
        .setAps(Aps.builder()
            .setCategory(topic)
            .setThreadId(topic)
            .build())
        .build();
  }

  private Message.Builder getMessageBuilder(final PushNotificationRequest request) {

    final AndroidConfig androidConfig = getAndroidConfig(request.getTopic());
    final ApnsConfig apnsConfig = getApnsConfig(request.getTopic());

    return Message.builder()
        .setApnsConfig(apnsConfig)
        .setAndroidConfig(androidConfig)
        .setNotification(Notification.builder()
            .setTitle(request.getTitle())
            .setBody(request.getMessage())
            .build());
  }
}
