package sttp.tapir.server.vertx.cats

import cats.effect.Sync
import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import io.vertx.core.logging.{Logger, LoggerFactory}
import sttp.tapir.server.interceptor.log.{DefaultServerLog, ServerLog}
import sttp.tapir.server.interceptor.{CustomiseInterceptors, Interceptor}
import sttp.tapir.server.vertx.VertxServerOptions
import sttp.tapir.{Defaults, TapirFile}

final case class VertxCatsServerOptions[F[_]](
    dispatcher: Dispatcher[F],
    uploadDirectory: TapirFile,
    deleteFile: TapirFile => F[Unit],
    maxQueueSizeForReadStream: Int,
    interceptors: List[Interceptor[F]]
) extends VertxServerOptions[F] {
  def prependInterceptor(i: Interceptor[F]): VertxCatsServerOptions[F] =
    copy(interceptors = i :: interceptors)
  def appendInterceptor(i: Interceptor[F]): VertxCatsServerOptions[F] =
    copy(interceptors = interceptors :+ i)
}

object VertxCatsServerOptions {

  /** Allows customising the interceptors used by the server interpreter. */
  def customiseInterceptors[F[_]: Async](
      dispatcher: Dispatcher[F]
  ): CustomiseInterceptors[F, VertxCatsServerOptions[F]] =
    CustomiseInterceptors(
      createOptions = (ci: CustomiseInterceptors[F, VertxCatsServerOptions[F]]) =>
        VertxCatsServerOptions(
          dispatcher,
          VertxServerOptions.uploadDirectory(),
          file => Sync[F].delay(Defaults.deleteFile()(file)),
          maxQueueSizeForReadStream = 16,
          ci.interceptors
        )
    ).serverLog(defaultServerLog(LoggerFactory.getLogger("tapir-vertx")))

  def default[F[_]: Async](dispatcher: Dispatcher[F]): VertxCatsServerOptions[F] = customiseInterceptors(dispatcher).options

  def defaultServerLog[F[_]: Async](log: Logger): ServerLog[F] = {
    DefaultServerLog(
      doLogWhenReceived = debugLog(log)(_, None),
      doLogWhenHandled = debugLog[F](log),
      doLogAllDecodeFailures = infoLog[F](log),
      doLogExceptions = (msg: String, ex: Throwable) => Sync[F].delay { log.error(msg, ex) },
      noLog = Async[F].pure(())
    )
  }

  private def debugLog[F[_]: Async](log: Logger)(msg: String, exOpt: Option[Throwable]): F[Unit] = Sync[F].delay {
    VertxServerOptions.debugLog(log)(msg, exOpt)
  }

  private def infoLog[F[_]: Async](log: Logger)(msg: String, exOpt: Option[Throwable]): F[Unit] = Sync[F].delay {
    VertxServerOptions.infoLog(log)(msg, exOpt)
  }
}
