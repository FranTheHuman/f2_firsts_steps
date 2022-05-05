
import cats.effect.std.Queue
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all.*
import fs2.{Chunk, INothing, Pipe, Pull, Pure, Stream}
import scala.concurrent.duration._

import scala.util.Random

object F2Demo extends IOApp.Simple {

  case class Actor(id: Int, firstName: String, lastName: String)

  object Data {
    // Justice League
    val henryCavil: Actor = Actor(0, "Henry", "Cavill")
    val galGodot: Actor = Actor(1, "Gal", "Godot")
    val ezraMiller: Actor = Actor(2, "Ezra", "Miller")
    val benFisher: Actor = Actor(3, "Ben", "Fisher")
    val rayHardy: Actor = Actor(4, "Ray", "Hardy")
    val jasonMomoa: Actor = Actor(5, "Jason", "Momoa")

    // Avengers
    val scarlettJohansson: Actor = Actor(6, "Scarlett", "Johansson")
    val robertDowneyJr: Actor = Actor(7, "Robert", "Downey Jr.")
    val chrisEvans: Actor = Actor(8, "Chris", "Evans")
    val markRuffalo: Actor = Actor(9, "Mark", "Ruffalo")
    val chrisHemsworth: Actor = Actor(10, "Chris", "Hemsworth")
    val jeremyRenner: Actor = Actor(11, "Jeremy", "Renner")
    val tomHolland: Actor = Actor(13, "Tom", "Holland")
    val tobeyMaguire: Actor = Actor(14, "Tobey", "Maguire")
    val andrewGarfield: Actor = Actor(15, "Andrew", "Garfield")
  }

  extension[A](io: IO[A]) {
    def debug: IO[A] = io.map { a =>
      println(s"[${Thread.currentThread().getName}] $a")
      a
    }
  }

  import Data._

  // PURE ----------------------------

  val jlActors: Stream[Pure, Actor] = Stream(
    henryCavil,
    galGodot,
    ezraMiller,
    benFisher,
    rayHardy,
    jasonMomoa
  )
  val tomHollandStram: Stream[Pure, Actor] = Stream.emit(tomHolland)
  val spiderMen = Stream.emit(List(tomHolland, andrewGarfield, tobeyMaguire))
  val jlActorList: List[Actor] = jlActors.toList // Aplicable for Strams[Pure, _]
  val jlActorVector: Vector[Actor] = jlActors.toVector

  // INFINITE --------------

  val infiniteJLActors: Stream[Pure, Actor] = jlActors.repeat
  val repeatedJLActorsList: List[Actor] = infiniteJLActors.take(12).toList

  // EFFECTFUL --------------
  val savingTomHolland: Stream[IO, Actor] = Stream.eval {
    IO {
      println("Saving actor tom holland into the database")
      Thread.sleep(1000)
      tomHolland
    }
  }

  // COMPILE --------------
  val compileStram: IO[Unit] = savingTomHolland.compile.drain

  //  CHUNKS --------------
  val avengersActors: Stream[Pure, Actor] = Stream.chunk(Chunk.array(Array(
    scarlettJohansson,
    robertDowneyJr,
    chrisEvans,
    markRuffalo,
    chrisHemsworth,
    jeremyRenner
  )))

  // TRANSFORMATIONS --------------
  val allSuperHeros = jlActors ++ avengersActors

  // FlatMap
  val printedJLActors: Stream[IO, Unit] = jlActors flatMap { actor =>
    Stream.eval(IO.println(actor))
  }

  // FlatMap + eval = evalMap
  val printedJLActors_3: Stream[IO, Unit] = jlActors.evalMap(IO.println)
  // FlatMap + eval while keeping the original type = evalTap
  val printedJLActors_4: Stream[IO, Actor] = jlActors.evalTap(IO.println)

  // PIPE -------------- = FUNCTION
  val actorToStringPipe: Pipe[IO, Actor, String] =
    inStream =>
      inStream.map(actor => s"${actor.firstName} ${actor.lastName}")

  def toConsole[A]: Pipe[IO, A, Unit] = inStream => inStream.evalMap(IO.println)

  val stringNamesPrinted = jlActors.through(actorToStringPipe).through(toConsole)

  // ERROR HANDLING --------------
  def saveToDatabase(actor: Actor): IO[Int] = IO {
    println(s"Saving $actor")
    if (Random.nextBoolean()) {
      throw new RuntimeException("Persistence Layer failed.")
    }
    println("Saved.")
    actor.id
  }

  val savedJLActors: Stream[IO, Int] = jlActors.evalMap(saveToDatabase)
  val errorHandledActors: Stream[IO, Int] = savedJLActors.handleErrorWith(error => Stream.emit(-1))

  // atempt
  val attemptedSavedJLActors: Stream[IO, Either[Throwable, Int]] = savedJLActors.attempt
  val attemptedProcessed = attemptedSavedJLActors.evalMap {
    case Left(error) => IO(s"Error: $error").debug
    case Right(value) => IO(s"Successfully processed actor id: $value").debug
  }

  // RESOURCE --------------
  case class DatabaseConnection(url: String)

  def acquireConnection(url: String): IO[DatabaseConnection] = IO {
    println("Getting DB connection")
    DatabaseConnection(url)
  }

  def release(connection: DatabaseConnection): IO[Unit] =
    IO.println(s"Releasing connection to ${connection.url}")

  // bracket pattern
  val managedJLActors: Stream[IO, Int] =
    Stream.bracket(acquireConnection("jdbc://mydatabase.com"))(release).flatMap { conn =>
      // process a stream using  this resource
      savedJLActors.evalTap(actorId => IO(s"Saving $actorId to ${conn.url}").debug)
    }

  // MERGE --------------
  val concurrentJlActors = jlActors.evalMap { actor => IO {
    Thread.sleep(400)
    actor
  }.debug }

  val concurrentAvengerActors = avengersActors.evalMap { actor => IO {
    Thread.sleep(200)
    actor
  }.debug }

  val mergedActors: Stream[IO, Actor] = concurrentJlActors.merge(concurrentJlActors)

  // concurrently
  // example: producer-consumer
  val queue: IO[Queue[IO, Actor]] = Queue.bounded(10)
  val concurrentSystem = Stream.eval(queue).flatMap { q =>
    // producer stream
    val producer: Stream[IO, Unit] =
      jlActors
        .evalTap(actor => IO(actor).debug)
        .evalMap(actor => q.offer(actor)) // enqueue
        .metered(1.second) // throttle at 1 effect per second
    // consumer stream
    val consumer: Stream[IO, Unit] =
      Stream
        .fromQueueUnterminated(q)
        .evalMap(actor => IO(s"Consumed actor $actor").debug.void)


    producer.concurrently(consumer)
  }

  override def run: IO[Unit] = concurrentSystem.compile.drain



}