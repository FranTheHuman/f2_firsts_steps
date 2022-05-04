
import cats.effect.std.Queue
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all.*
import fs2.{Chunk, INothing, Pipe, Pull, Pure, Stream}

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


  override def run: IO[Unit] = compileStram



}