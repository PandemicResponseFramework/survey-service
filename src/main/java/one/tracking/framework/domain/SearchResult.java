/**
 *
 */
package one.tracking.framework.domain;

import lombok.Builder;
import lombok.Data;
import one.tracking.framework.entity.meta.Survey;
import one.tracking.framework.entity.meta.container.Container;
import one.tracking.framework.entity.meta.question.Question;

/**
 * @author Marko Voß
 *
 */
@Data
@Builder
public class SearchResult {

  private Container originContainer;
  private Survey survey;
  private Question rootQuestion;
}
