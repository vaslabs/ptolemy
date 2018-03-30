package org.vaslabs.ptolemy.store

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.sharding.ShardRegion
import org.vaslabs.ptolemy.images.tiff.ImageRow
import org.vaslabs.ptolemy.store.Image.model.ImageId

class Image private(val imageId: Image.model.ImageId, imageFractions: ActorRef) extends Actor{

  import Image.Protocol._
  import Image.model._

  var stripsInImage: Set[StripId] = Set.empty

  override def receive: Receive = initialisingImage orElse partialServing

  private def initialisingImage: Receive = {
    case storeMessage @ Store(data, ack) =>
      imageFractions ! StoreFraction(imageId, data, sender(), ack)
    case StoredFractionAck(stripId, replyTo, ackOpt) =>
      stripsInImage += stripId
      ackOpt.foreach(ack => replyTo ! ack)
  }

  private def partialServing: Receive = {
    case GetData(stripId) =>
      if (stripsInImage(stripId))
        imageFractions ! SendDataTo(sender())
      else
        sender() ! ImageDataNotFound
    case GetStripsAvailable =>
      sender() ! StripsAvailable(imageId, stripsInImage)
  }
}

object ImageFraction {

  import Image.Protocol.StoreFraction

  private final val stripsPerShard = 10

  object Sharding {
    val extractShardId: ShardRegion.ExtractShardId = {
      case StoreFraction(imageId, data, _, _) =>
        s"_${imageId}_${data.strip.value % stripsPerShard}"
    }
    val extractEntityId: ShardRegion.ExtractEntityId = {
      case storeMsg @ StoreFraction(_, data, _, _) =>
        (data.strip.toString, storeMsg)
    }
  }

}

object Image {
  def props(imageId: ImageId, storage: ActorRef): Props = Props(new Image(imageId, storage))


  private[store] object model {
    case class ImageId(value: String) extends AnyVal
    case class StripId(value: Int) extends AnyVal
    sealed trait ImageData

    case class TiffData(strip: StripId, data: Seq[ImageRow]) extends ImageData
  }
  object Protocol {
    import model._
    case class Store(tiffData: TiffData, ack: Option[Any] = None)
    case class GetData(stripId: StripId)
    case object GetStripsAvailable
    case class StripsAvailable(imageId: ImageId, strips: Set[StripId])

    sealed trait ImageDataError
    case object ImageDataNotFound extends ImageDataError

    private[store] case class StoreFraction(imageId: ImageId, tiffData: TiffData, replyTo: ActorRef, ack: Option[Any])
    private[store] case class StoredFractionAck(stripId: StripId, replyTo: ActorRef, msg: Option[Any])
    private[store] case class SendDataTo(replyTo: ActorRef)
  }



}