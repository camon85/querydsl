package study.querydsl.entity;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
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

  /**
   * 회원 정렬 순서
   * 1. 회원 나이 내림차순 (desc)
   * 2. 회원 이름 오름차순 (asc)
   * 2에서 회원 이름이 없으면 마지막에 출력 (lulls last)
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

}
