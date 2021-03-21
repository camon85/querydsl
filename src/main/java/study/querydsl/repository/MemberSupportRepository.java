package study.querydsl.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.entity.Member;
import study.querydsl.repository.support.QuerydslRepositorySupport;

import java.util.List;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@Repository
public class MemberSupportRepository extends QuerydslRepositorySupport {

  public MemberSupportRepository() {
    super(Member.class);
  }

  // custom support 를 사용하여 조금 더 편리하게 쓸 수 있음
  public List<Member> basicSelect() {
    return select(member)
        .from(member)
        .fetch();
  }

  public List<Member> basicSelectFrom() {
    return selectFrom(member)
        .fetch();
  }

  // 아래 applyPagination 와 동일한 동작. support 없는 것과 비교 하기 위한 코드
  public Page<Member> searchPageByApplyPage(MemberSearchCondition condition, Pageable pageable) {
    JPAQuery<Member> query = selectFrom(member)
        .leftJoin(member.team, team)
        .where(usernameEq(condition.getUsername()),
            teamNameEq(condition.getTeamName()),
            ageGoe(condition.getAgeGoe()),
            ageLoe(condition.getAgeLoe()));
    List<Member> content = getQuerydsl().applyPagination(pageable, query)
        .fetch();
    return PageableExecutionUtils.getPage(content, pageable,
        query::fetchCount);
  }

  // 위 searchPageByApplyPage 와 동일한 동작. 좀 더 심플해짐
  public Page<Member> applyPagination(MemberSearchCondition condition, Pageable pageable) {
    return applyPagination(
        pageable,
        contentQuery -> contentQuery
            .selectFrom(member)
            .leftJoin(member.team, team)
            .where(usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe())));
  }

  // count query 분리
  public Page<Member> applyPagination2(MemberSearchCondition condition, Pageable pageable) {
    return applyPagination(
        pageable,
        contentQuery -> contentQuery
            .selectFrom(member)
            .leftJoin(member.team, team)
            .where(usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe())),
        countQuery -> countQuery
            .selectFrom(member)
            .leftJoin(member.team, team)
            .where(usernameEq(condition.getUsername()),
                teamNameEq(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe()))
    );
  }

  private BooleanExpression usernameEq(String username) {
    return StringUtils.hasText(username) ? member.username.eq(username) : null;
  }

  private BooleanExpression teamNameEq(String teamName) {
    return StringUtils.hasText(teamName) ? team.name.eq(teamName) : null;
  }

  private BooleanExpression ageGoe(Integer ageGoe) {
    return ageGoe != null ? member.age.goe(ageGoe) : null;
  }

  private BooleanExpression ageLoe(Integer ageLoe) {
    return ageLoe != null ? member.age.loe(ageLoe) : null;
  }

}