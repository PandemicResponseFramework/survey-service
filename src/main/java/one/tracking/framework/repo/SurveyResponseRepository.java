/**
 *
 */
package one.tracking.framework.repo;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import one.tracking.framework.entity.SurveyInstance;
import one.tracking.framework.entity.SurveyResponse;
import one.tracking.framework.entity.User;
import one.tracking.framework.entity.meta.question.Question;

/**
 * @author Marko Voß
 *
 */
public interface SurveyResponseRepository extends CrudRepository<SurveyResponse, Long> {

  // Optional<SurveyResponse> findByUserIdAndSurveyIdAndQuestionId(String userId, Long surveyId, Long
  // questionId);

  List<SurveyResponse> findByUserAndSurveyInstanceAndQuestion(User user, SurveyInstance surveyInstance,
      Question question);

  List<SurveyResponse> findByUserAndSurveyInstance(User user, SurveyInstance surveyInstance);

  void deleteByUserAndSurveyInstanceAndQuestion(User user, SurveyInstance surveyInstance, Question question);
}
