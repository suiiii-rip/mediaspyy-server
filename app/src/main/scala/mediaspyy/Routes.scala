package mediaspyy

import org.http4s.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import org.http4s.Request

import zio.interop.catz._
import zio._
import cats.data.Kleisli
import zio.logging._

import io.circe.generic.auto._
import io.circe.Decoder
import io.circe.Encoder
import org.http4s.circe._
import org.http4s.EntityDecoder
import org.http4s.EntityEncoder

import MediaService._
import AuthenticationService._

import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import org.http4s.AuthedRoutes
import cats.data.OptionT
import org.http4s.BasicCredentials
import org.http4s.server.middleware.authentication.BasicAuth

trait ApiRoutes[R <: MediaService with AuthenticationService with Logging] {

  type ApiTask[A] = RIO[R, A]
  type Err = String

  implicit def circeJsonDecoder[A](implicit
      decoder: Decoder[A]
  ): EntityDecoder[ApiTask, A] = jsonOf[ApiTask, A]
  implicit def circeJsonEncoder[A](implicit
      decoder: Encoder[A]
  ): EntityEncoder[ApiTask, A] = jsonEncoderOf[ApiTask, A]

  lazy val dsl: Http4sDsl[ApiTask] = Http4sDsl[ApiTask]
  import dsl._

  object resultSizeParam
      extends OptionalQueryParamDecoderMatcher[Int]("resultSize")

  val authedMiddleware: AuthMiddleware[ApiTask, User] =
    BasicAuth("mediaspyy", b => authentcate(b.username, b.password))

  def routes: HttpRoutes[ApiTask] = authedMiddleware(authedRoutes)

  def authedRoutes = AuthedRoutes.of[User, ApiTask] {
    case GET -> Root / "media" :? resultSizeParam(size) as user =>
      list(user, size.getOrElse(10))
        .foldM(
          e =>
            log.throwable("Request failed with error", e) *>
              InternalServerError("sorry"),
          Ok(_)
        )
    case req @ POST -> Root / "media" as user =>
      req.req.decode[BasicMediaData](m =>
        createMedia(user, m)
          .foldM(
            e =>
              log.throwable("Request failed with error", e) *>
                InternalServerError("sorry"),
            _ => Ok()
          )
      )
  }

}
