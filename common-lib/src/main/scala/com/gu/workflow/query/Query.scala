package com.gu.workflow.query

import scala.slick.ast.BaseTypedType
import scala.slick.driver.PostgresDriver.simple._
import com.github.tototoshi.slick.PostgresJodaSupport._
import scala.slick.lifted.{Query, Column}
import models._
import models.Flag.Flag
import org.joda.time.DateTime
import com.gu.workflow.db.Schema._
import com.gu.workflow.syntax._

case class WfQueryTime(
  from  : Option[DateTime],
  until : Option[DateTime]
)

case class WfQuery(
  section       : Seq[Section]     = Nil,
  desk          : Seq[Desk]        = Nil,
  dueTimes      : Seq[WfQueryTime] = Nil,
  status        : Seq[Status]      = Nil,
  contentType   : Seq[String]      = Nil,
  published     : Option[Boolean]  = None,
  flags         : Seq[Flag]  = Nil,
  prodOffice    : Seq[String]      = Nil,
  creationTimes : Seq[WfQueryTime] = Nil
)

object WfQuery {
  // correctly typed shorthand
  private val TrueCol : Column[Boolean] = LiteralColumn(true)
  private val FalseCol: Column[Boolean] = LiteralColumn(false)

  def searchSet[A, DB, Row](options: Seq[_], getField: DB => A)(pred: A => Column[Boolean]):
      (Query[DB, Row]) => Query[DB, Row] = options match {
    case Nil  => (startQuery => startQuery)
    case opts => (startQuery => startQuery.filter(table => pred(getField(table))))
  }

  def simpleInSet[A : BaseTypedType, DB, Row](options: Seq[A])(getField: DB => Column[A]):
      (Query[DB, Row]) => Query[DB, Row] =
    searchSet(options, getField)(_ inSet options)

  // can I find a better way to implement the option logic?
  def optInSet[A : BaseTypedType, DB, Row](options: Seq[A])(getField: DB => Column[Option[A]]):
      (Query[DB, Row]) => Query[DB, Row] =
    searchSet(options, getField)(col => (col inSet options).getOrElse(false))


  def dateInSet[DB, Row](options: Seq[WfQueryTime])
               (getField: DB => Column[Option[DateTime]]):
       (Query[DB, Row]) => Query[DB, Row] =
    searchSet(options, getField) { date =>
      // build up a query that compares each dateblock to the date
      // either of the date boundaries might be missing, in which
      // case, we want to return true
      options.foldLeft(FalseCol) { (sofar, dateblock) =>
        sofar || (!date.isNull &&
          (date >= dateblock.from).getOrElse(true) &&
             (date < dateblock.until).getOrElse(true))
      }
    }

  def optToSeq[A](o: Option[A]): Seq[A] =
    o map (List(_)) getOrElse Nil

  def dateTimeToQueryTime(from: Option[DateTime], until: Option[DateTime]) =
    (from, until) match {
      case (None, None)  => Nil
      case (from, until) => List(WfQueryTime(from, until))
    }

  def fromOptions(
    section:      Option[List[Section]]  = None,
    desk:         Option[Desk]     = None,
    dueFrom:      Option[DateTime] = None,
    dueUntil:     Option[DateTime] = None,
    status:       Option[Status]   = None,
    contentType:  Option[String]   = None,
    published:    Option[Boolean]  = None,
    flags:        Seq[String]      = Nil,
    prodOffice:   Option[String]   = None,
    createdFrom:  Option[DateTime] = None,
                  createdUntil: Option[DateTime] = None
  ): WfQuery = WfQuery(
    section getOrElse Nil,
    optToSeq(desk),
    dateTimeToQueryTime(dueFrom, dueUntil),
    optToSeq(status),
    optToSeq(contentType),
    published,
    flags.map(queryStringToFlag(_)),
    optToSeq(prodOffice),
    dateTimeToQueryTime(createdFrom, createdUntil)
  )

  val queryStringToFlag = Map("needsLegal" -> Flag.Required,
                              "approved" -> Flag.Complete,
                              "notRequired" -> Flag.NotRequired)

  def stubsQuery(q: WfQuery) = stubs |>
    simpleInSet(q.section.map(_.toString))(_.section) |>
    optInSet(q.contentType)(_.contentType) |>
    simpleInSet(q.prodOffice)(_.prodOffice) |>
    dateInSet(q.dueTimes)(_.due) |>
    dateInSet(q.creationTimes)(_.createdAt) |>
    simpleInSet(q.flags)(_.needsLegal)

  def contentQuery(q: WfQuery) = content |>
    simpleInSet(q.status.map(_.toString))(_.status) |>
    simpleInSet(q.contentType)(_.contentType) |>
    q.published.foldl[ContentQuery]((query, published) => query.filter(_.published === published))
}