/**
 *
 */
package one.tracking.framework.repo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
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

  List<SurveyResponse> findByUserAndSurveyInstanceAndQuestion(
      User user, SurveyInstance surveyInstance, Question question);

  Optional<SurveyResponse> findTopByUserAndSurveyInstanceAndQuestionOrderByVersionDesc(
      User user, SurveyInstance surveyInstance, Question question);

  List<SurveyResponse> findByUserAndSurveyInstanceAndQuestionInOrderByVersionDesc(
      User user, SurveyInstance surveyInstance, Collection<? extends Question> question);

  List<SurveyResponse> findByUserAndSurveyInstance(User user, SurveyInstance surveyInstance);

  @Query(value = "SELECT s "
      + "FROM SurveyResponse s "
      + "WHERE s.user = ?1"
      + "  AND s.surveyInstance = ?2"
      + "  AND s.version = ("
      + "    SELECT MAX(x.version) "
      + "    FROM SurveyResponse x "
      + "    WHERE x.user = s.user "
      + "      AND x.surveyInstance = s.surveyInstance "
      + "      AND x.question = s.question"
      + "  )")
  List<SurveyResponse> findByUserAndSurveyInstanceAndMaxVersion(User user, SurveyInstance surveyInstance);

  boolean existsByUserAndSurveyInstance(User user, SurveyInstance surveyInstance);

  void deleteByUserAndSurveyInstanceAndQuestion(User user, SurveyInstance surveyInstance, Question question);

}
