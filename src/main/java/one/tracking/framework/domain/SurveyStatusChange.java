/**
 *
 */
package one.tracking.framework.domain;

import lombok.Builder;
import lombok.Data;
import one.tracking.framework.entity.meta.question.Question;

/**
 * @author Marko Voß
 *
 */
@Data
@Builder
public class SurveyStatusChange {

  private Question nextQuestion;

  private boolean skipUpdate;

  public boolean hasNextQuestion() {
    return this.nextQuestion != null;
  }

  public static final SurveyStatusChange withNextQuestion(final Question nextQuestion) {
    return SurveyStatusChange.builder().nextQuestion(nextQuestion).skipUpdate(false).build();
  }

  public static final SurveyStatusChange skip() {
    return SurveyStatusChange.builder().skipUpdate(true).build();
  }

  public static final SurveyStatusChange noSkip() {
    return SurveyStatusChange.builder().skipUpdate(false).build();
  }
}
