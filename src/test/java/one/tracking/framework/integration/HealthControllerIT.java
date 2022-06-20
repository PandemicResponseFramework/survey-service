/**
 *
 */
package one.tracking.framework.integration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;
import one.tracking.framework.SurveyApplication;
import one.tracking.framework.dto.StepCountDto;
import one.tracking.framework.entity.User;
import one.tracking.framework.entity.health.StepCount;
import one.tracking.framework.repo.StepCountRepository;
import one.tracking.framework.support.JWTHelper;

/**
 * @author Marko Voß
 *
 */
@AutoConfigureMockMvc
@Import(ITConfiguration.class)
@SpringBootTest(classes = SurveyApplication.class)
@TestPropertySource(locations = "classpath:application-it.properties")
@DirtiesContext
public class HealthControllerIT {

  // private static final Logger LOG = LoggerFactory.getLogger(HealthControllerIT.class);

  private static final String ENDPOINT_HEALTH = "/health";
  private static final String ENDPOINT_STEPCOUNT = ENDPOINT_HEALTH + "/stepcount";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private StepCountRepository stepCountRepository;

  @Autowired
  private HelperBean helperBean;

  @Autowired
  private JWTHelper jwtHelper;

  private User user;

  private String token;

  @BeforeEach
  public void before() {
    this.user = this.helperBean.createUser("test");
    this.token = this.jwtHelper.createJWT(this.user.getId(), 24 * 60 * 60);
  }

  @Test
  public void testStepCounter() throws Exception {

    // Test empty
    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_STEPCOUNT)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(StepCountDto.builder()
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    // Test invalid count
    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_STEPCOUNT)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(StepCountDto.builder()
            .count(-1)
            .startTime(Instant.parse("2020-05-01T00:00:00Z").toEpochMilli())
            .endTime(Instant.parse("2020-05-08T00:00:00Z").toEpochMilli())
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    // Test invalid start time
    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_STEPCOUNT)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(StepCountDto.builder()
            .count(100000)
            .startTime(-1L)
            .endTime(Instant.parse("2020-05-01T00:00:00Z").toEpochMilli())
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    // Test invalid end time
    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_STEPCOUNT)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(StepCountDto.builder()
            .count(100000)
            .startTime(Instant.parse("2020-05-01T00:00:00Z").toEpochMilli())
            .endTime(-1L)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    // Test start time > end time
    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_STEPCOUNT)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(StepCountDto.builder()
            .count(100000)
            .startTime(Instant.parse("2020-05-08T00:00:00Z").toEpochMilli())
            .endTime(Instant.parse("2020-05-01T00:00:00Z").toEpochMilli())
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    /*
     * Test same time
     */

    Instant startTime = Instant.parse("2020-05-01T00:00:00Z");
    Instant endTime = Instant.parse("2020-05-01T00:00:00Z");

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_STEPCOUNT)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(StepCountDto.builder()
            .count(10000)
            .startTime(startTime.toEpochMilli())
            .endTime(endTime.toEpochMilli())
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    Optional<StepCount> stepCountOp = this.stepCountRepository.findByUserAndStartTimeAndEndTime(this.user,
        startTime, endTime);

    assertThat(stepCountOp, is(not(nullValue())));
    assertThat(stepCountOp.isPresent(), is(true));
    assertThat(stepCountOp.get().getStepCount(), is(10000));
    assertThat(stepCountOp.get().getUser().getId(), is(this.user.getId()));
    assertThat(stepCountOp.get().getStartTime(), is(equalTo(startTime)));
    assertThat(stepCountOp.get().getEndTime(), is(equalTo(endTime)));
    assertThat(stepCountOp.get().getVersion(), is(0));

    // Test normal time

    startTime = Instant.parse("2020-05-01T00:00:00Z");
    endTime = Instant.parse("2020-05-08T00:00:00Z");

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_STEPCOUNT)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(StepCountDto.builder()
            .count(100000)
            .startTime(Instant.parse("2020-05-01T00:00:00Z").toEpochMilli())
            .endTime(Instant.parse("2020-05-08T00:00:00Z").toEpochMilli())
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    stepCountOp = this.stepCountRepository.findByUserAndStartTimeAndEndTime(this.user, startTime, endTime);

    assertThat(stepCountOp, is(not(nullValue())));
    assertThat(stepCountOp.isPresent(), is(true));
    assertThat(stepCountOp.get().getStepCount(), is(100000));
    assertThat(stepCountOp.get().getUser().getId(), is(this.user.getId()));
    assertThat(stepCountOp.get().getStartTime(), is(equalTo(startTime)));
    assertThat(stepCountOp.get().getEndTime(), is(equalTo(endTime)));
    assertThat(stepCountOp.get().getVersion(), is(0));

    // Test no overwrite (highest wins)

    startTime = Instant.parse("2020-05-01T00:00:00Z");
    endTime = Instant.parse("2020-05-08T00:00:00Z");

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_STEPCOUNT)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(StepCountDto.builder()
            .count(90000)
            .startTime(Instant.parse("2020-05-01T00:00:00Z").toEpochMilli())
            .endTime(Instant.parse("2020-05-08T00:00:00Z").toEpochMilli())
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    stepCountOp = this.stepCountRepository.findByUserAndStartTimeAndEndTime(this.user, startTime, endTime);

    assertThat(stepCountOp, is(not(nullValue())));
    assertThat(stepCountOp.isPresent(), is(true));
    assertThat(stepCountOp.get().getStepCount(), is(100000));
    assertThat(stepCountOp.get().getUser().getId(), is(this.user.getId()));
    assertThat(stepCountOp.get().getStartTime(), is(equalTo(startTime)));
    assertThat(stepCountOp.get().getEndTime(), is(equalTo(endTime)));
    assertThat(stepCountOp.get().getVersion(), is(0));

    // Test overwrite (highest wins)

    startTime = Instant.parse("2020-05-01T00:00:00Z");
    endTime = Instant.parse("2020-05-08T00:00:00Z");

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_STEPCOUNT)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(StepCountDto.builder()
            .count(110000)
            .startTime(Instant.parse("2020-05-01T00:00:00Z").toEpochMilli())
            .endTime(Instant.parse("2020-05-08T00:00:00Z").toEpochMilli())
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    stepCountOp = this.stepCountRepository.findByUserAndStartTimeAndEndTime(this.user, startTime, endTime);

    assertThat(stepCountOp, is(not(nullValue())));
    assertThat(stepCountOp.isPresent(), is(true));
    assertThat(stepCountOp.get().getStepCount(), is(110000));
    assertThat(stepCountOp.get().getUser().getId(), is(this.user.getId()));
    assertThat(stepCountOp.get().getStartTime(), is(equalTo(startTime)));
    assertThat(stepCountOp.get().getEndTime(), is(equalTo(endTime)));
    assertThat(stepCountOp.get().getVersion(), is(1));
  }
}
