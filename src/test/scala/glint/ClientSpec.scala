package glint

import akka.actor.{ActorRef, ExtendedActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import breeze.linalg.DenseVector
import glint.exceptions.ModelCreationException
import glint.messages.master.ClientList
import glint.models.client.BigModel
import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import spire.implicits._

import scala.concurrent.duration._

/**
  * Client test specification
  */
class ClientSpec extends FlatSpec with SystemTest {


  "A client" should "register with master" in withMaster { master =>
    withClient { client =>
      val clientAddress = client.system.asInstanceOf[ExtendedActorSystem].provider.getDefaultAddress
      implicit val timeout = Timeout(30 seconds)
      val clientList = whenReady(master ? ClientList()) {
        case list: Array[ActorRef] => list
      }
      assert(clientList.exists(l =>
        l.path.toSerializationFormat == client.actor.path.toSerializationFormatWithAddress(clientAddress)))
    }
  }

  it should "register a created model with master" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel[Double]("test", 100, 0.0)) { identity }
        val bigModel2 = whenReady(client.get[Long, Double]("test")) { identity }
        assert(bigModel2.isInstanceOf[BigModel[_, _]])
      }
    }
  }

  it should "fail to register a model when there are no servers" in withMaster { _ =>
    withClient { client =>
      whenReady(client.denseScalarModel("test", 100, 0.0).failed) {
        case e => e shouldBe a[ModelCreationException]
      }
    }
  }

  it should "be able to create models with less keys than servers" in withMaster { _ =>
    withServer { server1 =>
      withServer { server2 =>
        withServer { server3 =>
          withClient { client =>
            val model = whenReady(client.denseScalarModel[Double]("test", 1, 0.0)) { identity }
            assert(model.isInstanceOf[BigModel[_, _]])
          }
        }
      }
    }
  }

  it should "be able to create models with more keys than servers" in withMaster { _ =>
    withServer { server1 =>
      withServer { server2 =>
        withServer { server3 =>
          withClient { client =>
            val bigModel = whenReady(client.denseScalarModel[Double]("test", 1327, 0.0)) { identity }
            assert(bigModel.isInstanceOf[BigModel[_, _]])
          }
        }
      }
    }
  }

  it should "be able to create a scalar model with Double primitives" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel[Double]("test", 100, 0.3)) { identity }
        assert(bigModel.isInstanceOf[BigModel[_, _]])
      }
    }
  }

  it should "be able to create a scalar model with Int primitives" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel[Int]("test", 100, 12)) { identity }
        assert(bigModel.isInstanceOf[BigModel[_, _]])
      }
    }
  }

  it should "be able to create a scalar model with Long primitives" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseScalarModel[Long]("test", 100, 89990009991L)) { identity }
        assert(bigModel.isInstanceOf[BigModel[_, _]])
      }
    }
  }

  it should "be able to create a vector model with Double primitives" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseVectorModel[Double]("test", 100, DenseVector.ones[Double](10))) { identity }
        assert(bigModel.isInstanceOf[BigModel[_, _]])
      }
    }
  }

  it should "be able to create a vector model with Int primitives" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseVectorModel[Int]("test", 100, DenseVector.ones[Int](12))) { identity }
        assert(bigModel.isInstanceOf[BigModel[_, _]])
      }
    }
  }

  it should "be able to create a vector model with Long primitives" in withMaster { _ =>
    withServer { _ =>
      withClient { client =>
        val bigModel = whenReady(client.denseVectorModel[Long]("test", 100, DenseVector.ones[Long](14))) { identity }
        assert(bigModel.isInstanceOf[BigModel[_, _]])
      }
    }
  }

}
