package org.vaslabs.ptolemy.store

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.vaslabs.ptolemy.store.Image.Protocol.StoredFractionAck
import org.vaslabs.ptolemy.store.Image.model.{ImageId, StripId, TiffData}

import scala.concurrent.ExecutionContext

class ImageSpec extends TestKit(ActorSystem("PtolemyTestSystem")) with WordSpecLike with Matchers
    with BeforeAndAfterAll with ImplicitSender
{

  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global

  override def afterAll(): Unit = system.terminate().map(println)

  "Image actor" can {
    case object Ack
    val storage = TestProbe()
    val imageActor: TestActorRef[Image] = TestActorRef(Image.props(ImageId("img"), storage.ref))
    val dummyData = TiffData(StripId(1), List.empty)
    "delegate the storage to the actor and forward acknowledgment of storing" in {
      imageActor ! Image.Protocol.Store(dummyData, Some(Ack))
      storage.expectMsg(Image.Protocol.StoreFraction(ImageId("img"), dummyData, self, Some(Ack)))
      storage.reply(StoredFractionAck(StripId(1), self, Some(Ack)))
      expectMsg(Ack)
    }

    "delegates the data retrieval to the storage actor and forwards result" in {
      imageActor ! Image.Protocol.GetData(StripId(1))
      storage.expectMsg(Image.Protocol.SendDataTo(self))
    }
  }
}
