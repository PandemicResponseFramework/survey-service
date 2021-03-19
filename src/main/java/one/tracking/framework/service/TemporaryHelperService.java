/**
 *
 */
package one.tracking.framework.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import one.tracking.framework.domain.ContainerQuestionRelation;
import one.tracking.framework.domain.SearchResult;
import one.tracking.framework.entity.meta.Survey;
import one.tracking.framework.entity.meta.container.Container;
import one.tracking.framework.entity.meta.question.Question;
import one.tracking.framework.repo.ContainerRepository;

/**
 * @author Marko Voß
 *
 */
@Service
public class TemporaryHelperService {

  @Autowired
  private ContainerRepository containerRepository;

  public List<SearchResult> searchSurveys(final Question question) {

    if (question == null || question.getId() == null)
      return null;

    final List<Container> originContainers =
        this.containerRepository.findByQuestionsId(question.getId());

    if (originContainers.isEmpty())
      throw new IllegalStateException(
          "Unexpected state. No container found containing question id: " + question.getId());

    boolean done = false;
    List<ContainerQuestionRelation> currentList = originContainers.stream()
        .map(container -> ContainerQuestionRelation.builder()
            .originContainer(container)
            .container(container)
            .childQuestion(question)
            .build())
        .collect(Collectors.toList());

    while (!done) {
      final List<ContainerQuestionRelation> result = new ArrayList<>();
      done = findParentContainers(currentList, result);
      currentList = result;
    }

    return currentList.stream().map(relation -> SearchResult.builder()
        .survey((Survey) relation.getContainer())
        .rootQuestion(relation.getChildQuestion())
        .originContainer(relation.getOriginContainer())
        .build())
        .collect(Collectors.toList());
  }

  private boolean findParentContainers(
      final List<ContainerQuestionRelation> currentRelations,
      final List<ContainerQuestionRelation> result) {

    boolean done = true;

    for (final ContainerQuestionRelation relation : currentRelations) {

      if (relation.getContainer().getParent() == null) {
        result.add(relation);
      } else {
        result.addAll(
            this.containerRepository.findByQuestionsId(relation.getContainer().getParent().getId()).stream()
                .map(container -> ContainerQuestionRelation.builder()
                    .originContainer(relation.getOriginContainer())
                    .container(container)
                    .childQuestion(relation.getContainer().getParent())
                    .build())
                .collect(Collectors.toList()));
        done = false;
      }
    }

    return done;
  }
}
