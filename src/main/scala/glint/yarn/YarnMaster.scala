package glint.yarn

import com.typesafe.config.ConfigFactory
import glint.Master

import scala.concurrent.ExecutionContext

/**
  * Start glint Master on Yarn
  */

object YarnMaster {

  /**
    * Start glint Master on Yarn Container
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

    println(s"Start Glint Master on Yarn in node $master")

    implicit val ec = ExecutionContext.Implicits.global
    Master.run(config).onSuccess {
      case (system, ref) =>
        println(s"Start Glint Master on Node $master Successfully")
        sys.addShutdownHook {
          system.terminate()
        }
    }

    ()
  }
}
