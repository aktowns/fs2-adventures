package adventures.stream

import adventures.stream.model.{PageId, PaginatedResult, SourceRecord, TargetRecord}
import cats.effect.IO
import fs2.Stream
import munit.CatsEffectSuite

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.*

class StreamAdventuresTests extends CatsEffectSuite:
  test("create a simple stream"):
    val source = List(SourceRecord("1", "1.1"), SourceRecord("2", "2.2"), SourceRecord("3", "3.3"))
    val obs    = StreamAdventures.listToStream(source)

    assertEquals(obs.compile.toList, source)

  test("transform data and filter out invalid data"):
    val source = Stream(SourceRecord("1", "1.1"), SourceRecord("2", "invalid"), SourceRecord("3", "3.3"))

    val obs = StreamAdventures.transform(source)

    val expected = List(TargetRecord("1", 1.1), TargetRecord("3", 3.3))

    obs.compile.toList.assertEquals(expected)

  test("load data in batches"):
    var loads: ListBuffer[List[TargetRecord]] = ListBuffer[List[TargetRecord]]()

    def esLoad(batch: Seq[TargetRecord]): IO[Unit] =
        IO(loads.append(batch.toList)).void

    val source   = (1 to 12).map(i => TargetRecord(i.toString, i)).toList
    val obs      = StreamAdventures.load(Stream.emits(source), esLoad)
    val expected = source.grouped(5).toList

    assertEquals(obs.compile.toList.unsafeRunSync(), List(5, 5, 2))
    assertEquals(loads.toList, expected)

  test("load data in batches and retry on failure"):
    var loads = ListBuffer[List[TargetRecord]]()

    var i = 0

    def esLoad(batch: Seq[TargetRecord]): IO[Unit] =
      IO {
        i = i + 1
        i
      }.flatMap { loadCount =>
        if loadCount % 2 == 0 then IO.raiseError(new RuntimeException("ES write failed"))
        else IO(loads.append(batch.toList))
      }

    val source   = (1 to 12).map(i => TargetRecord(i.toString, i)).toList
    val obs      = StreamAdventures.loadWithRetry(Stream.emits(source), esLoad)
    val expected = source.grouped(5).toList

    assertEquals(obs.compile.toList.unsafeRunSync(), List(5, 5, 2))
    assertEquals(loads.toList, expected)

  test("Consume a stream"):
    val subj = StreamAdventures.execute(Stream(5, 5, 2))

    subj.assertEquals(12)

  test("handling a paginated feed / handling a small set of data"):
    val pages = Map(
      PageId.FirstPage -> PaginatedResult(List(SourceRecord("1", "1.1")), Some(PageId("2"))),
      PageId("2")      -> PaginatedResult(List(SourceRecord("2", "2.2")), Some(PageId("3"))),
      PageId("3")      -> PaginatedResult(List(SourceRecord("3", "3.3")), None)
    )

    def readPage(pageId: PageId): IO[PaginatedResult] =
      IO(pages(pageId))

    val obs = StreamAdventures.readFromPaginatedDatasource(readPage)

    val expected = List(SourceRecord("1", "1.1"), SourceRecord("2", "2.2"), SourceRecord("3", "3.3"))
    obs.compile.toList.assertEquals(expected)

  // Verifies the implementation doesn't go the whole way to the end of all pages before emitting data.
  test("handling a paginated feed / should emit data as it is read"):
    val pages: Map[PageId, PaginatedResult] = Map(
      PageId.FirstPage -> PaginatedResult(List(SourceRecord("1", "1.1")), Some(PageId("2"))),
      PageId("2")      -> PaginatedResult(List(SourceRecord("2", "2.2")), Some(PageId("3"))),
      PageId("3")      -> PaginatedResult(List(SourceRecord("3", "3.3")), None)
    )

    def readPage(pageId: PageId): IO[PaginatedResult] =
      IO.sleep(1.second).as(pages(pageId))

    val obs: Stream[IO, SourceRecord] = StreamAdventures.readFromPaginatedDatasource(readPage)

    var dataEmitted: SourceRecord = null

    obs.foreach((emitted: SourceRecord) => IO { dataEmitted = emitted }).compile.drain.unsafeToFuture() // ???

    Thread.sleep(1500) // TODO: TestControl

    assertNotEquals(dataEmitted, null)

  test("handling a paginated feed / handling a large set of data"):
    val pages = (0 to 19).map { page =>
      val pageRecords = (0 to 4).map { record =>
        SourceRecord(s"${page}-${record}", "111")
      }.toList
      val nextPage =
        if page >= 19 then None else Some(PageId((page + 1).toString))
      PageId(page.toString) -> PaginatedResult(pageRecords, nextPage)
    }.toMap

    def readPage(pageId: PageId): IO[PaginatedResult] =
      IO.sleep(1.second).as(pages(pageId))

    def esLoad(batch: Seq[TargetRecord]): IO[Unit] =
      IO.sleep(500.milliseconds).void

    val job = StreamAdventures.readTransformAndLoadAndExecute(readPage, esLoad)

    val start            = System.currentTimeMillis()
    val recordsProcessed = Await.result(job.unsafeToFuture(), 1.minute)
    val duration         = System.currentTimeMillis() - start

    println(s"Processing took ${duration}ms")

    assert(duration < 22.seconds.toMillis)
    assertEquals(recordsProcessed, 100)