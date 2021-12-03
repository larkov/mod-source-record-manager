package org.folio.dao;

import org.folio.rest.jaxrs.model.JobExecution;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.dao.util.JobExecutionsColumns.FILE_NAME_FIELD;
import static org.folio.dao.util.JobExecutionsColumns.HRID_FIELD;

public class JobExecutionFilter {

  private List<JobExecution.Status> statusAny;
  private List<String> profileIdNotAny;
  private JobExecution.Status statusNot;
  private List<JobExecution.UiStatus> uiStatusAny;
  private String hrIdPattern;
  private String fileNamePattern;

  public JobExecutionFilter withStatusAny(List<JobExecution.Status> statusIn) {
    this.statusAny = statusIn;
    return this;
  }

  public JobExecutionFilter withProfileIdNotAny(List<String> profileIdNotIn) {
    this.profileIdNotAny = profileIdNotIn;
    return this;
  }

  public JobExecutionFilter withStatusNot(JobExecution.Status statusNot) {
    this.statusNot = statusNot;
    return this;
  }

  public JobExecutionFilter withUiStatusAny(List<JobExecution.UiStatus> uiStatusAny) {
    this.uiStatusAny = uiStatusAny;
    return this;
  }

  public JobExecutionFilter withHrIdPattern(String hrIdPattern) {
    this.hrIdPattern = hrIdPattern;
    return this;
  }

  public JobExecutionFilter withFileNamePattern(String fileNamePattern) {
    this.fileNamePattern = fileNamePattern;
    return this;
  }

  public String buildWhereClause() {
    StringBuilder conditionBuilder = new StringBuilder("TRUE");
    if (isNotEmpty(statusAny)) {
      String preparedStatuses = statusAny.stream()
        .map(JobExecution.Status::toString)
        .map(s -> String.format("'%s'", s))
        .collect(Collectors.joining(", ", "(", ")"));

      conditionBuilder
        .append(" AND status IN ")
        .append(preparedStatuses);
    }
    if (isNotEmpty(profileIdNotAny)) {
      List<String> profileIds = profileIdNotAny.stream()
        .map(s -> String.format("'%s'", s))
        .collect(Collectors.toList());

      conditionBuilder
        .append(" AND job_profile_id NOT IN (")
        .append(String.join(", ", profileIds))
        .append(")");
    }
    if (statusNot != null) {
      conditionBuilder
        .append(" AND status <> '")
        .append(statusNot)
        .append("'");
    }
    if (isNotEmpty(uiStatusAny)) {
      String preparedStatuses = uiStatusAny.stream()
        .map(JobExecution.UiStatus::toString)
        .map(s -> String.format("'%s'", s))
        .collect(Collectors.joining(", ", "(", ")"));

      conditionBuilder
        .append(" AND ui_status IN ")
        .append(preparedStatuses);
    }
    if (isNotEmpty(hrIdPattern) && isNotEmpty(fileNamePattern)) {
      conditionBuilder.append(String.format(" AND (%s OR %s)", buildLikeCondition(HRID_FIELD, hrIdPattern),
        buildLikeCondition(FILE_NAME_FIELD, fileNamePattern)));
    } else {
      if (isNotEmpty(hrIdPattern)) {
        conditionBuilder.append(" AND ")
          .append(buildLikeCondition(HRID_FIELD, hrIdPattern));
      }
      if (isNotEmpty(fileNamePattern)) {
        conditionBuilder.append(" AND ")
          .append(buildLikeCondition(FILE_NAME_FIELD, fileNamePattern));
      }
    }

    return conditionBuilder.toString();
  }

  private String buildLikeCondition(String columnName, String pattern) {
    String preparedLikePattern = pattern.replace("*", "%");
    return String.format("%s::text LIKE '%s'", columnName, preparedLikePattern);
  }

}
