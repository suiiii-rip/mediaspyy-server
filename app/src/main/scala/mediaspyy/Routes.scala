package mediaspyy

import org.http4s.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes
import org.http4s.Request

import zio.interop.catz._
import zio._
import cats.data.Kleisli

import io.circe.generic.auto._
import io.circe.Decoder
import io.circe.Encoder
import org.http4s.circe._
import org.http4s.EntityDecoder
import org.http4s.EntityEncoder

import MediaStorage._
import AuthenticationService._

import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import org.http4s.AuthedRoutes
import cats.data.OptionT
import org.http4s.BasicCredentials

trait ApiRoutes[R <: MediaStorage with AuthenticationService] {

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

  val authUser: Kleisli[ApiTask, Request[ApiTask], Either[Err, User]] =
    Kleisli(req => {
      req.headers.get(Authorization) match {
        case None => RIO(Left("nope"))
        case Some(h) =>
          h.credentials match {
            case BasicCredentials(user, password) =>
              authentcate(user, password).map(_.toRight("nope2"))
            case _ => RIO(Left("nope3"))
          }
      }
    })

  val authFailure: AuthedRoutes[Err, ApiTask] =
    AuthedRoutes(req => OptionT.liftF(Forbidden("just no")))

  val authedMiddleware: AuthMiddleware[ApiTask, User] =
    AuthMiddleware(authUser, authFailure)

  def routes: HttpRoutes[ApiTask] = authedMiddleware(authedRoutes)

  def authedRoutes = AuthedRoutes.of[User, ApiTask] {
    case GET -> Root / "media" :? resultSizeParam(size) as user =>
      list(size.getOrElse(10)).foldM(_ => BadRequest(), Ok(_))
    case req @ POST -> Root / "media" as user =>
      req.req.decode[MediaData](m =>
        createMedia(m).foldM(_ => BadRequest(), _ => Ok())
      )
  }

}
