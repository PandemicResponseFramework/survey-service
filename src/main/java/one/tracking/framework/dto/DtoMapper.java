/**
 *
 */
package one.tracking.framework.dto;

import java.util.List;
import java.util.stream.Collectors;
import one.tracking.framework.dto.meta.AnswerDto;
import one.tracking.framework.dto.meta.SurveyDto;
import one.tracking.framework.dto.meta.container.BooleanContainerDto;
import one.tracking.framework.dto.meta.container.ChoiceContainerDto;
import one.tracking.framework.dto.meta.question.BooleanQuestionDto;
import one.tracking.framework.dto.meta.question.ChecklistEntryDto;
import one.tracking.framework.dto.meta.question.ChecklistQuestionDto;
import one.tracking.framework.dto.meta.question.ChoiceQuestionDto;
import one.tracking.framework.dto.meta.question.QuestionDto;
import one.tracking.framework.dto.meta.question.RangeQuestionDto;
import one.tracking.framework.dto.meta.question.TextQuestionDto;
import one.tracking.framework.entity.meta.Answer;
import one.tracking.framework.entity.meta.Survey;
import one.tracking.framework.entity.meta.container.BooleanContainer;
import one.tracking.framework.entity.meta.container.ChoiceContainer;
import one.tracking.framework.entity.meta.question.BooleanQuestion;
import one.tracking.framework.entity.meta.question.ChecklistEntry;
import one.tracking.framework.entity.meta.question.ChecklistQuestion;
import one.tracking.framework.entity.meta.question.ChoiceQuestion;
import one.tracking.framework.entity.meta.question.Question;
import one.tracking.framework.entity.meta.question.RangeQuestion;
import one.tracking.framework.entity.meta.question.TextQuestion;

/**
 * @author Marko Voß
 *
 */
public abstract class DtoMapper {

  /**
   *
   * @param entity
   * @return
   */
  public static final SurveyDto map(final Survey entity) {

    return SurveyDto.builder()
        .id(entity.getId())
        .nameId(entity.getNameId())
        .title(entity.getTitle())
        .description(entity.getDescription())
        .version(entity.getVersion())
        .questions(entity.getQuestions().stream().map(DtoMapper::map).collect(Collectors.toList()))
        .build();
  }

  /**
   *
   * @param entity
   * @return
   */
  public static final QuestionDto map(final Question entity) {

    if (entity instanceof BooleanQuestion)
      return map((BooleanQuestion) entity);
    if (entity instanceof ChoiceQuestion)
      return map((ChoiceQuestion) entity);
    if (entity instanceof RangeQuestion)
      return map((RangeQuestion) entity);
    if (entity instanceof TextQuestion)
      return map((TextQuestion) entity);
    if (entity instanceof ChecklistQuestion)
      return map((ChecklistQuestion) entity);

    return null;
  }

  /**
   *
   * @param entity
   * @return
   */
  public static final BooleanQuestionDto map(final BooleanQuestion entity) {

    return BooleanQuestionDto.builder()
        .id(entity.getId())
        .order(entity.getRanking())
        .question(entity.getQuestion())
        .defaultAnswer(entity.getDefaultAnswer())
        .container(map(entity.getContainer()))
        .build();
  }

  /**
   *
   * @param entity
   * @return
   */
  public static final ChoiceQuestionDto map(final ChoiceQuestion entity) {

    return ChoiceQuestionDto.builder()
        .id(entity.getId())
        .order(entity.getRanking())
        .question(entity.getQuestion())
        .defaultAnswer(entity.getDefaultAnswer() == null ? null : entity.getDefaultAnswer().getId())
        .answers(entity.getAnswers().stream().map(DtoMapper::map).collect(Collectors.toList()))
        .multiple(entity.getMultiple())
        .container(map(entity.getContainer()))
        .build();
  }

  public static final ChecklistQuestionDto map(final ChecklistQuestion entity) {
    return ChecklistQuestionDto.builder()
        .id(entity.getId())
        .order(entity.getRanking())
        .question(entity.getQuestion())
        .entries(entity.getEntries().stream().map(DtoMapper::map).collect(Collectors.toList()))
        .build();
  }

  public static final ChecklistEntryDto map(final ChecklistEntry entity) {
    return ChecklistEntryDto.builder()
        .id(entity.getId())
        .order(entity.getRanking())
        .question(entity.getQuestion())
        .build();
  }

  /**
   *
   * @param entity
   * @return
   */
  public static final RangeQuestionDto map(final RangeQuestion entity) {

    return RangeQuestionDto.builder()
        .id(entity.getId())
        .order(entity.getRanking())
        .question(entity.getQuestion())
        .defaultValue(entity.getDefaultAnswer())
        .minValue(entity.getMinValue())
        .maxValue(entity.getMaxValue())
        .minText(entity.getMinText())
        .maxText(entity.getMaxText())
        .build();
  }

  /**
   *
   * @param entity
   * @return
   */
  public static final TextQuestionDto map(final TextQuestion entity) {

    return TextQuestionDto.builder()
        .id(entity.getId())
        .order(entity.getRanking())
        .question(entity.getQuestion())
        .multiline(entity.isMultiline())
        .length(entity.getLength())
        .build();
  }

  /**
   *
   * @param entity
   * @return
   */
  public static final BooleanContainerDto map(final BooleanContainer entity) {

    if (entity == null || entity.getQuestions() == null || entity.getQuestions().isEmpty())
      return null;

    return BooleanContainerDto.builder()
        .boolDependsOn(entity.getDependsOn())
        .subQuestions(map(entity.getQuestions()))
        .build();
  }

  /**
   *
   * @param entity
   * @return
   */
  public static final ChoiceContainerDto map(final ChoiceContainer entity) {

    if (entity == null || entity.getQuestions() == null || entity.getQuestions().isEmpty())
      return null;

    final List<Long> dependsOn = entity.getDependsOn() == null || entity.getDependsOn().isEmpty() ? null
        : entity.getDependsOn().stream().map(c -> c.getId()).collect(Collectors.toList());

    return ChoiceContainerDto.builder()
        .choiceDependsOn(dependsOn)
        .subQuestions(map(entity.getQuestions()))
        .build();
  }

  /**
   *
   * @param subQuestions
   * @return
   */
  public static final List<QuestionDto> map(final List<Question> subQuestions) {

    if (subQuestions == null || subQuestions.isEmpty())
      return null;

    return subQuestions.stream().map(DtoMapper::map).collect(Collectors.toList());
  }

  /**
   *
   * @param entity
   * @return
   */
  public static final AnswerDto map(final Answer entity) {
    return AnswerDto.builder()
        .id(entity.getId())
        .value(entity.getValue())
        .build();
  }
}
