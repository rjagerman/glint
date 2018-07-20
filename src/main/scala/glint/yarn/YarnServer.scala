package glint.yarn

import com.typesafe.config.ConfigFactory
import glint.Server

import scala.concurrent.ExecutionContext

/**
  * Start glint Server on Yarn
  */
object YarnServer {

  /**
    * Start glint Server on Yarn Container
    * @param args args(0) should contains master hostname or ip
    */
  def main(args: Array[String]) = {
    if (args.length < 1) {
      error("Not Specific Master Host")
    }
    val master = args(0)

    val default = ConfigFactory.parseResourcesAnySyntax("glint")
    val config = ConfigFactory
      .parseString(s"glint.master.host=$master")
      .withFallback(default)
      .resolve()

    implicit val ec = ExecutionContext.Implicits.global
    Server.run(config).onSuccess {
      case (system, ref) =>
        println(s"Start Glint Server on Node $master Successfully")
        sys.addShutdownHook {
          system.terminate()
        }
    }

    ()
  }
}

