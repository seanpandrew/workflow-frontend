package com.gu.workflow.query

import lib.PostgresDB
import models.{ContentItem, DashboardRow}
import org.scalatest.Matchers
import org.scalatest.matchers.{Matcher, MatchResult}

trait FilterTestOps extends Matchers {

  type Content = List[ContentItem]

  case class FilterTest(p: (ContentItem) => Boolean, testData: Content) {
    val splitTestData = testData.partition(p)

    def compareTo(dbResult: DBResult): Boolean = {
      val (testIn, testOut) = splitTestData
      (dbResult.results sameElements testIn) && (dbResult.rest sameElements testOut)
    }

    def matchWith(query: WfQuery): MatchResult = {
      val dbResult = DBResult(query, testData)
      val (testIn, testOut) = splitTestData
      MatchResult(
        compareTo(dbResult),
        s"Result from database (${dbResult.results}) did not contain expected elements (${testOut})",
        s"Result from database (${dbResult.results}) contained unexpected elements (" +
          testOut diff dbResult.results + ")"
      )
    }
  }

  case class DBResult(query: WfQuery, inputData: Content) {
    val results = PostgresDB.getContent(query).map(DashboardRow.toContentItem(_))
    val rest = inputData diff results
  }

  class DBResultMatcher(filterTest: FilterTest) extends Matcher[WfQuery] {
    def apply(query: WfQuery) = filterTest.matchWith(query)
  }

  def selectSameResultsAs(filterTest: FilterTest) =
    new DBResultMatcher(filterTest)

}
// trait FilterTestOps extends Matchers {
//   def compare(filter: FilterTest, res: DBResult, testData: Content) = {
//               testData: Content,
//               dbAll: Content,
//               dbIn: Content) = {
//     val (testIn, testOut) = filter.splitTestData(testData)
//     val dbOut = dbAll diff dbIn

//     dbIn should contain theSameElementsAs (testIn)
//   }
// }
