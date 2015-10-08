package glint

import java.io.File

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

/**
 * Main application
 */
object Glint extends StrictLogging {

  /**
   * Command-line options
   *
   * @param mode The mode of operation (either "manager" or "server")
   * @param config The configuration file to load (defaults to the included default.conf)
   * @param host The host of the parameter server (only when mode of operation is "server")
   * @param port The port of the parameter server (only when mode of operation is "server")
   */
  case class Options(mode: String = "",
                     config: File = new File(getClass.getClassLoader.getResource("default.conf").getFile),
                     host: String = "localhost",
                     port: Int = 0)

  /**
   * Main entry point of the application
   *
   * @param args
   */
  def main(args: Array[String]): Unit = {

    val parser = new scopt.OptionParser[Options]("glint") {
      head("glint", "0.1")
      opt[File]('c', "config") valueName("<file>") action { (x, c) =>
        c.copy(config = x) } text("The .conf file for glint")
      cmd("master") action { (_, c) =>
        c.copy(mode = "master") } text("Starts a master node.")
      cmd("server") action { (_, c) =>
        c.copy(mode = "server") } text("Starts a server node.") children(
          opt[String]('h', "host") required() valueName("<host>") action { (x, c) =>
            c.copy(host = x) } text("The hostname of the server"),
          opt[Int]('p', "port") valueName("<port>") action { (x, c) =>
            c.copy(port = x) } text("The port of the server")
        )
    }

    parser.parse(args, Options()) match {
      case Some(options) =>

        // Read configuration
        logger.debug("Parsing configuration file")
        val config = ConfigFactory.parseFile(options.config)

        // Start specified mode of operation
        options.mode match {
          case "server" => Server.run(config, options.host, options.port)
          case "master" => Master.run(config)
          case _ =>
            parser.showUsageAsError
            System.exit(1)
      }
      case None => System.exit(1)
    }
  }

}
