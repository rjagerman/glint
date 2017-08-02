package glint

import akka.actor.{ActorRef, ExtendedActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import glint.exceptions.ModelCreationException
import glint.messages.master.ClientList
import glint.models.client.async.{AsyncBigMatrix, AsyncBigVector}
import glint.models.client.{BigMatrix, BigVector}
import org.scalatest.FlatSpec

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

  it should "fail to create a BigMatrix when there are no servers" in withMaster { _ =>
    withClient { client =>
      val thrown = intercept[ModelCreationException] {
        client.matrix[Long](100, 10)
      }
      assert(thrown.isInstanceOf[ModelCreationException])
    }
  }

  it should "be able to create a BigMatrix with less rows than servers" in withMaster { _ =>
    withServers(3) { case _ =>
      withClient { client =>
        val model = client.matrix[Long](2, 10)
        assert(model.isInstanceOf[BigMatrix[Long]])
      }
    }
  }

  it should "be able to create a BigMatrix with more rows than servers" in withMaster { _ =>
    withServers(2) { _ =>
      withClient { client =>
        val model = client.matrix[Long](49, 7)
        assert(model.isInstanceOf[BigMatrix[Long]])
      }
    }
  }

  it should "be able to create a BigMatrix with one partial model per server" in withMaster { _ =>
    withServers(2) { _ =>
      withClient { client =>
        val model = client.matrix[Long](49, 7, 1)
        assert(model.isInstanceOf[BigMatrix[Long]])
        assert(model.asInstanceOf[AsyncBigMatrix[_, _, _]].nrOfPartitions == 2)
      }
    }
  }

  it should "be able to create a BigMatrix with multiple partial models per server" in withMaster { _ =>
    withServers(2) { _ =>
      withClient { client =>
        val model = client.matrix[Long](49, 7, 3)
        assert(model.isInstanceOf[BigMatrix[Long]])
        assert(model.asInstanceOf[AsyncBigMatrix[_, _, _]].nrOfPartitions == 6)
      }
    }
  }

  it should "be able to create a BigVector with one partial model per server" in withMaster { _ =>
    withServers(3) { _ =>
      withClient { client =>
        val model = client.vector[Long](4200, 1)
        assert(model.isInstanceOf[BigVector[Long]])
        assert(model.asInstanceOf[AsyncBigVector[_, _, _]].nrOfPartitions == 3)
      }
    }
  }

  it should "be able to create a BigVector with multiple partial models per server" in withMaster { _ =>
    withServers(3) { _ =>
      withClient { client =>
        val model = client.vector[Long](4200, 8)
        assert(model.isInstanceOf[BigVector[Long]])
        assert(model.asInstanceOf[AsyncBigVector[_, _, _]].nrOfPartitions == 24)
      }
    }
  }

  it should "be able to create a BigMatrix[Int]" in withMaster { _ =>
    withServer { server =>
      withClient { client =>
        val model = client.matrix[Int](49, 7)
        assert(model.isInstanceOf[BigMatrix[Int]])
      }
    }
  }

  it should "be able to create a BigMatrix[Long]" in withMaster { _ =>
    withServer { server =>
      withClient { client =>
        val model = client.matrix[Long](49, 7)
        assert(model.isInstanceOf[BigMatrix[Long]])
      }
    }
  }

  it should "be able to create a BigMatrix[Float]" in withMaster { _ =>
    withServer { server =>
      withClient { client =>
        val model = client.matrix[Float](49, 7)
        assert(model.isInstanceOf[BigMatrix[Float]])
      }
    }
  }

  it should "be able to create a BigMatrix[Double]" in withMaster { _ =>
    withServer { server =>
      withClient { client =>
        val model = client.matrix[Double](49, 7)
        assert(model.isInstanceOf[BigMatrix[Double]])
      }
    }
  }

  it should "be able to create a BigVector[Int]" in withMaster { _ =>
    withServer { server =>
      withClient { client =>
        val model = client.vector[Int](49)
        assert(model.isInstanceOf[BigVector[Int]])
      }
    }
  }

  it should "be able to create a BigVector[Long]" in withMaster { _ =>
    withServer { server =>
      withClient { client =>
        val model = client.vector[Long](490)
        assert(model.isInstanceOf[BigVector[Long]])
      }
    }
  }

  it should "be able to create a BigVector[Float]" in withMaster { _ =>
    withServer { server =>
      withClient { client =>
        val model = client.vector[Float](1)
        assert(model.isInstanceOf[BigVector[Float]])
      }
    }
  }

  it should "be able to create a BigVector[Double]" in withMaster { _ =>
    withServer { server =>
      withClient { client =>
        val model = client.vector[Double](10000)
        assert(model.isInstanceOf[BigVector[Double]])
      }
    }
  }

}
