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
import one.tracking.framework.dto.meta.container.DefaultContainerDto;
import one.tracking.framework.dto.meta.question.BooleanQuestionDto;
import one.tracking.framework.dto.meta.question.ChoiceQuestionDto;
import one.tracking.framework.dto.meta.question.QuestionDto;
import one.tracking.framework.dto.meta.question.QuestionType;
import one.tracking.framework.dto.meta.question.RangeQuestionDto;
import one.tracking.framework.dto.meta.question.TextQuestionDto;
import one.tracking.framework.dto.meta.question.TitleQuestionDto;
import one.tracking.framework.entity.meta.Answer;
import one.tracking.framework.entity.meta.Survey;
import one.tracking.framework.entity.meta.container.BooleanContainer;
import one.tracking.framework.entity.meta.container.ChoiceContainer;
import one.tracking.framework.entity.meta.container.DefaultContainer;
import one.tracking.framework.entity.meta.question.BooleanQuestion;
import one.tracking.framework.entity.meta.question.ChoiceQuestion;
import one.tracking.framework.entity.meta.question.Question;
import one.tracking.framework.entity.meta.question.RangeQuestion;
import one.tracking.framework.entity.meta.question.TextQuestion;
import one.tracking.framework.entity.meta.question.TitleQuestion;

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
    if (entity instanceof TitleQuestion)
      return map((TitleQuestion) entity);

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
        .defaultAnswer(entity.getDefaultValue())
        .container(map(entity.getContainer()))
        .type(QuestionType.valueOf(entity.getType()))
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
        .defaultAnswer(entity.getDefaultValue() == null ? null : entity.getDefaultValue().getId())
        .answers(entity.getAnswers().stream().map(DtoMapper::map).collect(Collectors.toList()))
        .multiple(entity.getMultiple())
        .container(map(entity.getContainer()))
        .type(QuestionType.valueOf(entity.getType()))
        .build();
  }

  /**
   *
   * @param entity
   * @return
   */
  public static final TitleQuestionDto map(final TitleQuestion entity) {

    return TitleQuestionDto.builder()
        .id(entity.getId())
        .order(entity.getRanking())
        .question(entity.getQuestion())
        .container(map(entity.getContainer()))
        .type(QuestionType.valueOf(entity.getType()))
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
        .defaultValue(entity.getDefaultValue())
        .minValue(entity.getMinValue())
        .maxValue(entity.getMaxValue())
        .minText(entity.getMinText())
        .maxText(entity.getMaxText())
        .container(map(entity.getContainer()))
        .type(QuestionType.valueOf(entity.getType()))
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
        .container(map(entity.getContainer()))
        .type(QuestionType.valueOf(entity.getType()))
        .build();
  }

  /**
   *
   * @param entity
   * @return
   */
  public static final BooleanContainerDto map(final BooleanContainer entity) {

    if (entity == null || entity.getSubQuestions() == null || entity.getSubQuestions().isEmpty())
      return null;

    return BooleanContainerDto.builder()
        .boolDependsOn(entity.getDependsOn())
        .subQuestions(map(entity.getSubQuestions()))
        .build();
  }

  /**
   *
   * @param entity
   * @return
   */
  public static final ChoiceContainerDto map(final ChoiceContainer entity) {

    if (entity == null || entity.getSubQuestions() == null || entity.getSubQuestions().isEmpty())
      return null;

    final List<Long> dependsOn = entity.getDependsOn() == null || entity.getDependsOn().isEmpty() ? null
        : entity.getDependsOn().stream().map(c -> c.getId()).collect(Collectors.toList());

    return ChoiceContainerDto.builder()
        .choiceDependsOn(dependsOn)
        .subQuestions(map(entity.getSubQuestions()))
        .build();
  }

  /**
   *
   * @param entity
   * @return
   */
  public static final DefaultContainerDto map(final DefaultContainer entity) {

    if (entity == null || entity.getSubQuestions() == null || entity.getSubQuestions().isEmpty())
      return null;

    return DefaultContainerDto.builder()
        .subQuestions(map(entity.getSubQuestions()))
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
