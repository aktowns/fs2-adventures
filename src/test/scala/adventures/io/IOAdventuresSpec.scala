package adventures.io

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.specs2.execute.Result
import org.specs2.mutable.Specification

import scala.collection.mutable.ListBuffer

class IOAdventuresSpec extends Specification:
  def logger(logged: ListBuffer[String])(log: String): Unit = logged.append(log)

  "TaskAdventures" should {
    "immediately executing Task should return 43" in {
      IOAdventures.immediatelyExecutingTask().unsafeRunSync() must beEqualTo(43)
    }

    "side effectful helloWorld Task" should {
      "have no side effect if not executed" in {
        val logged = ListBuffer[String]()

        IOAdventures.helloWorld(logger(logged))

        logged.toList must beEmpty
      }

      "have single side effect if run once" in {
        val logged = ListBuffer[String]()

        IOAdventures.helloWorld(logger(logged)).unsafeRunSync()

        logged.toList must beEqualTo(List("hello world"))
      }

      "have two side effects if run twice" {
        val logged = ListBuffer[String]()

        val helloWorldTask = IOAdventures.helloWorld(logger(logged))
        helloWorldTask.unsafeRunSync()
        helloWorldTask.unsafeRunSync()

        logged.toList must beEqualTo(List("hello world", "hello world"))
      }
    }

    "failing Task should fail" in {
      IOAdventures.alwaysFailingTask().unsafeRunSync() must throwA[Exception]()
    }

    "get Current Temp In F" in {
      IOAdventures.getCurrentTempInF(() => IO(45)).unsafeRunSync() must beEqualTo(113)
    }

    "get Current Temp In F again" in {
      val currentTemp = () => IO(45)

      def cToF(c: Int) = IO {
        c * 9 / 5 + 32
      }

      IOAdventures.getCurrentTempInFAgain(currentTemp, cToF).unsafeRunSync() must beEqualTo(113)
    }

    // "calculate string complexity in parallel" in {
    //   val source = List("a", "b", "c", "e", "f", "g")

    //   def complexity(string: String) =
    //     IO.sleep(1.second).as(string.length)

    //   val task: IO[Int] =
    //     IOAdventures.calculateStringComplexityInParallel(source, complexity)

    //   val scheduler = Scheduler.fixedPool(name = "test1", poolSize = 10)

    //   val result = task.unsafeToFuture(scheduler)

    //   // if not run in parallel this will timeout.
    //   Await.result(result, 2.seconds) must beEqualTo(6)
    // }

    // "calculate string complexity in parallel again" in {
    //   val source = List("a", "b", "c", "e", "f", "g")

    //   def complexity(string: String) =
    //     IO.sleep(1.second).as(string.length)

    //   val task: IO[Int] = IOAdventures
    //     .calculateStringComplexityInParallelAgain(source, complexity)

    //   val scheduler = Scheduler.fixedPool(name = "test2", poolSize = 10)

    //   val result = task.unsafeToFuture(scheduler)

    //   // if not run in parallel this will timeout.
    //   Await.result(result, 2.seconds) must beEqualTo(6)
    // }

    // "retry on failure" should {
    //   "not retry on success" in new RetryScope(0):
    //     result(task) must beEqualTo(10)

    //     calls must beEqualTo(1)

    //   "not retry before delay" in new RetryScope(1):
    //     task.unsafeToFuture(scheduler)

    //     calls must beEqualTo(1)

    //   "retry after one failure with delay" in new RetryScope(1):
    //     result(task) must beEqualTo(20)

    //     scheduler.tick(1.second)

    //     calls must beEqualTo(2)

    //   "return success result of 10th retry" in new RetryScope(10):
    //     task.unsafeToFuture(scheduler)

    //     scheduler.tick(10.seconds)

    //     calls must beEqualTo(11)

    //   "return failure result of 10th retry" in new RetryScope(11):
    //     val futureResult = task.unsafeToFuture(scheduler)

    //     scheduler.tick(10.seconds)

    //     calls must beEqualTo(11)

    //     Await
    //       .result(futureResult, 1.second) must throwA[IllegalArgumentException]

    //   "give up after 10 retries" in new RetryScope(100):
    //     task.unsafeToFuture(scheduler)

    //     scheduler.tick(1.minute)

    //     calls must beEqualTo(11)
    // }
  }

  // class RetryScope(failures: Int) extends Scope:
  //   var calls = 0
  //   private val taskToRetry = IO
  //     .eval {
  //       calls = calls + 1
  //       calls
  //     }
  //     .flatMap { i =>
  //       if i <= failures then Task.raiseError(new IllegalArgumentException(s"failing call $i"))
  //       else Task.now(i * 10)
  //     }

  //   val scheduler = TestScheduler()

  //   val task = IOAdventures.retryOnFailure(taskToRetry, 10, 1.second)

  // private def result[T](task: IO[T]): T =
  //   import monix.execution.Scheduler.Implicits.global

  //   Await.result(task.runToFuture, 2.seconds)
