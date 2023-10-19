package adventures.io

import cats.*
import cats.implicits.*
import cats.effect.*
import cats.effect.implicits.*

import scala.concurrent.duration.*

/** If a result 'A' is available synchronously, then that same result asynchronously could be represented as a 'IO[A]'
  */
object IOAdventures:

  /**   1. Create a IO which returns 43
    */
  def immediatelyExecutingTask(): IO[Int] =
    IO(43)

  /** 2. Create a IO which when executed logs “hello world” (using `logger`) */
  def helloWorld(logger: String => Unit): IO[Unit] =
    IO(logger("hello world"))

  /** 3. Create a IO which always fails.
    */
  def alwaysFailingTask(): IO[Unit] =
    IO.raiseError(new RuntimeException("boom"))

  /** 4. There is 1 remote service which will return you an effect that provides the current temperature in celsius.
    */
  def getCurrentTempInF(currentTemp: () => IO[Int]): IO[Int] =
    // def cToF(c: Int) = c * 9 / 5 + 32
    currentTemp().map(c => c * 9 / 5 + 32)

  /** 5. There is 1 remote service which will return you a task that provides the current temperature in celsius. The
    * conversion is complex so we have decided to refactor it out to another remote microservice. Make use of both of
    * these services to return the current temperature in fahrenheit.
    */
  def getCurrentTempInFAgain(currentTemp: () => IO[Int], converter: Int => IO[Int]): IO[Int] =
    currentTemp().flatMap(converter)

  /** 6. Computing the complexity of a string is a very expensive op. Implement this function so that complexity of the
    * calculation will be done in parallel. Sum the returned results to provide the overall complexity for all Strings.
    * (try using operations from monix)
    */
  def calculateStringComplexityInParallel(strings: List[String], complexity: String => IO[Int]): IO[Int] =
    strings.parTraverse(complexity).map(_.sum)

  /** 6.b. As above, but try to implement the parallel processing using the Applicative instance for IO and the
    * cats `sequence` function. (if you haven't heard of cats / sequence skip this - even if you have consider this as
    * optional).
    */
  def calculateStringComplexityInParallelAgain(strings: List[String], complexity: String => IO[Int]): IO[Int] =
    strings.map(complexity).parSequence.map(_.sum)
    // NOTE: Uh why not just parTraverse above, is this needed? is there a different way the first one should be done
    // and this one should be parTraverse??

  /** 7. Write a function which given a IO, will reattempt that effect after a specified delay for a maximum number of
    * attempts if the supplied IO fails.
    */
  def retryOnFailure[T](t: IO[T], maxRetries: Int, delay: FiniteDuration): IO[T] =
    t.handleErrorWith { e =>
      if maxRetries > 0 then
        IO.sleep(delay) *> retryOnFailure(t, maxRetries - 1, delay)
      else
        IO.raiseError(e)
    }
