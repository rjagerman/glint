package glint.yarn

import java.io.File
import java.nio.ByteBuffer
import java.util.Collections

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.slf4j.StrictLogging
import glint.yarn.AppMaster.Options
import glint.yarn.AppClient.{setUpLocalResource, setupLaunchEnv}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.yarn.api.ApplicationConstants
import org.apache.hadoop.yarn.api.records._
import org.apache.hadoop.yarn.client.api.{AMRMClient, NMClient}
import org.apache.hadoop.yarn.client.api.AMRMClient.ContainerRequest
import org.apache.hadoop.yarn.conf.YarnConfiguration
import org.apache.hadoop.yarn.util.{ConverterUtils, Records}

import scala.util.Try
import scala.collection.JavaConverters._
import scala.collection.mutable

private[glint] class AppMaster(conf: Config,
                options: Options,
                hadoopConf: Option[Configuration] = None
               ) extends StrictLogging {

  private val yarnRMClient = AMRMClient.createAMRMClient().asInstanceOf[AMRMClient[ContainerRequest]]
  private val yarnConf = new YarnConfiguration(hadoopConf.getOrElse(new Configuration()))
  private val yarnNMClient = NMClient.createNMClient()

  private val maxNumContainerFailures = conf.getInt("glint.yarn.max-number-container-failures")
  private val initialAllocationInterval = conf.getInt("glint.yarn.initial-allocation-interval")

  private val memory = options.memory.getOrElse(conf.getInt("glint.yarn.container-memory"))
  private val vcores = options.cores.getOrElse(conf.getInt("glint.yarn.container-vcores"))

  private val masterClass = "glint.yarn.YarnMaster"
  private val serverClass = "glint.yarn.YarnServer"


  yarnRMClient.init(yarnConf)
  yarnRMClient.start()
  yarnRMClient.registerApplicationMaster("", 0, "")

  yarnNMClient.init(yarnConf)
  yarnNMClient.start()

  private val localResources = {
    logger.info("Prepare Local resources")
  }

  def getAttempId(): ApplicationAttemptId = {
    getContainerId().getApplicationAttemptId
  }

  def getContainerId(): ContainerId = {
    val containerIdString = System.getenv(ApplicationConstants.Environment.CONTAINER_ID.name())
    ConverterUtils.toContainerId(containerIdString)
  }

  def requestContainer(): ContainerRequest = {
    /* Init Resources */
    val priority = Records.newRecord(classOf[Priority])
    priority.setPriority(0)

    val resource = Records.newRecord(classOf[Resource])
    resource.setMemory(memory)
    resource.setVirtualCores(vcores)

    new ContainerRequest(resource, null, null, priority)
  }


  final def run(): Int = {
    import AppClient.EMPTY_MASTER_STRING
    try {
      for (i <- 1 to options.number) {
        logger.info(s"Add Container Launching Ask")
        yarnRMClient.addContainerRequest(requestContainer())
      }

      var responseId = 0
      var completedContainers = 0
      var launchedContainers = 0
      var failureContainerRequest = 0
      var masterHost: Option[String] = options.master

      while (completedContainers < options.number) {
        val appJar = Records.newRecord(classOf[LocalResource])
        setUpLocalResource(yarnConf, new Path(options.jarPath), appJar)

        val launchEnv = setupLaunchEnv(yarnConf)

        responseId += 1
        val response = yarnRMClient.allocate(responseId)
        for (container <- response.getAllocatedContainers.asScala) {
          val userClass =
            if (launchedContainers == 0 && options.master.isEmpty) {
              masterHost = Some(container.getNodeId.getHost)
              masterClass
            } else serverClass

          val __master = masterHost getOrElse EMPTY_MASTER_STRING
          val commands = List(
            "$JAVA_HOME/bin/java",
            "-Xmx" + memory + "m",
            userClass,
            __master,
            "1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout",
            "2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stderr"
          )
          val containerLaunchContext = Records.newRecord(classOf[ContainerLaunchContext])
          containerLaunchContext.setCommands(commands.asJava)
          containerLaunchContext.setLocalResources(Collections.singletonMap(AppClient.default_app_jar, appJar))
          containerLaunchContext.setEnvironment(launchEnv.asJava)

          var result = mutable.Map[String, ByteBuffer]()
          logger.info(s"Launching Container ${container}")
          try {
            result = yarnNMClient.startContainer(container, containerLaunchContext).asScala
          } catch {
            case ex: Throwable =>
              ex.printStackTrace()
          } finally {
            if (result.nonEmpty) {
              launchedContainers += 1
              logger.info(s"Launched $launchedContainers")
              if (launchedContainers == 1 && options.master.isEmpty) {
                Thread.sleep(initialAllocationInterval)
              }
            }
          }
        }

        for (status <- response.getCompletedContainersStatuses.asScala) {
          if (status.getExitStatus == 0) {
            completedContainers += 1
            logger.info(s"Completed Task with Status $status")
          } else {
            launchedContainers -= 1
            failureContainerRequest += 1
            if (failureContainerRequest > maxNumContainerFailures) {
              throw new Exception(s"Request Container and Running Task Failed $failureContainerRequest > $maxNumContainerFailures")
            }
            if (launchedContainers <= options.number * 0.5) {
              throw new Exception("Requested Containers failed more than 50%")
            }
          }
        }
      }
      0
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        stop(FinalApplicationStatus.FAILED, e.toString)
        1
    }
  }

  def stop(status: FinalApplicationStatus, message: String = ""): Unit = {
    yarnRMClient.unregisterApplicationMaster(status, message, "")
  }
}


object AppMaster extends StrictLogging {

  import AppClient.EMPTY_MASTER_STRING

  val default_container_memory: Int = 1024 * 10
  val default_container_vcores: Int = 2
  val default_container_instances: Int = 2

  def main(args: Array[String]): Unit = {
    var options = Options()
    if (args.size < 5 || args(0) == "") {
      throw new Exception("Not Enough Arguments, the parameter should be: " +
        "jar_path master_ip_or_hostname instance_number container_memory container_vcores")
    }
    options = options.copy(jarPath = args(0))

    if (args(1) != "" && args(1) != EMPTY_MASTER_STRING) {
      options = options.copy(master = Some(args(1)))
    }

    options = options.copy(number =
      Try {
        args(2) toInt
      } getOrElse (default_container_instances))

    options = options.copy(memory = Try {
      args(3) toInt
    } toOption)

    options = options.copy(cores = Try {
      args(4) toInt
    } toOption)

    val default = ConfigFactory
      .parseResourcesAnySyntax("glint")
    val config = options
      .conf.withFallback(default)
      .resolve()
    val master = new AppMaster(config, options)
    System.exit(master.run())
    ()
  }

  private[glint] case class Options(cores: Option[Int] = None,
                                    memory: Option[Int] = None,
                                    master: Option[String] = None,
                                    jarPath: String = "",
                                    number: Int = 0) {
    def conf: Config = {
      ConfigFactory
        .parseResourcesAnySyntax("glint")
        .resolve()
    }
  }

}
