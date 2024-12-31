package io.github.positionpal.location.tracking.utils

import scala.concurrent.TimeoutException
import scala.concurrent.duration.{Duration, DurationInt}

import akka.Done
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.{BootstrapSetup, CoordinatedShutdown}
import cats.effect
import cats.effect.implicits.genTemporalOps
import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.effect.{Deferred, Resource}
import cats.implicits.{catsSyntaxApplicativeError, toFlatMapOps, toFunctorOps}
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

object AkkaUtils:

  /** Starts up a typed Akka Actor System in the context of a Cats-Effect [[Resource]],
    * integrating the two runtimes with their cancellation abilities.
    * @param config The HOCON actor system configuration.
    * @param actorSystemName The name of the actor system. By default, it is `ClusterSystem`.
    * @param timeoutAwaitCatsEffect The maximum amount of time Akka's coordinated-shutdown is
    *  allowed to wait for Cats-Effect to finish.
    * @param timeoutAwaitAkkaTermination The maximum amount of time to wait for the actor system
    *  to terminate, after `terminate()` was called.
    * @param behavior The behavior of the actor system.
    * @tparam F The [[Async]] effect type.
    * @tparam T The type of the actor system.
    * @return A Cats-Effect [[Resource]] that starts up the actor system and takes care of its termination.
    *
    * @see <a href="https://alexn.org/blog/2023/04/17/integrating-akka-with-cats-effect-3/">Integrating Akka with Cats-Effect 3</a>
    */
  def startup[F[_]: Async, T](
      config: Config,
      actorSystemName: String = "ClusterSystem",
      timeoutAwaitCatsEffect: Duration = 20.seconds,
      timeoutAwaitAkkaTermination: Duration = 20.seconds,
  )(behavior: => Behavior[T]): Resource[F, ActorSystem[T]] =
    Dispatcher.parallel[F](await = true).flatMap: dispatcher =>
      Resource:
        for
          ec <- Async[F].executionContext // fishing Async `ExecutionContext`
          awaitCancel <- Deferred[F, Unit] // for synchronizing Cats-Effect with Akka
          awaitTermination <- Deferred[F, Unit] // for awaiting termination via coordinated-shutdown
          configuration <- Async[F].pure(BootstrapSetup(config).withDefaultExecutionContext(ec))
          logger = LoggerFactory.getLogger(getClass)
          system <- Async[F].delay:
            logger.info("Creating actor system...")
            val sys = ActorSystem(behavior, actorSystemName, configuration)
            // Registering task in Akka's CoordinatedShutdown that will wait for Cats-Effect to catch up,
            // blocking Akka from terminating, see:
            // https://doc.akka.io/docs/akka/current/coordinated-shutdown.html.
            CoordinatedShutdown(sys).addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "cats-effect-sync"): () =>
              dispatcher.unsafeToFuture:
                // WARN: this may not happen, if Akka decided to terminate, and
                // `coordinated-shutdown.exit-jvm` configuration **isn't** `on`, hence the timeout.
                awaitCancel.get.timeout(timeoutAwaitCatsEffect).recoverWith:
                  case _: TimeoutException =>
                    Async[F].delay:
                      logger.error(
                        """
                          |Timed out waiting for Cats-Effect to catch up!
                          |This might indicate either a non-terminating cancellation logic,
                          |or a misconfiguration of Akka.
                          |""".stripMargin,
                      )
                .as(Done)
            CoordinatedShutdown(sys).addTask(CoordinatedShutdown.PhaseActorSystemTerminate, "system-terminated"): () =>
              dispatcher.unsafeToFuture(awaitTermination.complete(()).as(Done))
            sys
        yield
          val cancel =
            for
              // Signals that Cats-Effect has caught up with Akka
              _ <- awaitCancel.complete(())
              _ <- Async[F].delay(logger.warn("Shutting down actor system!"))
              // Shuts down Akka, and waits for its termination.
              // Here, system.terminate() returns a `Future[Terminated]`, but we are ignoring it,
              // as it could be non-terminating
              _ <- Async[F].delay(system.terminate())
              // Waiting for Akka to terminate via coordinated-shutdown
              _ <- Async[F].delay(awaitTermination.get)
              // WARN: `whenTerminated` is unreliable, hence the timeout
              _ <- Async[F].fromFuture(Async[F].pure(system.whenTerminated))
                .timeoutAndForget(timeoutAwaitAkkaTermination).void
                .handleErrorWith(_ => Async[F].delay(logger.warn("Timed-out waiting for Akka to terminate!")))
            yield ()
          (system, cancel)
