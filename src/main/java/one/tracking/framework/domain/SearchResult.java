/**
 *
 */
package one.tracking.framework.domain;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import one.tracking.framework.entity.meta.question.Question;

@Data
@Builder
public final class SearchResult {

  private Question question;
  private List<Question> questionContainer;
  private int position;
}
