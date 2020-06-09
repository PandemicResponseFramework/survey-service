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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;
import one.tracking.framework.SurveyApplication;
import one.tracking.framework.dto.SurveyResponseConflictDto;
import one.tracking.framework.dto.SurveyResponseConflictType;
import one.tracking.framework.dto.SurveyResponseDto;
import one.tracking.framework.dto.SurveyStatusDto;
import one.tracking.framework.dto.SurveyStatusType;
import one.tracking.framework.dto.meta.AnswerDto;
import one.tracking.framework.dto.meta.SurveyDto;
import one.tracking.framework.dto.meta.question.BooleanQuestionDto;
import one.tracking.framework.dto.meta.question.ChecklistEntryDto;
import one.tracking.framework.dto.meta.question.ChecklistQuestionDto;
import one.tracking.framework.dto.meta.question.ChoiceQuestionDto;
import one.tracking.framework.dto.meta.question.NumberQuestionDto;
import one.tracking.framework.dto.meta.question.QuestionDto;
import one.tracking.framework.dto.meta.question.RangeQuestionDto;
import one.tracking.framework.dto.meta.question.TextQuestionDto;
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
@TestPropertySource(locations = "classpath:application-it.properties")
@Import(ITConfiguration.class)
@RunWith(SpringRunner.class)
// @SpringBootTest(classes = SurveyApplication.class, webEnvironment =
// SpringBootTest.WebEnvironment.DEFINED_PORT)
@SpringBootTest(classes = SurveyApplication.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class SurveyIT {

  private static final Logger LOG = LoggerFactory.getLogger(SurveyIT.class);

  private static final String ENDPOINT_OVERVIEW = "/overview";
  // private static final String ENDPOINT_OVERVIEW_TEST = ENDPOINT_OVERVIEW + "/TEST";

  private static final String ENDPOINT_SURVEY = "/survey";
  private static final String ENDPOINT_SURVEY_TEST = ENDPOINT_SURVEY + "/TEST";
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
  public void testSurveyExecution() throws Exception {

    this.helper.createSurvey("TEST");

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

    LOG.info("TEST SURVEY: {}", result.getResponse().getContentAsString());

    final SurveyDto survey = this.mapper.readValue(result.getResponse().getContentAsByteArray(), SurveyDto.class);

    assertThat(survey.getNameId(), is("TEST"));
    assertThat(survey.getVersion(), is(0));
    assertThat(survey.getTitle(), is("TITLE"));
    assertThat(survey.getDescription(), is("DESCRIPTION"));
    assertThat(survey.getQuestions().size(), is(11));

    performSurvey(survey, token);
  }

  private String testOverview(
      final SurveyStatusType expectedStatus,
      final Long expectedNextQuestionId) throws Exception {

    return testOverview(expectedStatus, expectedNextQuestionId, null);
  }

  private String testOverview(
      final SurveyStatusType expectedStatus,
      final Long expectedNextQuestionId,
      final String expectedDependsOnNameId) throws Exception {

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
    assertThat(status.getTitle(), is("TITLE"));
    assertThat(status.getDescription(), is("DESCRIPTION"));
    assertThat(status.getCountQuestions(), is(11));

    if (expectedNextQuestionId == null)
      assertThat(status.getNextQuestionId(), is(nullValue()));
    else
      assertThat(status.getNextQuestionId(), is(expectedNextQuestionId));

    assertThat(status.getStatus(), is(expectedStatus));
    assertThat(status.getDependsOn(), is(expectedDependsOnNameId));
    assertThat(status.getToken(), is(not(nullValue())));

    return status.getToken();
  }

  private void performSurvey(final SurveyDto survey, final String surveyToken) throws Exception {

    /*
     * We do iterate through the list of questions in order to test the correct order of the question.
     * If specific questions need to be selected, we can use the getQuestion(List<Question>, String)
     * method.
     */

    final Iterator<QuestionDto> iterator = survey.getQuestions().iterator();

    /*
     * Q1 - boolean - no children
     */

    QuestionDto question = iterator.next();

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(BooleanQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q1"));
    assertThat(question.getOrder(), is(0));
    assertThat(question.getOptional(), is(true));
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
            .numberAnswer(1)
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

    QuestionDto nextQuestion = getQuestion(survey.getQuestions(), "Q2");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q2"));

    testOverview(SurveyStatusType.INCOMPLETE, nextQuestion.getId());

    /*
     * Q2 - boolean - with children
     */

    question = iterator.next();

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(BooleanQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q2"));
    assertThat(question.getOrder(), is(1));
    assertThat(question.getOptional(), is(true));
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

    nextQuestion = getQuestion(survey.getQuestions(), "Q2C1");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q2C1"));

    testOverview(SurveyStatusType.INCOMPLETE, nextQuestion.getId());

    /*
     * Q2C1
     */

    question = ((BooleanQuestionDto) question).getContainer().getSubQuestions().get(0);

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(BooleanQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q2C1"));
    assertThat(question.getOrder(), is(0));
    assertThat(question.getOptional(), is(true));
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

    nextQuestion = getQuestion(survey.getQuestions(), "Q3");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q3"));

    testOverview(SurveyStatusType.INCOMPLETE, nextQuestion.getId());

    /*
     * Q3 - single choice - no children
     */

    question = iterator.next();

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(ChoiceQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q3"));
    assertThat(question.getOrder(), is(2));
    assertThat(question.getOptional(), is(true));

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

    nextQuestion = getQuestion(survey.getQuestions(), "Q4");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q4"));

    testOverview(SurveyStatusType.INCOMPLETE, nextQuestion.getId());

    /*
     * Q4 - multiple choice - no children
     */

    question = iterator.next();

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(ChoiceQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q4"));
    assertThat(question.getOrder(), is(3));
    assertThat(question.getOptional(), is(true));

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

    nextQuestion = getQuestion(survey.getQuestions(), "Q5");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q5"));

    testOverview(SurveyStatusType.INCOMPLETE, nextQuestion.getId());

    /*
     * Q5 - single choice - with children
     */

    question = iterator.next();

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(ChoiceQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q5"));
    assertThat(question.getOrder(), is(4));
    assertThat(question.getOptional(), is(true));

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

    nextQuestion = getQuestion(survey.getQuestions(), "Q5C1");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q5C1"));

    testOverview(SurveyStatusType.INCOMPLETE, nextQuestion.getId());

    /*
     * Q5C1
     */

    question = choiceQuestion.getContainer().getSubQuestions().get(0);

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(BooleanQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q5C1"));
    assertThat(question.getOrder(), is(0));
    assertThat(question.getOptional(), is(true));
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

    nextQuestion = getQuestion(survey.getQuestions(), "Q6");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q6"));

    testOverview(SurveyStatusType.INCOMPLETE, nextQuestion.getId());

    /*
     * Q6 - multiple choice - with children
     */

    question = iterator.next();

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(ChoiceQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q6"));
    assertThat(question.getOrder(), is(5));
    assertThat(question.getOptional(), is(true));

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

    nextQuestion = getQuestion(survey.getQuestions(), "Q6C1");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q6C1"));

    testOverview(SurveyStatusType.INCOMPLETE, nextQuestion.getId());

    /*
     * Q6C1
     */

    question = choiceQuestion.getContainer().getSubQuestions().get(0);

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(BooleanQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q6C1"));
    assertThat(question.getOrder(), is(0));
    assertThat(question.getOptional(), is(true));
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

    nextQuestion = getQuestion(survey.getQuestions(), "Q7");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q7"));

    testOverview(SurveyStatusType.INCOMPLETE, nextQuestion.getId());

    /*
     * Q7 - checklist
     */

    question = iterator.next();

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(ChecklistQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q7"));
    assertThat(question.getOrder(), is(6));
    assertThat(question.getOptional(), is(true));

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

    nextQuestion = getQuestion(survey.getQuestions(), "Q8");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q8"));

    testOverview(SurveyStatusType.INCOMPLETE, nextQuestion.getId());

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

    testOverview(SurveyStatusType.INCOMPLETE, nextQuestion.getId());

    // Sending an entry of the checklist as a separated answer must fail
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

    testOverview(SurveyStatusType.INCOMPLETE, nextQuestion.getId());

    /*
     * Q8 - Range - no children
     */

    question = iterator.next();

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(RangeQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q8"));
    assertThat(question.getOrder(), is(7));
    assertThat(question.getOptional(), is(true));

    final RangeQuestionDto rangeQuestion = (RangeQuestionDto) question;

    assertThat(rangeQuestion.getDefaultValue(), is(5));
    assertThat(rangeQuestion.getMaxText(), is("Q8MAX"));
    assertThat(rangeQuestion.getMinText(), is("Q8MIN"));
    assertThat(rangeQuestion.getMaxValue(), is(10));
    assertThat(rangeQuestion.getMinValue(), is(1));

    // Test maxValue violation
    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .numberAnswer(11)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    // Test minValue violation
    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .numberAnswer(0)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .numberAnswer(4)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    nextQuestion = getQuestion(survey.getQuestions(), "Q9");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q9"));

    testOverview(SurveyStatusType.INCOMPLETE, nextQuestion.getId());

    /*
     * Q9 - TextField - no children
     */

    question = iterator.next();

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(TextQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q9"));
    assertThat(question.getOrder(), is(8));
    assertThat(question.getOptional(), is(true));

    TextQuestionDto textQuestion = (TextQuestionDto) question;

    assertThat(textQuestion.getLength(), is(256));

    // Test empty string violation
    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .textAnswer("")
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    // Test blank string violation
    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .textAnswer(" ")
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    // Test max length violation
    String generatedString = generateText(257);
    assertThat(generatedString.length(), is(257));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .textAnswer(generatedString)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    // Test max length
    generatedString = generateText(256);
    assertThat(generatedString.length(), is(256));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .textAnswer(generatedString)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    nextQuestion = getQuestion(survey.getQuestions(), "Q10");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q10"));

    testOverview(SurveyStatusType.INCOMPLETE, nextQuestion.getId());

    /*
     * Q10 - TextArea - no children
     */

    question = iterator.next();

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(TextQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q10"));
    assertThat(question.getOrder(), is(9));
    assertThat(question.getOptional(), is(true));

    textQuestion = (TextQuestionDto) question;

    assertThat(textQuestion.getLength(), is(512));

    // Test empty string violation
    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .textAnswer("")
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    // Test blank string violation
    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .textAnswer(" ")
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    // Test max length violation
    generatedString = generateText(513);
    assertThat(generatedString.length(), is(513));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .textAnswer(generatedString)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    // Test max length
    generatedString = generateText(512);
    assertThat(generatedString.length(), is(512));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .textAnswer(generatedString)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    nextQuestion = getQuestion(survey.getQuestions(), "Q11");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q11"));

    testOverview(SurveyStatusType.INCOMPLETE, nextQuestion.getId());

    /*
     * Q11 - Number - no children
     */

    question = iterator.next();

    assertThat(question, is(not(nullValue())));
    assertThat(question, is(instanceOf(NumberQuestionDto.class)));
    assertThat(question.getQuestion(), is("Q11"));
    assertThat(question.getOrder(), is(10));
    assertThat(question.getOptional(), is(true));

    final NumberQuestionDto numberQuestion = (NumberQuestionDto) question;

    assertThat(numberQuestion.getMinValue(), is(0));
    assertThat(numberQuestion.getMaxValue(), is(10));
    assertThat(numberQuestion.getDefaultValue(), is(5));

    // Test violation of minimum value
    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .numberAnswer(-1)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    // Test violation of maximum value
    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .numberAnswer(11)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    // Test violation of null value
    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .numberAnswer(4)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.COMPLETED, null);

    /*
     * Survey completed successfully. Now let us change answers and check the behavior. Special behavior
     * of questions containing child questions: If parent question gets responded to again, child
     * question(s) must be answered again.
     */
    // The following questions contain children: Q2, Q5, Q6, Q9, Q11, Q13 so let us redo them

    /*
     * Q2 - boolean - with children
     */

    question = getQuestion(survey.getQuestions(), "Q2");
    assertThat(question, is(not(nullValue())));
    assertThat(question.getQuestion(), is("Q2"));

    // Answer = false -> Child question not required -> SurveyStatusType = COMPLETED
    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .boolAnswer(false)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    nextQuestion = getQuestion(survey.getQuestions(), "Q3");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q3"));

    testOverview(SurveyStatusType.COMPLETED, nextQuestion.getId());

    // Answer = true -> Child question required -> SurveyStatusType = INCOMPLETE
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

    nextQuestion = getQuestion(survey.getQuestions(), "Q2C1");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q2C1"));

    testOverview(SurveyStatusType.INCOMPLETE, nextQuestion.getId());

    /*
     * Q2C1
     */
    question = ((BooleanQuestionDto) question).getContainer().getSubQuestions().get(0);
    assertThat(question, is(not(nullValue())));
    assertThat(question.getQuestion(), is("Q2C1"));

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

    nextQuestion = getQuestion(survey.getQuestions(), "Q3");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q3"));

    testOverview(SurveyStatusType.COMPLETED, nextQuestion.getId());

    /*
     * Q5 - single choice - with children
     */

    question = getQuestion(survey.getQuestions(), "Q5");
    assertThat(question, is(not(nullValue())));
    assertThat(question.getQuestion(), is("Q5"));

    choiceQuestion = (ChoiceQuestionDto) question;

    // Child question depends on answers Q5A1 OR Q5A2 -> answer Q5A3 -> SurveyStatusType = COMPLETED
    AnswerDto answer = choiceQuestion.getAnswers().get(2);
    assertThat(answer.getValue(), is("Q5A3"));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .answerIds(Arrays.asList(
                answer.getId()))
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    nextQuestion = getQuestion(survey.getQuestions(), "Q6");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q6"));

    testOverview(SurveyStatusType.COMPLETED, nextQuestion.getId());

    // Child question depends on answers Q5A1 OR Q5A2 -> answer Q5A2 -> SurveyStatusType = INCOMPLETE
    answer = choiceQuestion.getAnswers().get(1);
    assertThat(answer.getValue(), is("Q5A2"));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .answerIds(Arrays.asList(
                answer.getId()))
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    nextQuestion = getQuestion(survey.getQuestions(), "Q5C1");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q5C1"));

    testOverview(SurveyStatusType.INCOMPLETE, nextQuestion.getId());

    /*
     * Q5C1
     */

    question = choiceQuestion.getContainer().getSubQuestions().get(0);
    assertThat(question, is(not(nullValue())));
    assertThat(question.getQuestion(), is("Q5C1"));

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

    nextQuestion = getQuestion(survey.getQuestions(), "Q6");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q6"));

    testOverview(SurveyStatusType.COMPLETED, nextQuestion.getId());

    /*
     * Q6 - multiple choice - with children
     */

    question = getQuestion(survey.getQuestions(), "Q6");
    assertThat(question, is(not(nullValue())));
    assertThat(question.getQuestion(), is("Q6"));

    choiceQuestion = (ChoiceQuestionDto) question;

    // Child question depends on answers Q6A1 OR Q6A2 -> answer Q6A3 -> SurveyStatusType = COMPLETED
    answer = choiceQuestion.getAnswers().get(2);
    assertThat(answer.getValue(), is("Q6A3"));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .answerIds(Arrays.asList(
                answer.getId()))
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    nextQuestion = getQuestion(survey.getQuestions(), "Q7");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q7"));

    testOverview(SurveyStatusType.COMPLETED, nextQuestion.getId());

    // Child question depends on answers Q6A1 OR Q6A2 -> answer Q6A2 -> SurveyStatusType = INCOMPLETE
    answer = choiceQuestion.getAnswers().get(1);
    assertThat(answer.getValue(), is("Q6A2"));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .answerIds(Arrays.asList(
                answer.getId()))
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    nextQuestion = getQuestion(survey.getQuestions(), "Q6C1");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q6C1"));

    testOverview(SurveyStatusType.INCOMPLETE, nextQuestion.getId());

    /*
     * Q6C1
     */

    question = choiceQuestion.getContainer().getSubQuestions().get(0);
    assertThat(question, is(not(nullValue())));
    assertThat(question.getQuestion(), is("Q6C1"));

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

    nextQuestion = getQuestion(survey.getQuestions(), "Q7");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q7"));

    testOverview(SurveyStatusType.COMPLETED, nextQuestion.getId());

    /*
     * Now test setting all questions to SKIPPED and check if overview stays valid
     */

    question = getQuestion(survey.getQuestions(), "Q1");
    assertThat(question, is(not(nullValue())));
    assertThat(question.getQuestion(), is("Q1"));

    nextQuestion = getQuestion(survey.getQuestions(), "Q2");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q2"));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .skipped(true)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.COMPLETED, nextQuestion.getId());

    question = nextQuestion;

    nextQuestion = getQuestion(survey.getQuestions(), "Q3");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q3"));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .skipped(true)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.COMPLETED, nextQuestion.getId());

    question = nextQuestion;

    nextQuestion = getQuestion(survey.getQuestions(), "Q4");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q4"));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .skipped(true)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.COMPLETED, nextQuestion.getId());

    question = nextQuestion;

    nextQuestion = getQuestion(survey.getQuestions(), "Q5");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q5"));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .skipped(true)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.COMPLETED, nextQuestion.getId());

    question = nextQuestion;

    nextQuestion = getQuestion(survey.getQuestions(), "Q6");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q6"));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .skipped(true)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.COMPLETED, nextQuestion.getId());

    question = nextQuestion;

    nextQuestion = getQuestion(survey.getQuestions(), "Q7");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q7"));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .skipped(true)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.COMPLETED, nextQuestion.getId());

    question = nextQuestion;

    nextQuestion = getQuestion(survey.getQuestions(), "Q8");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q8"));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .skipped(true)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.COMPLETED, nextQuestion.getId());

    question = nextQuestion;

    nextQuestion = getQuestion(survey.getQuestions(), "Q9");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q9"));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .skipped(true)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.COMPLETED, nextQuestion.getId());

    question = nextQuestion;

    nextQuestion = getQuestion(survey.getQuestions(), "Q10");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q10"));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .skipped(true)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.COMPLETED, nextQuestion.getId());

    question = nextQuestion;

    nextQuestion = getQuestion(survey.getQuestions(), "Q11");
    assertThat(nextQuestion, is(not(nullValue())));
    assertThat(nextQuestion.getQuestion(), is("Q11"));

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .skipped(true)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.COMPLETED, nextQuestion.getId());

    question = nextQuestion;

    nextQuestion = null;

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY_TEST_ANSWER)
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(question.getId())
            .skipped(true)
            .surveyToken(surveyToken)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    testOverview(SurveyStatusType.COMPLETED, null);
  }

  @Test
  public void testSurveyDependency() throws Exception {

    this.helper.createSimpleSurvey("NEXT", true, this.helper.createSimpleSurvey("PREVIOUS", false));

    /*
     * Test overview
     */

    MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_OVERVIEW)
        .with(csrf())
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    final List<SurveyStatusDto> statusList = this.mapper.readValue(result.getResponse().getContentAsByteArray(),
        this.mapper.getTypeFactory().constructCollectionType(List.class, SurveyStatusDto.class));

    // Currently ordered by nameId ASC
    assertThat(statusList.size(), is(2));

    SurveyStatusDto status = statusList.get(0);

    assertThat(status, is(not(nullValue())));
    assertThat(status.getNameId(), is("NEXT"));
    assertThat(status.getTitle(), is("TITLE"));
    assertThat(status.getDescription(), is("DESCRIPTION"));
    assertThat(status.getCountQuestions(), is(1));
    assertThat(status.getNextQuestionId(), is(nullValue()));
    assertThat(status.getStatus(), is(SurveyStatusType.INCOMPLETE));
    assertThat(status.getDependsOn(), is("PREVIOUS"));
    assertThat(status.getToken(), is(not(nullValue())));

    final String surveyTokenNext = status.getToken();

    status = statusList.get(1);

    assertThat(status, is(not(nullValue())));
    assertThat(status.getNameId(), is("PREVIOUS"));
    assertThat(status.getTitle(), is("TITLE"));
    assertThat(status.getDescription(), is("DESCRIPTION"));
    assertThat(status.getCountQuestions(), is(1));
    assertThat(status.getNextQuestionId(), is(nullValue()));
    assertThat(status.getStatus(), is(SurveyStatusType.INCOMPLETE));
    assertThat(status.getDependsOn(), is(nullValue()));
    assertThat(status.getToken(), is(not(nullValue())));

    final String surveyTokenPrevious = status.getToken();

    /*
     * Test survey meta data
     */

    result = this.mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_SURVEY + "/PREVIOUS")
        .with(csrf())
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    final SurveyDto surveyPrevious =
        this.mapper.readValue(result.getResponse().getContentAsByteArray(), SurveyDto.class);

    assertThat(surveyPrevious.getNameId(), is("PREVIOUS"));
    assertThat(surveyPrevious.getVersion(), is(0));
    assertThat(surveyPrevious.getTitle(), is("TITLE"));
    assertThat(surveyPrevious.getDescription(), is("DESCRIPTION"));
    assertThat(surveyPrevious.getQuestions().size(), is(1));
    assertThat(surveyPrevious.getDependsOn(), is(nullValue()));

    result = this.mockMvc.perform(MockMvcRequestBuilders.get(ENDPOINT_SURVEY + "/NEXT")
        .with(csrf())
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    final SurveyDto surveyNext = this.mapper.readValue(result.getResponse().getContentAsByteArray(), SurveyDto.class);

    assertThat(surveyNext.getNameId(), is("NEXT"));
    assertThat(surveyNext.getVersion(), is(0));
    assertThat(surveyNext.getTitle(), is("TITLE"));
    assertThat(surveyNext.getDescription(), is("DESCRIPTION"));
    assertThat(surveyNext.getQuestions().size(), is(1));
    assertThat(surveyNext.getDependsOn(), is("PREVIOUS"));

    final QuestionDto questionPrevious = surveyPrevious.getQuestions().get(0);
    final QuestionDto questionNext = surveyNext.getQuestions().get(0);

    /*
     * TEST send response to NEXT survey
     */
    result = this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY + "/NEXT/answer")
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(questionNext.getId())
            .boolAnswer(true)
            .surveyToken(surveyTokenNext)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isConflict())
        .andReturn();

    SurveyResponseConflictDto conflict =
        this.mapper.readValue(result.getResponse().getContentAsByteArray(), SurveyResponseConflictDto.class);

    assertThat(conflict, is(not(nullValue())));
    assertThat(conflict.getConflictType(), is(not(nullValue())));
    assertThat(conflict.getConflictType(), is(SurveyResponseConflictType.UNSATISFIED_DEPENDENCY));

    /*
     * TEST invalid survey token
     */
    result = this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY + "/PREVIOUS/answer")
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(questionPrevious.getId())
            .boolAnswer(true)
            .surveyToken("INVALID")
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isConflict())
        .andReturn();

    conflict = this.mapper.readValue(result.getResponse().getContentAsByteArray(), SurveyResponseConflictDto.class);

    assertThat(conflict, is(not(nullValue())));
    assertThat(conflict.getConflictType(), is(not(nullValue())));
    assertThat(conflict.getConflictType(), is(SurveyResponseConflictType.INVALID_SURVEY_TOKEN));

    /*
     * TEST invalid question id
     */

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY + "/PREVIOUS/answer")
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(questionNext.getId())
            .boolAnswer(true)
            .surveyToken(surveyTokenPrevious)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    /*
     * Complete previous survey and TEST response to NEXT survey again
     */

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY + "/PREVIOUS/answer")
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(questionPrevious.getId())
            .boolAnswer(true)
            .surveyToken(surveyTokenPrevious)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    this.mockMvc.perform(MockMvcRequestBuilders.post(ENDPOINT_SURVEY + "/NEXT/answer")
        .with(csrf())
        .content(this.mapper.writeValueAsBytes(SurveyResponseDto.builder()
            .questionId(questionNext.getId())
            .boolAnswer(true)
            .surveyToken(surveyTokenNext)
            .build()))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  /**
   * Generates a {@link String} of the specified length.
   *
   * @param length the length of the resulting {@link String}
   * @return the generated {@link String}
   */
  private String generateText(final int length) {
    return Stream.generate(() -> "A").limit(length).collect(Collectors.joining());
  }

  /**
   * Returns the {@link QuestionDto}, which owns the specified question text.
   *
   * @param questions the {@link List} of {@link QuestionDto} to search in
   * @param questionText the question text to search for
   * @return the {@link QuestionDto} found or <code>null</code> if no entry could be found, the
   *         specified {@link List} is <code>null</code> or empty or the specified question text is
   *         <code>null</code>
   */
  private QuestionDto getQuestion(final List<QuestionDto> questions, final String questionText) {

    if (questions == null || questions.isEmpty() || questionText == null)
      return null;

    for (final QuestionDto question : questions) {

      if (questionText.equals(question.getQuestion()))
        return question;

      final List<QuestionDto> subQuestions = question.getSubQuestions();
      if (subQuestions != null) {
        final QuestionDto result = getQuestion(subQuestions, questionText);
        if (result != null)
          return result;
      }
    }

    return null;
  }
}
