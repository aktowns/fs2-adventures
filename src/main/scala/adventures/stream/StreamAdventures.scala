package adventures.stream

import adventures.stream.model.{PageId, PaginatedResult, SourceRecord, TargetRecord}
import cats.effect.IO
import fs2.{Pure, Stream}

import scala.concurrent.Future
import scala.concurrent.duration.*
import adventures.io.IOAdventures

/** If elements from a list can be operated on synchronously as a List[A], then the equivalent data structure where
  * those elements can be operated asynchronously could be represented as a Stream[A].
  *
  * The docs for Stream are online https://fs2.io/#/guide?id=building-streams. 
  *
  * These exercises will introduce you to a common problem ETL pipeline. The steps to complete this are.
  *   1. Read from a external paginated datasource (no need to worry about duplicate data, assume data will only come
  *      through once). 
  *   2. Transform that data (filtering out any invalid data) 
  *   3. Insert that data into elasticsearch
  *      (which has an efficient API to insert in batches of 5 - in reality its much more but it keeps the test output
  *      small).
  */
object StreamAdventures:

  /** For this exercise, think (NO NEED TO IMPLEMENT) about how you would implement the following without FS2.
    *
    * Given an Iterable or record ids, how would you go about loading each of those records asynchronously?
    *
    * This exercise doesn't have to be implemented, but just think about what this would look like. Would you model the
    * return type as:
    *   - Iterable[TargetRecord] (what does this imply must happen?)
    *   - Iterable[Future[TargetRecord]] (what is then responsible for handling the asynchronous nature?)
    *
    * What would you do if you needed back pressure (i.e. if something else consuming from the iterable slowed down, how
    * would this propagate?)
    */
  def iterablesAndConcurrency(records: Iterable[Int], load: Int => Future[SourceRecord]): Iterable[TargetRecord] = ???

  /** Create an Stream which emits each element of the source list
    */
  def listToStream(records: List[SourceRecord]): Stream[Pure, SourceRecord] = Stream.emits(records)

  /** Transform all of the SourceRecords to TargetRecords. If the price cannot be converted to a double, then drop the
    * Source element.
    *
    * @param sourceRecords
    * @return
    */
  def transform(sourceRecords: Stream[IO, SourceRecord]): Stream[IO, TargetRecord] =
    sourceRecords.collect:
      case SourceRecord(id, price) if price.matches("^[0-9]+(\\.[0-9]+)?$") =>
        TargetRecord(id, price.toDouble)

  /** Elastic search supports saving batches of 5 records. This is a remote async call so the result is represented by
    * `Stream`.
    *
    * Implement the following method so it calls elasticSearchLoad with batches of 5 records and returns the number of
    * loaded items.
    */
  def load(targetRecords: Stream[IO, TargetRecord], elasticSearchLoad: Seq[TargetRecord] => IO[Unit]): Stream[IO, Int] =
    targetRecords
      .groupWithin(5, 1.second)
      .evalMap{chunk => elasticSearchLoad(chunk.toList).as(chunk.size)}

  /** Elasticsearch supports saving batches of 5 records. This is a remote async call so the result is represented by
    * `IO`. Note that the elasticSearchLoad may fail (in practice this is pretty rare). Rather than the Observable
    * terminating with an error, try using the Task retry logic you created earlier in IOAdventures.
    *
    * Returns the number of records which were saved to elastic search.
    */
  def loadWithRetry(
    targetRecords: Stream[IO, TargetRecord],
    elasticSearchLoad: Seq[TargetRecord] => IO[Unit]
  ): Stream[IO, Int] =
    load(targetRecords, records => IOAdventures.retryOnFailure(elasticSearchLoad(records), 10, 500.millis))

  /** Consume the Observable
    *
    * The final result should be the number of records which were saved to ElasticSearch.
    */
  def execute(loadedStream: Stream[IO, Int]): IO[Int] = loadedStream.compile.toList.map(_.sum)

  /** Create an Observable from which all records can be read.
    *
    * Earlier we created "listToStream", but what if the source data comes from a paginated datasource. In the
    * following method `readPage` returns a PaginatedResult object that contains list of record and a reference to the
    * next page. You are required the read the records from ALL the pages and return them in a single Stream.
    *
    * The first page of data can be obtained using `PageId.FirstPage`, after which you should follow the nextPage
    * references in the PaginatedResult.
    *
    */
  def readFromPaginatedDatasource(readPage: PageId => IO[PaginatedResult]): Stream[IO, SourceRecord] =
    Stream.unfoldLoopEval(PageId.FirstPage) { pageId =>
      readPage(pageId).map { paginatedResult =>
        paginatedResult.nextPage match
          case Some(nextPageId) => (paginatedResult.results, Some(nextPageId))
          case None             => (paginatedResult.results, None)
      }
    }.flatMap(Stream.emits)


  /** Lets say reading a page takes 1 second and loading a batch of records takes 1 second. If there are 20 pages (each
    * of one load batch in size), how long will it take to execute? Look for "Processing took XXms" in the logs. Try to
    * reduce the overall time by doing the reads and writes in parallel. Below is provided a sequential implementation
    * (assuming you have implemented the methods above).
    */
  def readTransformAndLoadAndExecute(
    readPage: PageId => IO[PaginatedResult],
    elasticSearchLoad: Seq[TargetRecord] => IO[Unit]
  ): IO[Int] =
    // Note it wouldn't look like this in the prod code, but more a factor of combining our building blocks above.
    val readObservable        = readFromPaginatedDatasource(readPage)
    val transformedObservable = transform(readObservable)
    execute(load(transformedObservable, elasticSearchLoad))
