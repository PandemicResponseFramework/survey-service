/**
 *
 */
package one.tracking.framework.integration;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;
import one.tracking.framework.SurveyApplication;
import one.tracking.framework.dto.SurveyResponseDto;
import one.tracking.framework.dto.SurveyStatusDto;
import one.tracking.framework.dto.SurveyStatusType;
import one.tracking.framework.dto.meta.SurveyDto;
import one.tracking.framework.dto.meta.question.BooleanQuestionDto;
import one.tracking.framework.dto.meta.question.ChecklistEntryDto;
import one.tracking.framework.dto.meta.question.ChecklistQuestionDto;
import one.tracking.framework.dto.meta.question.ChoiceQuestionDto;
import one.tracking.framework.dto.meta.question.QuestionDto;
import one.tracking.framework.entity.User;
import one.tracking.framework.repo.UserRepository;
import one.tracking.framework.util.JWTHelper;

/**
 * TODO: Evaluation of persistence layer
 *
 * @author Marko Voß
 *
 */
@AutoConfigureMockMvc
@TestPropertySource(
    locations = "classpath:application-it.properties")
@Import(ITConfiguration.class)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SurveyApplication.class)
public class SurveyIT {

  private static final String ENDPOINT_OVERVIEW = "/survey";
  private static final String ENDPOINT_SURVEY_TEST = ENDPOINT_OVERVIEW + "/TEST";
  private static final String ENDPOINT_SURVEY_TEST_ANSWER = ENDPOINT_SURVEY_TEST + "/answer";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JWTHelper jwtHelper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private HelperBean helper;

  private User user;

  private String token;

  @Before
  public void beforeEach() {
    this.user = this.userRepository.save(User.builder().userToken("test").build());
    this.token = this.jwtHelper.createJWT(this.user.getId(), 24 * 60 * 60);
  }

  @Test
  public void test() throws Exception {

    this.helper.createTestSurvey();

    /*
     * Test overview
     */

    final String token = testOverview(SurveyStatusType.INCOMPLETE, null);

    /*
     * Test survey meta data
     */

    this.mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_SURVEY_TEST)
        .with(csrf())
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    final MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_SURVEY_TEST)
        .with(csrf())
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    final SurveyDto survey = this.mapper.readValue(result.getResponse().getContentAsByteArray(), SurveyDto.class);

    assertThat(survey.getNameId(), is("TEST"));
    assertThat(survey.getVersion(), is(0));
    assertThat(survey.getTitle(), is("TITLE"));
    assertThat(survey.getDescription(), is("DESCRIPTION"));
    assertThat(survey.getQuestions().size(), is(13));

    performSurvey(survey, token);
  }

  private String testOverview(final SurveyStatusType expectedStatus, final Long expectedLastQuestionId)
      throws Exception {

    this.mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_OVERVIEW)
        .with(csrf())
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    final MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_OVERVIEW)
        .with(csrf())
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    final List<SurveyStatusDto> statusList = this.mapper.readValue(result.getResponse().getContentAsByteArray(),
        this.mapper.getTypeFactory().constructCollectionType(List.class, SurveyStatusDto.class));

    assertThat(statusList.size(), is(1));

    final SurveyStatusDto status = statusList.get(0);

    assertThat(status, is(not(nullValue())));
    assertThat(status.getNameId(), is("TEST"));

    if (expectedLastQuestionId == null)
      assertThat(status.getLastQuestionId(), is(nullValue()));
    else
      assertThat(status.getLastQuestionId(), is(expectedLastQuestionId));

    assertThat(status.getStatus(), is(expectedStatus));
    assertThat(status.getToken(), is(not(nullValue())));

    return status.getToken();
  }

  private void performSurvey(final SurveyDto survey, final String surveyToken) throws Exception {

    final Iterator<QuestionDto> iterator = survey.getQuestions().iterator();

    /*
     * Q1 - boolean - no children
     */

    QuestionDto question = iterator.next();

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(BooleanQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q1"));
    assertThat(((BooleanQuestionDto) question).getDefaultAnswer(), is(nullValue()));
    assertThat(((BooleanQuestionDto) question).getContainer(), is(nullValue()));

    // Test security only once
    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .boolAnswer(true)
            .build()))
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    // Test missing survey token only once
    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .boolAnswer(true)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    // Test invalid answer type
    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .rangeAnswer(1)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .boolAnswer(true)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.INCOMPLETE, question.getId());

    /*
     * Q2 - boolean - with children
     */

    question = iterator.next();

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(BooleanQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q2"));
    assertThat(((BooleanQuestionDto) question).getDefaultAnswer(), is(nullValue()));

    assertThat(((BooleanQuestionDto) question).getContainer(), is(not(nullValue())));
    assertThat(((BooleanQuestionDto) question).getContainer().getBoolDependsOn(), is(true));
    assertThat(((BooleanQuestionDto) question).getContainer().getSubQuestions(), is(not(nullValue())));
    assertThat(((BooleanQuestionDto) question).getContainer().getSubQuestions().size(), is(1));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .boolAnswer(true)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.INCOMPLETE, question.getId());

    /*
     * Q2C1
     */

    question = ((BooleanQuestionDto) question).getContainer().getSubQuestions().get(0);

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(BooleanQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q2C1"));
    assertThat(((BooleanQuestionDto) question).getDefaultAnswer(), is(nullValue()));
    assertThat(((BooleanQuestionDto) question).getContainer(), is(nullValue()));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .boolAnswer(true)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.INCOMPLETE, question.getId());

    /*
     * Q3 - single choice - no children
     */

    question = iterator.next();

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(ChoiceQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q3"));

    ChoiceQuestionDto choiceQuestion = (ChoiceQuestionDto) question;

    assertThat(choiceQuestion.getDefaultAnswer(), is(nullValue()));
    assertThat(choiceQuestion.isMultiple(), is(false));
    assertThat(choiceQuestion.getContainer(), is(nullValue()));

    assertThat(choiceQuestion.getAnswers(), is(not(nullValue())));
    assertThat(choiceQuestion.getAnswers().size(), is(3));
    assertThat(choiceQuestion.getAnswers().get(0), is(not(nullValue())));
    assertThat(choiceQuestion.getAnswers().get(0).getValue(), is("Q3A1"));
    assertThat(choiceQuestion.getAnswers().get(1), is(not(nullValue())));
    assertThat(choiceQuestion.getAnswers().get(1).getValue(), is("Q3A2"));
    assertThat(choiceQuestion.getAnswers().get(2), is(not(nullValue())));
    assertThat(choiceQuestion.getAnswers().get(2).getValue(), is("Q3A3"));

    // Invalid answer for single choice
    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .answerIds(Arrays.asList(
                choiceQuestion.getAnswers().get(0).getId(),
                choiceQuestion.getAnswers().get(1).getId()))
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .answerIds(Collections.singletonList(choiceQuestion.getAnswers().get(0).getId()))
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.INCOMPLETE, question.getId());

    /*
     * Q4 - multiple choice - no children
     */

    question = iterator.next();

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(ChoiceQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q4"));

    choiceQuestion = (ChoiceQuestionDto) question;

    assertThat(choiceQuestion.getDefaultAnswer(), is(nullValue()));
    assertThat(choiceQuestion.isMultiple(), is(true));
    assertThat(choiceQuestion.getContainer(), is(nullValue()));

    assertThat(choiceQuestion.getAnswers(), is(not(nullValue())));
    assertThat(choiceQuestion.getAnswers().size(), is(3));
    assertThat(choiceQuestion.getAnswers().get(0), is(not(nullValue())));
    assertThat(choiceQuestion.getAnswers().get(0).getValue(), is("Q4A1"));
    assertThat(choiceQuestion.getAnswers().get(1), is(not(nullValue())));
    assertThat(choiceQuestion.getAnswers().get(1).getValue(), is("Q4A2"));
    assertThat(choiceQuestion.getAnswers().get(2), is(not(nullValue())));
    assertThat(choiceQuestion.getAnswers().get(2).getValue(), is("Q4A3"));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .answerIds(Arrays.asList(
                choiceQuestion.getAnswers().get(0).getId(),
                choiceQuestion.getAnswers().get(1).getId()))
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.INCOMPLETE, question.getId());

    /*
     * Q5 - single choice - with children
     */

    question = iterator.next();

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(ChoiceQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q5"));

    choiceQuestion = (ChoiceQuestionDto) question;

    assertThat(choiceQuestion.getDefaultAnswer(), is(nullValue()));
    assertThat(choiceQuestion.isMultiple(), is(false));

    assertThat(choiceQuestion.getAnswers(), is(not(nullValue())));
    assertThat(choiceQuestion.getAnswers().size(), is(3));
    assertThat(choiceQuestion.getAnswers().get(0), is(not(nullValue())));
    assertThat(choiceQuestion.getAnswers().get(0).getValue(), is("Q5A1"));
    assertThat(choiceQuestion.getAnswers().get(1), is(not(nullValue())));
    assertThat(choiceQuestion.getAnswers().get(1).getValue(), is("Q5A2"));
    assertThat(choiceQuestion.getAnswers().get(2), is(not(nullValue())));
    assertThat(choiceQuestion.getAnswers().get(2).getValue(), is("Q5A3"));

    assertThat(choiceQuestion.getContainer(), is(not(nullValue())));
    assertThat(choiceQuestion.getContainer().getChoiceDependsOn(), is(not(nullValue())));
    assertThat(choiceQuestion.getContainer().getChoiceDependsOn(), allOf(
        hasItem(choiceQuestion.getAnswers().get(0).getId()),
        hasItem(choiceQuestion.getAnswers().get(1).getId())));

    assertThat(choiceQuestion.getContainer().getSubQuestions(), is(not(nullValue())));
    assertThat(choiceQuestion.getContainer().getSubQuestions().size(), is(1));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .answerIds(Arrays.asList(
                choiceQuestion.getAnswers().get(0).getId()))
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.INCOMPLETE, question.getId());

    /*
     * Q5C1
     */

    question = choiceQuestion.getContainer().getSubQuestions().get(0);

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(BooleanQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q5C1"));
    assertThat(((BooleanQuestionDto) question).getDefaultAnswer(), is(nullValue()));
    assertThat(((BooleanQuestionDto) question).getContainer(), is(nullValue()));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .boolAnswer(true)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.INCOMPLETE, question.getId());

    /*
     * Q6 - multiple choice - with children
     */

    question = iterator.next();

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(ChoiceQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q6"));

    choiceQuestion = (ChoiceQuestionDto) question;

    assertThat(choiceQuestion.getDefaultAnswer(), is(nullValue()));
    assertThat(choiceQuestion.isMultiple(), is(true));

    assertThat(choiceQuestion.getAnswers(), is(not(nullValue())));
    assertThat(choiceQuestion.getAnswers().size(), is(3));
    assertThat(choiceQuestion.getAnswers().get(0), is(not(nullValue())));
    assertThat(choiceQuestion.getAnswers().get(0).getValue(), is("Q6A1"));
    assertThat(choiceQuestion.getAnswers().get(1), is(not(nullValue())));
    assertThat(choiceQuestion.getAnswers().get(1).getValue(), is("Q6A2"));
    assertThat(choiceQuestion.getAnswers().get(2), is(not(nullValue())));
    assertThat(choiceQuestion.getAnswers().get(2).getValue(), is("Q6A3"));

    assertThat(choiceQuestion.getContainer(), is(not(nullValue())));
    assertThat(choiceQuestion.getContainer().getChoiceDependsOn(), is(not(nullValue())));
    assertThat(choiceQuestion.getContainer().getChoiceDependsOn(), allOf(
        hasItem(choiceQuestion.getAnswers().get(0).getId()),
        hasItem(choiceQuestion.getAnswers().get(1).getId())));

    assertThat(choiceQuestion.getContainer().getSubQuestions(), is(not(nullValue())));
    assertThat(choiceQuestion.getContainer().getSubQuestions().size(), is(1));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .answerIds(Arrays.asList(
                choiceQuestion.getAnswers().get(0).getId(),
                choiceQuestion.getAnswers().get(1).getId()))
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.INCOMPLETE, question.getId());

    /*
     * Q6C1
     */

    question = choiceQuestion.getContainer().getSubQuestions().get(0);

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(BooleanQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q6C1"));
    assertThat(((BooleanQuestionDto) question).getDefaultAnswer(), is(nullValue()));
    assertThat(((BooleanQuestionDto) question).getContainer(), is(nullValue()));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .boolAnswer(true)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.INCOMPLETE, question.getId());

    /*
     * Q7 - checklist
     */

    question = iterator.next();

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(ChecklistQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q7"));

    final ChecklistQuestionDto checklistQuestionDto = (ChecklistQuestionDto) question;

    assertThat(checklistQuestionDto.getEntries(), is(not(nullValue())));
    assertThat(checklistQuestionDto.getEntries().size(), is(3));

    assertThat(checklistQuestionDto.getEntries().get(0), is(instanceOf(ChecklistEntryDto.class)));
    assertThat(checklistQuestionDto.getEntries().get(0).getQuestion(), is("Q7E1"));
    assertThat(checklistQuestionDto.getEntries().get(0).getDefaultAnswer(), is(nullValue()));

    assertThat(checklistQuestionDto.getEntries().get(1), is(instanceOf(ChecklistEntryDto.class)));
    assertThat(checklistQuestionDto.getEntries().get(1).getQuestion(), is("Q7E2"));
    assertThat(checklistQuestionDto.getEntries().get(1).getDefaultAnswer(), is(nullValue()));

    assertThat(checklistQuestionDto.getEntries().get(2), is(instanceOf(ChecklistEntryDto.class)));
    assertThat(checklistQuestionDto.getEntries().get(2).getQuestion(), is("Q7E3"));
    assertThat(checklistQuestionDto.getEntries().get(2).getDefaultAnswer(), is(nullValue()));

    // Test empty checklist -> all false
    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .checklistAnswer(Collections.emptyMap())
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.INCOMPLETE, question.getId());

    // Replace answer with some entries set to true and some to false
    // -> result should be Q7E1=true, Q7E2=false, Q7E3=false
    final Map<Long, Boolean> answers = new HashMap<>();
    answers.put(checklistQuestionDto.getEntries().get(0).getId(), true);
    answers.put(checklistQuestionDto.getEntries().get(1).getId(), false);

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .checklistAnswer(answers)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.INCOMPLETE, question.getId());

    // Sending an entry of the checklist as a separated answer should fail
    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(checklistQuestionDto.getEntries().get(0).getId())
            .boolAnswer(true)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }
}
