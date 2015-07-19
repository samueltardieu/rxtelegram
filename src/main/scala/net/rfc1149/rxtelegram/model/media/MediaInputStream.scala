package net.rfc1149.rxtelegram.model.media

import java.io.InputStream
import org.apache.commons.io.IOUtils

import akka.http.scaladsl.model.MediaType

case class MediaInputStream(mediaType: MediaType, inputStream: InputStream, fileName: Option[String] = None) extends Media {

  override def toBodyPart(fieldName: String) =
    MediaData(mediaType, IOUtils.toByteArray(inputStream), fileName).toBodyPart(fieldName)

}
