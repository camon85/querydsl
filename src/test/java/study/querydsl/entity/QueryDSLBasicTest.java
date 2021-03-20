package study.querydsl.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
//@Commit
public class QueryDSLBasicTest {

  @Autowired EntityManager em;

  private JPAQueryFactory queryFactory;

  @BeforeEach
  void testEntity() {
    queryFactory = new JPAQueryFactory(em);

    Team teamA = new Team("teamA");
    Team teamB = new Team("teamB");
    em.persist(teamA);
    em.persist(teamB);

    Member member1 = new Member("member1", 10, teamA);
    Member member2 = new Member("member2", 20, teamA);

    Member member3 = new Member("member3", 30, teamB);
    Member member4 = new Member("member4", 40, teamB);
    em.persist(member1);
    em.persist(member2);
    em.persist(member3);
    em.persist(member4);
  }

  @Test
  void startJPQL() {
    // member1 찾기
    String qlString = "select m from Member m where m.username = :username";
    Member findMember = em.createQuery(qlString, Member.class)
        .setParameter("username", "member1")
        .getSingleResult();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  void startQueryDSL() {
    Member findMember = queryFactory
        .select(member)
        .from(member)
        .where(member.username.eq("member1"))
        .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  @DisplayName("where 절 내에서 조건을 and로 연결")
  void search() {
    Member findMember = queryFactory
        .selectFrom(member)
        .where(member.username.eq("member1")
            .and(member.age.eq(10)))
        .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");
    assertThat(findMember.getAge()).isEqualTo(10);
  }

  @Test
  @DisplayName("where 절 내에서 조건을 콤마로  연결. 이때 null 값은 무시된다.")
  void searchAndParam() {
    Member findMember = queryFactory
        .selectFrom(member)
        .where(
            member.username.eq("member1"),
            (member.age.eq(10)), null
        )
        .fetchOne();

    assertThat(findMember.getUsername()).isEqualTo("member1");
    assertThat(findMember.getAge()).isEqualTo(10);
  }

  @Test
  void resultFetch() {
    //    List<Member> fetch = queryFactory
    //        .selectFrom(member)
    //        .fetch();
    //
    //    Member fetchOne = queryFactory
    //        .selectFrom(QMember.member)
    //        .fetchOne();
    //
    //    Member fetchFirst = queryFactory
    //        .selectFrom(QMember.member)
    //        .fetchFirst();

    //    QueryResults<Member> results = queryFactory
    //        .selectFrom(member)
    //        .fetchResults(); // count까지 가져온다.
    //    results.getTotal();
    //    List<Member> content = results.getResults();

    long totalCount = queryFactory
        .selectFrom(member)
        .fetchCount(); // count쿼리로 변경된다.
  }

  /*
    회원 정렬 순서
    1. 회원 나이 내림차순 (desc)
    2. 회원 이름 오름차순 (asc)
    2에서 회원 이름이 없으면 마지막에 출력 (lulls last)
   */
  @Test
  void sort() {
    em.persist((new Member(null, 100)));
    em.persist((new Member("member5", 100)));
    em.persist((new Member("member6", 100)));

    List<Member> results = queryFactory
        .selectFrom(member)
        .where(member.age.eq(100))
        .orderBy(member.age.desc(), member.username.asc().nullsLast())
        .fetch();

    Member member5 = results.get(0);
    Member member6 = results.get(1);
    Member memberNull = results.get(2);

    assertThat(member5.getUsername()).isEqualTo("member5");
    assertThat(member6.getUsername()).isEqualTo("member6");
    assertThat(memberNull.getUsername()).isNull();
  }

  @Test
  void paging1() {
    List<Member> results = queryFactory
        .selectFrom(member)
        .orderBy(member.username.desc())
        .offset(1)
        .limit(2)
        .fetch();

    assertThat(results.size()).isEqualTo(2);
  }

  @Test
  void paging2() {
    QueryResults<Member> queryResults = queryFactory
        .selectFrom(member)
        .orderBy(member.username.desc())
        .offset(1)
        .limit(2)
        .fetchResults();

    assertThat(queryResults.getTotal()).isEqualTo(4);
    assertThat(queryResults.getLimit()).isEqualTo(2);
    assertThat(queryResults.getOffset()).isEqualTo(1);
    assertThat(queryResults.getResults().size()).isEqualTo(2);
  }

  @Test
  void aggregation() {
    // Tuple이 아니라 DTO로 조회할 수도 있다.
    List<Tuple> result = queryFactory
        .select(
            member.count(),
            member.age.sum(),
            member.age.avg(),
            member.age.max(),
            member.age.min()
        )
        .from(member)
        .fetch();

    Tuple tuple = result.get(0);
    assertThat(tuple.get(member.count())).isEqualTo(4);
    assertThat(tuple.get(member.age.sum())).isEqualTo(100);
    assertThat(tuple.get(member.age.avg())).isEqualTo(25);
    assertThat(tuple.get(member.age.max())).isEqualTo(40);
    assertThat(tuple.get(member.age.min())).isEqualTo(10);
  }

  /*
    팀의 이름과 각 팀의 평균 연령을 구해라.
   */
  @Test
  void group() {
    List<Tuple> result = queryFactory
        .select(team.name, member.age.avg())
        .from(member)
        .join(member.team, team)
        .groupBy(team.name)
        .fetch();

    Tuple teamA = result.get(0);
    Tuple teamB = result.get(1);

    assertThat(teamA.get(team.name)).isEqualTo("teamA");
    assertThat(teamA.get(member.age.avg())).isEqualTo(15); // (10 + 20) / 2 = 15

    assertThat(teamB.get(team.name)).isEqualTo("teamB");
    assertThat(teamB.get(member.age.avg())).isEqualTo(35); // (30 + 40) / 2 = 35
  }

  // 팀A에 소속된 모든 회원
  @Test
  void join() {
    List<Member> result = queryFactory
        .selectFrom(member)
        .join(member.team, team)
        .where(team.name.eq("teamA"))
        .fetch();

    assertThat(result)
        .extracting("username")
        .containsExactly("member1", "member2");
  }

  // 세타 조인. 회원의 이름이 팀 이름과 같은 회원 조회
  @Test
  void theta_join() {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));
    em.persist(new Member("teamC"));

    List<Member> result = queryFactory
        .select(member)
        .from(member, team) // 연관관계 참조 없이 막 조인. cross join
        .where(member.username.eq(team.name))
        .fetch();

    assertThat(result)
        .extracting("username")
        .containsExactly("teamA", "teamB");
  }

  // 회원과 팀을 join 하면서 팀 이름이 teamA인 팀만 join. 회원은 모두 조회
  // JPQL: select m,t from Member m left join m.team t on t.name = 'teamA'
  @Test
  void join_on_filtering() {
    List<Tuple> result = queryFactory
        .select(member, team)
        .from(member)
        .leftJoin(member.team, team).on(team.name.eq("teamA"))
        .fetch();
    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }
  }

  // 연관관계 없는 엔티티 외부 조인
  // 회원의 이름이 팀 이름과 같은 대상 외부 조인
  @Test
  void theta_join_on_no_relation() {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));
    em.persist(new Member("teamC"));

    List<Tuple> result = queryFactory
        .select(member, team)
        .from(member)
        .leftJoin(team).on(member.username.eq(team.name))
        .fetch();

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }

    /*
    tuple = [Member(id=3, username=member1, age=10), null]
    tuple = [Member(id=4, username=member2, age=20), null]
    tuple = [Member(id=5, username=member3, age=30), null]
    tuple = [Member(id=6, username=member4, age=40), null]
    tuple = [Member(id=7, username=teamA, age=0), Team(id=1, name=teamA)]
    tuple = [Member(id=8, username=teamB, age=0), Team(id=2, name=teamB)]
    tuple = [Member(id=9, username=teamC, age=0), null]
     */
  }

  @PersistenceUnit
  EntityManagerFactory emf;

  @Test
  void notUseFetchJoin() {
    em.flush();
    em.clear();

    Member findMember = queryFactory
        .selectFrom(member)
        .where(member.username.eq("member1"))
        .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
    assertThat(loaded).as("페치 조인 미적용").isFalse();
  }

  @Test
  void useFetchJoin() {
    em.flush();
    em.clear();

    Member findMember = queryFactory
        .selectFrom(member)
        .join(member.team, team).fetchJoin()
        .where(member.username.eq("member1"))
        .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
    assertThat(loaded).as("페치 조인 적용").isTrue();
  }

  // 나이가 가장 많은 회원 조회
  @Test
  void subQuery() {
    // alias 중복되지 않도록 새로 생성
    QMember memberSub = new QMember("memberSub");

    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.age.eq(
            JPAExpressions.select(memberSub.age.max())
                .from(memberSub)
        ))
        .fetch();

    assertThat(result).extracting("age")
        .containsExactly(40);
  }

  // 나이가 평균 이상인 회원 조회
  @Test
  void subQueryGoe() {
    QMember memberSub = new QMember("memberSub");

    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.age.goe(
            JPAExpressions.select(memberSub.age.avg())
                .from(memberSub)
        ))
        .fetch();

    assertThat(result).extracting("age")
        .containsExactly(30, 40);
  }

  // 나이가 평균 이상인 회원 조회
  @Test
  void subQueryIn() {
    QMember memberSub = new QMember("memberSub");

    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.age.in(
            JPAExpressions
                .select(memberSub.age)
                .from(memberSub)
                .where(memberSub.age.gt(10))
        ))
        .fetch();

    assertThat(result).extracting("age")
        .containsExactly(20, 30, 40);
  }

  @Test
  void selectSubQuery() {
    QMember memberSub = new QMember("memberSub");

    List<Tuple> result = queryFactory
        .select(member.username,
            JPAExpressions
                .select(memberSub.age.avg())
                .from(memberSub))
        .from(member)
        .fetch();

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }
  }

  @Test
  void basicCase() {
    List<String> result = queryFactory
        .select(member.age
            .when(10).then("열살")
            .when(20).then("스무살")
            .otherwise("기타"))
        .from(member)
        .fetch();
    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  @Test
  void complexCase() {
    List<String> result = queryFactory
        .select(new CaseBuilder()
            .when(member.age.between(0, 20)).then("0 ~ 20살")
            .when(member.age.between(21, 30)).then("21 ~ 30살")
            .otherwise("기타"))
        .from(member)
        .fetch();

    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  @Test
  void constant() {
    List<Tuple> result = queryFactory
        .select(member.username, Expressions.constant("A"))
        .from(member)
        .fetch();

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }

    /*
      tuple = [member1, A]
      tuple = [member2, A]
      tuple = [member3, A]
      tuple = [member4, A]
     */
  }

  @Test
  void concat() {
    // {username}_{age}
    List<String> result = queryFactory
        .select(member.username.concat("_").concat(member.age.stringValue()))
        .from(member)
        .where(member.username.eq("member1"))
        .fetch();
    for (String s : result) {
      System.out.println("s = " + s);
    }

    // s = member1_10
  }

  @Test
  void simpleProjection() {
    List<String> result = queryFactory
        .select(member.username)
        .from(member)
        .fetch();

    /*
    select
        member0_.username as col_0_0_
    from
        member member0_
     */

    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  @Test
  void tupleProjection() {
    List<Tuple> result = queryFactory
        .select(member.username, member.age)
        .from(member)
        .fetch();

    for (Tuple tuple : result) {
      String username = tuple.get(member.username);
      Integer age = tuple.get(member.age);

      System.out.println("username = " + username);
      System.out.println("age = " + age);
    }
  }

  @Test
  void findDtoByJPQL() {
    // JPQL projection은 생성자 방식만 지원해서 불편함
    String qlString = "" +
        "select new study.querydsl.dto.MemberDto(m.username, m.age) " +
        "from Member m";
    List<MemberDto> result = em.createQuery(qlString, MemberDto.class)
        .getResultList();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  void findDtoBySetter() {
    // Projections.bean: MemberDTO의 setter 메소드 활용
    List<MemberDto> result = queryFactory
        .select(Projections.bean(MemberDto.class,
            member.username,
            member.age))
        .from(member)
        .fetch();
    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  void findDtoByField() {
    // Projections.fields: 필드에 직접 set
    List<MemberDto> result = queryFactory
        .select(Projections.fields(MemberDto.class,
            member.username,
            member.age))
        .from(member)
        .fetch();
    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  void findDtoByConstructor() {
    // Projections.constructor: 생성자로 set
    List<MemberDto> result = queryFactory
        .select(Projections.constructor(MemberDto.class,
            member.username,
            member.age))
        .from(member)
        .fetch();
    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  @DisplayName("DTO 필드명이 다른 경우 alias 처리")
  void findUserDtoByField() {
    QMember memberSub = new QMember("memberSub");

    List<UserDto> result = queryFactory
        .select(Projections.fields(UserDto.class,
            // 이름이 다른 경우 as로 적용
            member.username.as("name"),
            // 서브쿼리도 alias 처리하여 적용 가능
            ExpressionUtils.as(JPAExpressions
                .select(memberSub.age.max())
                .from(memberSub), "age")))
        .from(member)
        .fetch();
    for (UserDto userDto : result) {
      System.out.println("memberDto = " + userDto);
    }
  }

  /*
  Projections.constructor 는 compile 시에 오류 검출이 되지 않는다.
  @QueryProjection 은 compile 에러가 발생한다.
   */
  @Test
  void findDtoByQueryProjection() {
    List<MemberDto> result = queryFactory
        .select(new QMemberDto(member.username, member.age))
        .from(member)
        .fetch();
    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  @Test
  void dynamicQuery_BooleanBuilder() {
    String usernameParam = "member1";
    Integer ageParam = 10;

    List<Member> result = searchMember1(usernameParam, ageParam);
    assertThat(result.size()).isEqualTo(1);
  }

  private List<Member> searchMember1(String usernameCond, Integer ageCond) {
    BooleanBuilder booleanBuilder = new BooleanBuilder();
    if (usernameCond != null) {
      booleanBuilder.and(member.username.eq(usernameCond));
    }

    if (ageCond != null) {
      booleanBuilder.and(member.age.eq(ageCond));
    }

    return queryFactory
        .selectFrom(member)
        .where(booleanBuilder)
        .fetch();
  }

}
