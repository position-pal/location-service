package io.github.positionpal.location.messages.experiments

import scala.concurrent.duration.DurationInt

import lepus.client.apis.NormalMessagingChannel
import lepus.protocol.domains.*
import com.comcast.ip4s.{host, port}
import lepus.client.*
import cats.effect.{IO, IOApp, Resource}

val connection: Resource[IO, Connection[IO]] = LepusClient[IO](
  host = host"localhost",
  port = port"5672",
  username = "guest",
  password = "admin",
  vhost = Path("/"),
  config = ConnectionConfig.default,
)

object HelloWorld extends IOApp.Simple:

  private val exchange = ExchangeName.default

  def app(con: Connection[IO]) = con.channel.use: ch =>
    for
      _ <- IO.println(con.capabilities.toFieldTable)
      _ <- ch.exchange.declare(ExchangeName("events"), ExchangeType.Topic)
      q <- ch.queue.declare(QueueName("hello-world"), autoDelete = false)
      q <- IO.fromOption(q)(new Exception())
      print = ch.messaging.consume[String](q.queue, mode = ConsumeMode.RaiseOnError(true)).printlns
      publish = fs2.Stream
        .awakeEvery[IO](1.second)
        .map(_.toMillis)
        .evalTap(l => IO.println(s"publishing $l"))
        .map(l => Message(l.toString))
        .evalMap(ch.messaging.publish(exchange, q.queue, _))
      _ <- IO.println(q)
      _ <- print.merge(publish).interruptAfter(10.seconds).compile.drain
    yield ()

  override def run: IO[Unit] = connection.use(app)
end HelloWorld

import dev.hnaderi.namedcodec.{of, CirceAdapter}
import io.circe.generic.auto.*
import lepus.circe.given
import lepus.std.*

object PubSub extends IOApp.Simple:

  enum Event:
    case Created(id: String)
    case Updated(id: String, value: Int)

  private val protocol = TopicDefinition(
    ExchangeName("pub_sub_example"),
    ChannelCodec.default(CirceAdapter.of[Event]),
    TopicNameEncoder.of[Event],
  )

  def publisher(con: Connection[IO]): fs2.Stream[IO, Unit] = for
    ch <- fs2.Stream.resource(con.channel)
    bus <- fs2.Stream.eval(EventChannel.publisher(protocol, ch))
    (toPublish, idx) <- fs2
      .Stream(
        Event.Created("b"),
        Event.Updated("a", 10),
        Event.Updated("b", 100),
        Event.Created("c"),
      )
      .zipWithIndex
    _ <- fs2.Stream.eval(bus.publish(ShortString.from(idx), toPublish))
  yield ()

  def consumer1(con: Connection[IO]): fs2.Stream[IO, Unit] = for
    ch <- fs2.Stream.resource(con.channel)
    bus <- fs2.Stream.eval(EventChannel.consumer(protocol)(ch))
    evt <- bus.events
    _ <- fs2.Stream.eval(IO.println(s"consumer 1: $evt"))
  yield ()

  def consumer2(con: Connection[IO]): fs2.Stream[IO, Unit] = for
    ch <- fs2.Stream.resource(con.channel)
    bus <- fs2.Stream.eval(EventChannel.consumer(protocol, ch, TopicSelector("Created")))
    evt <- bus.events
    _ <- fs2.Stream.eval(IO.println(s"consumer 2: $evt"))
  yield ()

  override def run: IO[Unit] = connection.use: con =>
    fs2.Stream(publisher(con), consumer1(con), consumer2(con)).parJoinUnbounded.interruptAfter(15.seconds).compile.drain

import cats.effect.{Async, ExitCode}

object WorkPool:

  final case class Task(value: String) derives io.circe.Codec.AsObject

  private val protocol = WorkPoolDefinition(
    QueueName("jobs"),
    ChannelCodec.plain(MessageCodec.json[Task]),
  )

  private val connectionRes = fs2.Stream.resource(connection)
  private val channel = connectionRes.flatMap(con => fs2.Stream.resource(con.channel))

  val server: fs2.Stream[IO, Unit] = channel
    .evalMap(WorkPoolChannel.publisher(protocol, _))
    .flatMap(pool => fs2.io.stdinUtf8(100)(using Async[IO]).map(Task(_)).evalMap(pool.publish))

  def worker(name: String): fs2.Stream[IO, Unit] = channel
    .evalMap(WorkPoolChannel.worker(protocol, _))
    .flatMap(pool => pool.jobs.evalMap(job => IO.println(s"worker $name: $job") >> pool.processed(job)))

  def run(args: List[String]): IO[ExitCode] =
    (args.map(_.toLowerCase) match
      case "server" :: _ => server
      case "worker" :: name :: _ => worker(name)
      case _ => fs2.Stream.exec(IO.println("""
        | Usage: workpool command
        | Commands:
        | - server
        | - worker <name>
        |""".stripMargin))
    ).compile.drain.as(ExitCode.Success)

object WorkPoolServer extends IOApp:
  override def run(args: List[String]): IO[ExitCode] =
    WorkPool.run("server" :: Nil)

object WorkPoolWorker1 extends IOApp:
  override def run(args: List[String]): IO[ExitCode] =
    WorkPool.run("worker" :: "1" :: Nil)

object WorkPoolWorker2 extends IOApp:
  override def run(args: List[String]): IO[ExitCode] =
    WorkPool.run("worker" :: "2" :: Nil)
