package adventures.io

import cats.Id
import cats.effect.IO
import cats.effect.kernel.Outcome.{Errored, Succeeded}
import cats.effect.testkit.{TestControl, TestException}
import munit.CatsEffectSuite

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.*

class IOAdventuresTests extends CatsEffectSuite:
  def logger(logged: ListBuffer[String])(log: String): Unit = logged.append(log)

  test("immediatelyExecutingTask should return 43"):
    val result = IOAdventures.immediatelyExecutingTask()
    result.assertEquals(43)

  test("helloWorld should have no side effect if not executed"):
    val logged = ListBuffer[String]()

    IOAdventures.helloWorld(logger(logged))

    assertEquals(logged.toList, List())

  test("helloWorld should have single side effect if run once"):
    val logged = ListBuffer[String]()

    IOAdventures.helloWorld(logger(logged)).unsafeRunSync()

    assertEquals(logged.toList, List("hello world"))

  test("helloWorld should have two side effects if run twice"):
    val logged = ListBuffer[String]()

    val helloWorldTask = IOAdventures.helloWorld(logger(logged))
    helloWorldTask.unsafeRunSync()
    helloWorldTask.unsafeRunSync()

    assertEquals(logged.toList, List("hello world", "hello world"))

  test("alwaysFailingTask should fail"):
    val subj = IOAdventures.alwaysFailingTask().attempt.unsafeRunSync()
    assert(subj.isLeft)

  test("getCurrentTempInF should get current temp in F"):
    val result = IOAdventures.getCurrentTempInF(() => IO(45))
    result.assertEquals(113)

  test("getCurrentTempInFAgain should get current temp in F again"):
    val currentTemp = () => IO(45)

    def cToF(c: Int) = IO {
      c * 9 / 5 + 32
    }

    IOAdventures.getCurrentTempInFAgain(currentTemp, cToF).assertEquals(113)

  test("calculateStringComplexityInParallel should calculate string complexity in parallel"):
    val source = List("a", "b", "c", "e", "f", "g")

    def complexity(string: String) =
      IO.sleep(1.second).as(string.length)

    val calc: IO[Int] = IOAdventures.calculateStringComplexityInParallel(source, complexity)

    val result = calc.unsafeRunTimed(2.seconds)

    assertEquals(result, Some(6))

  test("calculateStringComplexityInParallelAgain should calculate string complexity in parallel again"):
    val source = List("a", "b", "c", "e", "f", "g")

    def complexity(string: String) =
      IO.sleep(1.second).as(string.length)

    val calc: IO[Int] = IOAdventures.calculateStringComplexityInParallelAgain(source, complexity)

    val result = calc.unsafeRunTimed(2.seconds)

    assertEquals(result, Some(6))

  test("retryOnFailure should not retry on success"):
    var calls = 0
    IOAdventures.retryOnFailure(IO { calls = calls + 1 }, 10, 1.second).unsafeRunSync()

    assertEquals(calls, 1)

  def failUntil(count: Int): Int => IO[Unit] = i =>
    if i < count then IO.raiseError(TestException(i))
    else IO.unit

  test("retryOnFailure should not retry before delay"):
    var calls         = 0
    val incrementCall = IO { calls = calls + 1; calls }

    val subj = IOAdventures.retryOnFailure(incrementCall.flatMap(failUntil(100)), 10, 1.second)

    TestControl.execute(subj).flatMap { control =>
      for
        _ <- assertIO(IO(calls), 0)
        _ <- control.advanceAndTick(100.millis)
        _ <- assertIO(IO(calls), 1)
        _ <- control.advanceAndTick(999.millis)
        _ <- assertIO(IO(calls), 1)
      yield ()
    }

  test("retryOnFailure should retry after delay"):
    var calls         = 0
    val incrementCall = IO { calls = calls + 1; calls }

    val subj = IOAdventures.retryOnFailure(incrementCall.flatMap(failUntil(100)), 10, 1.second)

    TestControl.execute(subj).flatMap { control =>
      for
        _ <- assertIO(IO(calls), 0)
        _ <- control.advanceAndTick(100.millis)
        _ <- assertIO(IO(calls), 1)
        _ <- control.advanceAndTick(1.second)
        _ <- assertIO(IO(calls), 2)
      yield ()
    }

  test("retryOnFailure should retry 10 times and return value"):
    var calls         = 0
    val incrementCall = IO { calls = calls + 1; calls }

    val subj = IOAdventures.retryOnFailure(incrementCall.flatMap(failUntil(10)), 10, 1.second)

    TestControl.execute(subj).flatMap { control =>
      for
        _ <- assertIO(IO(calls), 0)
        _ <- control.advanceAndTick(100.millis)
        _ <- assertIO(IO(calls), 1)
        _ <- 0.until(10).foldLeft(IO.unit)((acc, _) => acc.flatMap(_ => control.advanceAndTick(1.second)))
        _ <- assertIO(IO(calls), 10)
        _ <- control.results.assertEquals(Some(Succeeded(Id(()))))
      yield ()
    }

  test("retryOnFailure should retry 10 times and fail"):
    var calls         = 0
    val incrementCall = IO { calls = calls + 1; calls }

    val subj = IOAdventures.retryOnFailure(incrementCall.flatMap(failUntil(12)), 10, 1.second)

    TestControl.execute(subj).flatMap { control =>
      for
        _ <- assertIO(IO(calls), 0)
        _ <- control.advanceAndTick(100.millis)
        _ <- assertIO(IO(calls), 1)
        _ <- 0.until(10).foldLeft(IO.unit)((acc, _) => acc.flatMap(_ => control.advanceAndTick(1.second)))
        _ <- assertIO(IO(calls), 11)
        _ <- control.results.assertEquals(Some(Errored(TestException(11))))
      yield ()
    }
