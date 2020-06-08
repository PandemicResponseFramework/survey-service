/**
 *
 */
package one.tracking.framework.service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import one.tracking.framework.domain.Period;
import one.tracking.framework.dto.SurveyStatusType;
import one.tracking.framework.entity.SurveyResponse;
import one.tracking.framework.entity.meta.Answer;
import one.tracking.framework.entity.meta.Survey;
import one.tracking.framework.entity.meta.question.BooleanQuestion;
import one.tracking.framework.entity.meta.question.ChecklistEntry;
import one.tracking.framework.entity.meta.question.ChecklistQuestion;
import one.tracking.framework.entity.meta.question.ChoiceQuestion;
import one.tracking.framework.entity.meta.question.Question;

/**
 * @author Marko Voß
 *
 */
@Component
public final class ServiceUtility {

  private static final Random RANDOM = new Random();

  /**
   *
   * @param length
   * @return
   */
  public String generateString(final int length) {
    final int leftLimit = 48; // numeral '0'
    final int rightLimit = 122; // letter 'z'

    final String generatedString = RANDOM.ints(leftLimit, rightLimit + 1)
        .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
        .limit(length)
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();

    return generatedString;
  }

  public Period getCurrentSurveyInstancePeriod(final Survey survey) {

    Assert.notNull(survey, "Survey must not be null.");

    switch (survey.getIntervalType()) {
      case WEEKLY:
        return getCurrentPeriodByWeek(survey);
      case NONE:
      default:
        return Period.INFINITE;
    }
  }

  /**
   * @param survey
   * @return
   */
  private Period getCurrentPeriodByWeek(final Survey survey) {

    final ZonedDateTime start = survey.getIntervalStart().atZone(ZoneOffset.UTC);
    final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

    if (start.isAfter(now))
      return null;

    final int weekStart = start.get(WeekFields.ISO.weekOfWeekBasedYear());
    final int weekNow = now.get(WeekFields.ISO.weekOfWeekBasedYear());

    final int weekDelta = (int) (Math.floor((weekNow - weekStart) / (double) survey.getIntervalValue()));

    final ZonedDateTime startTime = start.plusWeeks(weekDelta * survey.getIntervalValue());
    final ZonedDateTime endTime = startTime.plus(survey.getIntervalValue(), survey.getIntervalType().toChronoUnit())
        .minusSeconds(1);

    return new Period(startTime.toInstant(), endTime.toInstant());
  }

  public SurveyStatusType calculateSurveyStatus(final Survey survey, final List<SurveyResponse> surveyResponses) {

    final Map<Long, SurveyResponse> responses =
        surveyResponses.stream().collect(Collectors.toMap(e -> e.getQuestion().getId(), e -> e));

    if (responses.isEmpty())
      return SurveyStatusType.INCOMPLETE;

    if (checkAnswers(survey.getQuestions(), responses))
      return SurveyStatusType.COMPLETED;

    return SurveyStatusType.INCOMPLETE;
  }

  private boolean checkAnswers(final List<Question> questions, final Map<Long, SurveyResponse> responses) {

    if (questions == null || responses == null || responses.isEmpty())
      return false;

    for (final Question question : questions) {

      if (!isAnswered(question, responses))
        return false;

      if (isSubQuestionRequired(question, responses.get(question.getId()))) {
        if (!checkAnswers(getQuestions(question), responses))
          return false;
      }
    }

    return true;
  }

  private List<Question> getQuestions(final Question question) {

    switch (question.getType()) {
      case BOOL:
        return ((BooleanQuestion) question).getContainer().getQuestions();
      case CHOICE:
        return ((ChoiceQuestion) question).getContainer().getQuestions();
      default:
        return null;

    }
  }

  /**
   *
   * @param question
   * @param response
   * @return
   */
  private boolean isSubQuestionRequired(final Question question, final SurveyResponse response) {

    if (question == null || !question.hasContainer() || response == null || !response.isValid()
        || (question.isOptional() && response.isSkipped()))
      return false;

    switch (question.getType()) {
      case BOOL:

        final BooleanQuestion booleanQuestion = (BooleanQuestion) question;
        if (booleanQuestion.getContainer().getDependsOn() == null)
          return true;

        return response.getBoolAnswer().equals(booleanQuestion.getContainer().getDependsOn());

      case CHOICE:

        final ChoiceQuestion choiceQuestion = (ChoiceQuestion) question;
        if (choiceQuestion.getContainer().getDependsOn() == null)
          return true;

        final List<Long> givenAnswerIds =
            response.getAnswers().stream().map(Answer::getId).collect(Collectors.toList());

        // anyMatch -> OR-linked answers
        // allMatch -> AND-linked answers
        return choiceQuestion.getContainer().getDependsOn().stream().anyMatch(p -> givenAnswerIds.contains(p.getId()));

      default:
        return true;

    }
  }

  private boolean isAnswered(final Question question, final Map<Long, SurveyResponse> responses) {

    switch (question.getType()) {
      case BOOL: {

        final SurveyResponse response = responses.get(question.getId());
        return response != null && response.isValid()
            && (response.getBoolAnswer() != null || question.isOptional() && response.isSkipped());

      }
      case CHECKLIST: {

        final ChecklistQuestion checklistQuestion = (ChecklistQuestion) question;
        for (final ChecklistEntry entry : checklistQuestion.getEntries()) {

          final SurveyResponse response = responses.get(entry.getId());
          if (response == null || !response.isValid()
              || (!question.isOptional() && response.getBoolAnswer() == null)
              || (question.isOptional() && response.getBoolAnswer() == null && !response.isSkipped()))
            return false;
        }

        return true;

      }
      case CHOICE: {

        final ChoiceQuestion choiceQuestion = (ChoiceQuestion) question;
        final SurveyResponse response = responses.get(question.getId());

        if (response == null || !response.isValid()
            || (!question.isOptional() && (response.getAnswers() == null || response.getAnswers().isEmpty()))
            || (question.isOptional() && (response.getAnswers() == null || response.getAnswers().isEmpty())
                && !response.isSkipped()))
          return false;

        if (question.isOptional() && response.isSkipped())
          return true;

        // Is the given answer part of the possible answers (data integrity validation)
        for (final Answer answer : choiceQuestion.getAnswers()) {
          if (response.getAnswers().stream().anyMatch(p -> p.getId().equals(answer.getId())))
            return true;
        }

        return false;

      }
      case NUMBER:
      case RANGE: {

        final SurveyResponse response = responses.get(question.getId());
        return response != null && response.isValid()
            && (response.getNumberAnswer() != null || question.isOptional() && response.isSkipped());

      }
      case TEXT: {

        final SurveyResponse response = responses.get(question.getId());
        return response != null && response.isValid()
            && ((response.getTextAnswer() != null && !response.getTextAnswer().isBlank())
                || question.isOptional() && response.isSkipped());

      }
      default:
        return false;
    }
  }
}
