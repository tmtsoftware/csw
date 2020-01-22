package csw.aas.core

import csw.aas.core.token.AccessToken
import csw.aas.core.token.claims.{Access, Audience, Authorization, Permission, TokenSubsystems}
import csw.prefix.codecs.CommonCodecs
import csw.prefix.models.Subsystem
import io.bullet.borer.Dom.{ArrayElem, Element, NullElem, StringElem}
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec
import io.bullet.borer.{Codec, Decoder, Dom, Encoder}

trait AuthCodecs extends CommonCodecs {
  implicit lazy val accessCodec: Codec[Access]               = deriveCodec
  implicit lazy val authorizationCodec: Codec[Authorization] = deriveCodec
  implicit lazy val permissionCodec: Codec[Permission]       = deriveCodec
  implicit lazy val accessTokenCodec: Codec[AccessToken]     = deriveCodec

  implicit lazy val audienceEnc: Encoder[Audience] = implicitly[Encoder[Element]].contramap[Audience] { audience =>
    audience.value match {
      case head :: Nil => StringElem(head)
      case Nil         => NullElem
      case xs          => ArrayElem.Unsized(xs.map(StringElem): _*)
    }
  }

  implicit lazy val audienceDec: Decoder[Audience] = implicitly[Decoder[Element]].map[Audience] {
    case Dom.NullElem                    => Audience()
    case Dom.StringElem(x) if x.isBlank  => Audience()
    case Dom.StringElem(value)           => Audience(value)
    case Dom.ArrayElem.Unsized(elements) => Audience(elements.collect { case StringElem(value) => value })
    case _                               => throw new RuntimeException("parsing failed due to invalid value")
  }

  implicit lazy val tokenSubsystemsEnc: Encoder[TokenSubsystems] = implicitly[Encoder[Element]].contramap[TokenSubsystems] { ts =>
    ts.values.toList match {
      case Nil  => NullElem
      case list => StringElem(list.map(_.entryName).mkString(", "))
    }
  }

  implicit lazy val tokenSubsystemsDec: Decoder[TokenSubsystems] = implicitly[Decoder[Element]].map[TokenSubsystems] {
    case Dom.NullElem => TokenSubsystems()
    case Dom.StringElem(str) =>
      TokenSubsystems(
        str
          .split(",")
          .map(_.trim)
          .flatMap(Subsystem.withNameInsensitiveOption)
          .toSet
      )
    case _ => throw new RuntimeException("parsing failed due to invalid value")
  }
}
