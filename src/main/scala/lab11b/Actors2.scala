package lab11b

import akka.actor._

import scala.util.Random._
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.{Await, ExecutionContext, Future}
import akka.util.Timeout
import akka.pattern.ask

import scala.util.Success
 
//////////////////////////////////////////
// Introduction to Scala (Akka) Actors  //
//////////////////////////////////////////


/**
 * Actor:
 * - an object with identity
 * - with a behavior
 * - interacting only via asynchronous messages
 *
 * Consequently: actors are fully encapsulated / isolated from each other
 * - the only way to exchange state is via messages (no global synchronization)
 * - all actors run fully concurrently 
 *
 * Messages:
 * - are received sequentially and enqueued
 * - processing one message is atomic
 *
**/


/**
 * type Receive = PartialFunction[Any, Unit]
 *           Any  -> any message can arrive
 *           Unit -> the actor can do something, but does not return anything
 *            
 * trait Actor {
 *     implicit val self: ActorRef
 *     def receive: Receive
 *     ...
 * }
 *
 * API documentation: http://doc.akka.io/api/akka/2.2.3
 * 
**/


/**
 * Logging options: read article
 * http://doc.akka.io/docs/akka/snapshot/scala/logging.html
 *
 * a) ActorLogging
 * class MyActor extends Actor with akka.actor.ActorLogging {
 *  ...
 * }
 *
 * b) LoggingReceive
 *
 *  def receive = LoggingReceive {
 *     ....
 *  }
 * 
 * Hint: in order to enable logging you can also pass the following arguments to the VM
 * -Dakka.loglevel=DEBUG -Dakka.actor.debug.receive=on
 * (In Eclipse: Run Configuraiton -> Arguments / VM Arguments)
 *
**/
object Wiadomosc {
  case object Obroniona
  case object Bramka
  case object Kopnij
  case object Init
  case class Init(actorRef: ActorRef)
  case object Stop
  case class Stop(kraj1: ActorRef, kraj2: ActorRef)
  case class Wynik(score: Int)
}

class Kraj(name: String) extends Actor {
  import Wiadomosc._
  //implicit val ec: ExecutionContext = context.dispatcher
  //val system = ActorSystem("Reactive1")

  override def receive: Receive = play
  private var score = 0

  def play: Receive = {
    case Init(opponent: ActorRef) =>
        opponent ! Kopnij

    case Kopnij =>
      println(name + " kopie!")
      sender ! (if (nextBoolean()) {score += 1; Bramka} else Obroniona)

      sender ! Kopnij

    case Bramka =>
      println(name + " strzelił bramkę")

    case Obroniona =>
      println(name + " nie trafił")

    case Stop =>
      sender() ! Wynik(score)
      context become finished
  }

  def finished: Receive = {
    case _ => println("mecz się skończył")
  }
}

class Mecz extends Actor {
  import Wiadomosc._
  import system.dispatcher
  implicit val timeout: Timeout = 5 seconds

  val system = akka.actor.ActorSystem("system")

  override def receive: Receive = {
    case Init =>
      val kraj1 = context.actorOf(Props(new Kraj("Poland")), "kraj1")
      val kraj2 = context.actorOf(Props(new Kraj("Russian")), "kraj2")

      val starts = if (nextBoolean()) kraj1 else kraj2

      if (starts == kraj1)
        kraj1 ! Init(kraj2)
      else
        kraj2 ! Init(kraj1)

      context.system.scheduler.scheduleOnce(2 seconds) {
        self ! Stop(kraj1, kraj2)
      }

    case Stop(kraj1, kraj2) =>
      val f1 = ask(kraj1, Stop)
      val f2 = ask(kraj2, Stop)

      val x: Future[(Wynik, Wynik)] = for {
        home ← f1.mapTo[Wynik]
        guest ← f2.mapTo[Wynik]
      } yield (home, guest)

      x.onComplete {
        case Success((home, guest)) => println(guest.score + ":" + home.score)
          context.system.terminate
      }


  }
}

object ApplicationMain extends App {
  import Wiadomosc._

  val system = ActorSystem("system")
  val mainActor = system.actorOf(Props[Mecz], "mainActor")

  mainActor ! Init


  Await.result(system.whenTerminated, Duration.Inf)
}