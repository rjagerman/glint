package glint.yarn

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.slf4j.StrictLogging
import glint.yarn.AppClient.Options
import glint.yarn.AppClient.{setUpLocalResource, setupLaunchEnv}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.yarn.api.ApplicationConstants
import org.apache.hadoop.yarn.api.ApplicationConstants.Environment
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponse
import org.apache.hadoop.yarn.api.records.{ContainerLaunchContext, _}
import org.apache.hadoop.yarn.client.api.{YarnClient, YarnClientApplication}
import org.apache.hadoop.yarn.conf.YarnConfiguration
import org.apache.hadoop.yarn.exceptions.ApplicationNotFoundException
import org.apache.hadoop.yarn.util.{Apps, ConverterUtils, Records}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.control.NonFatal


/**
  * glint running on yarn needs yarn client to start, and this file
  * is the client file to submit application to yarn as application.
  *
  * hadoop -jar /path/to/compiled/Glint.jar -c /path/to/glint.conf -n Server Numbers <-m master>
  */
private[glint] class AppClient(conf: Config,
                               options: Options,
                               hadoopConf: Option[Configuration] = None) extends StrictLogging {

  private val yarnClient = YarnClient.createYarnClient()
  private val yarnConf = new YarnConfiguration(hadoopConf.getOrElse(new Configuration()))

  // AM Settings
  private val amMemory = conf.getLong("glint.yarn.am-memory")
  private val amMemoryOverhead = conf.getLong("glint.yarn.am-memory-overhead")
  private val amVcores = conf.getInt("glint.yarn.am-vcores")

  // Container Settings
  private val containerMemory = conf.getLong("glint.yarn.container-memory")
  private val containerMemoryOverhead = conf.getLong("glint.yarn.container-memory-overhead")
  private val containerVcores = conf.getInt("glint.yarn.container-vcores")

  private val minimumContainers = conf.getInt("glint.yarn.minimum-server-number")
  private val numberOfContainers = Math.max(minimumContainers, options.number)

  private val userClass = "glint.yarn.AppMaster"

  private val interval = conf.getInt("glint.yarn.monitor-interval")

  private var appId: ApplicationId = null

  def stop(): Unit = {
    yarnClient.stop()
  }

  def run(): Unit = {
    this.appId = submitApplication()
    val (yarnApplicationState, finalApplicationStatus) = monitorApplication(appId)
    if (yarnApplicationState == YarnApplicationState.FAILED ||
      finalApplicationStatus == FinalApplicationStatus.FAILED ||
      yarnApplicationState == YarnApplicationState.KILLED ||
      finalApplicationStatus == FinalApplicationStatus.KILLED) {
      throw new Exception(s"Application $appId with state: $yarnApplicationState final status: $finalApplicationStatus")
    }
    logger.info(s"Application $appId with final status $finalApplicationStatus")
    ()
  }

  /**
    *
    * @param appId
    * @return
    */
  private def monitorApplication(appId: ApplicationId): (YarnApplicationState, FinalApplicationStatus) = {
    var lastState: YarnApplicationState = null
    while (true) {
      Thread.sleep(interval)
      val report: ApplicationReport =
        try {
          getApplicationReport(appId)
        } catch {
          case e: ApplicationNotFoundException =>
            logger.error(s"Aplication $appId not found")
            cleanUpResources(appId)
            return (YarnApplicationState.KILLED, FinalApplicationStatus.KILLED)
          case NonFatal(e) =>
            logger.error(s"Failed to contact YARN for Application $appId", e)
            return (YarnApplicationState.FAILED, FinalApplicationStatus.FAILED)
        }
      val state = report.getYarnApplicationState

      logger.info(s"Application report for $appId (state: $state)")
      logger.info(formatReportDetails(report))

      if (lastState != state) {
        lastState = state
      }

      if (state == YarnApplicationState.FINISHED ||
        state == YarnApplicationState.FAILED ||
        state == YarnApplicationState.KILLED) {
        cleanUpResources(appId)
        return (state, report.getFinalApplicationStatus)
      }
    }

    cleanUpResources(appId)

    throw new Exception("Monitoring While true loop is depleted! What Fuck ...")
  }


  private def formatReportDetails(report: ApplicationReport): String = {
    val details = Seq[(String, String)](
      ("client token", getClientToken(report)),
      ("diagnostics", report.getDiagnostics),
      ("ApplicationMaster host", report.getHost),
      ("ApplicationMaster RPC port", report.getRpcPort.toString),
      ("queue", report.getQueue),
      ("start time", report.getStartTime.toString),
      ("final status", report.getFinalApplicationStatus.toString),
      ("tracking URL", report.getTrackingUrl),
      ("user", report.getUser)
    )

    // Use more loggable format if value is null or empty
    details.map { case (k, v) =>
      val newValue = Option(v).filter(_.nonEmpty).getOrElse("N/A")
      s"\n\t $k: $newValue"
    }.mkString("")
  }

  /**
    * Return the security token used by this client to communicate with the ApplicationMaster.
    * If no security is enabled, the token returned by the report is null.
    */
  private def getClientToken(report: ApplicationReport): String = {
    Option(report.getClientToAMToken).map(_.toString).getOrElse("")
  }

  /** Get application report from ResourceManager through applicationId */
  def getApplicationReport(appId: ApplicationId): ApplicationReport = {
    yarnClient.getApplicationReport(appId)
  }


  private def submitApplication(): ApplicationId = {
    var appId: ApplicationId = null
    try {
      yarnClient.init(yarnConf)
      yarnClient.start()

      logger.info("Requesting a new application form cluster with %d NodeManagers"
        .format(yarnClient.getYarnClusterMetrics.getNumNodeManagers))

      val newApp = yarnClient.createApplication()
      val newAppResponse = newApp.getNewApplicationResponse()
      appId = newAppResponse.getApplicationId()

      // Verify cluster Resources for application
      verifyClusterResources(newAppResponse)

      // Set up Container launch environment
      val containerContext = createContainerLaunchContext(newAppResponse)
      val appContext = createApplicationSubmissionContext(newApp, containerContext)

      logger.info(s"Submitting application $appId to ResourceManager")
      yarnClient.submitApplication(appContext)
      appId
    } catch {
      case e: Throwable =>
        if (appId != null) {
          cleanUpResources(appId)
        }
        throw e
    }
  }

  /**
    * Fail fast if we have requested more resources per container than is available in the cluster.
    */
  private def verifyClusterResources(newAppResponse: GetNewApplicationResponse): Unit = {
    val maxMem = newAppResponse.getMaximumResourceCapability().getMemory()
    logger.info("Verifying our application has not requested more than the maximum " +
      s"memory capability of the cluster ($maxMem MB per container)")
    val containerMem = containerMemory + containerMemoryOverhead
    if (containerMem > maxMem) {
      throw new IllegalArgumentException(s"Required executor memory ($containerMemory" +
        s"+$containerMemoryOverhead MB) is above the max threshold ($maxMem MB) of this cluster! " +
        "Please check the values of 'yarn.scheduler.maximum-allocation-mb' and/or " +
        "'yarn.nodemanager.resource.memory-mb'.")
    }
    val amMem = amMemory + amMemoryOverhead
    if (amMem > maxMem) {
      throw new IllegalArgumentException(s"Required AM memory ($amMemory" +
        s"+$amMemoryOverhead MB) is above the max threshold ($maxMem MB) of this cluster! " +
        "Please increase the value of 'yarn.scheduler.maximum-allocation-mb'.")
    }
    logger.info("Will allocate AM container, with %d MB memory including %d MB overhead".format(
      amMem,
      amMemoryOverhead))

    // We could add checks to make sure the entire cluster has enough resources but that involves
    // getting all the node reports and computing ourselves.
  }

  /** TODO Automatic Upload jar file to HDFS, and clear this jar **/
  // TODO
  def cleanUpResources(appId: ApplicationId): Unit = {
    ()
  }

  /** Set up a ContainerLaunch Context to launch our ApplicationMaster container */
  def createContainerLaunchContext(appResponse: GetNewApplicationResponse): ContainerLaunchContext = {
    logger.info("Settting up container launch context for our AM")
    val appId = appResponse.getApplicationId

    val launchEnv = setupLaunchEnv(yarnConf)
    val localResources = prepareLocalResources()

    val amContainer = Records.newRecord(classOf[ContainerLaunchContext])
    amContainer.setLocalResources(localResources.asJava)
    amContainer.setEnvironment(launchEnv.asJava)

    import AppClient.EMPTY_MASTER_STRING

    val commands = List(
      "$JAVA_HOME/bin/java",
      "-Xmx" + amMemory + "m",
      userClass,
      options.jarPath,
      options.master.getOrElse(EMPTY_MASTER_STRING),
      numberOfContainers,
      containerMemory + containerMemoryOverhead,
      containerVcores,
      " 1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout",
      " 2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stderr"
    )

    val printableCommands = commands.map(s => if (s == null) "null" else s.toString)
    amContainer.setCommands(printableCommands.asJava)

    logger.info("=====================================================")
    logger.info("YARN AM LAUNCH CONTEXT:")
    logger.info("\tenv:")
    launchEnv foreach { case (k, v) => logger.info(s"\t\t$k -> $v")}
    logger.info("\tresource:")
    localResources foreach { case (k, v) => logger.info(s"\t\t$k -> $v")}
    logger.info("\tcommand: ")
    logger.info(s"\t\t${commands.mkString(" ")}")
    logger.info("=====================================================")

    amContainer
  }

  /** Prepare Local resources For AM Container **/
  private def prepareLocalResources(): mutable.HashMap[String, LocalResource] = {
    logger.info("Preparing resources for AM container")
    val resources = new mutable.HashMap[String, LocalResource]()

    //add the jar which contains the Application master code to classpath
    val appMasterJar = Records.newRecord(classOf[LocalResource])
    setUpLocalResource(yarnConf, new Path(options.jarPath), appMasterJar)

    resources(AppClient.default_app_jar) = appMasterJar
    resources
  }



  def createApplicationSubmissionContext(app: YarnClientApplication, containerContext: ContainerLaunchContext)
  : ApplicationSubmissionContext = {
    val appContext = app.getApplicationSubmissionContext
    appContext.setApplicationName(AppClient.default_app_name)
    appContext.setQueue("default")
    appContext.setAMContainerSpec(containerContext)
    appContext.setApplicationType("Yarn-Application")

    val capability = Records.newRecord(classOf[Resource])
    capability.setMemory((amMemory + amMemoryOverhead).toInt)
    capability.setVirtualCores(amVcores)

    appContext.setResource(capability)

    appContext
  }
}

object AppClient extends StrictLogging {
  val default_app_name: String = "glint-on-yarn"
  val default_app_jar: String = default_app_name + s"__app__.jar"
  val EMPTY_MASTER_STRING: String = "EMPTY"

  def setUpLocalResource(yarnConf: Configuration, resourcePath: Path, resource: LocalResource) = {
    val jarStat = FileSystem.get(yarnConf).getFileStatus(resourcePath)
    resource.setResource(ConverterUtils.getYarnUrlFromPath(resourcePath))
    resource.setSize(jarStat.getLen())
    resource.setTimestamp(jarStat.getModificationTime())
    resource.setType(LocalResourceType.FILE)
    resource.setVisibility(LocalResourceVisibility.APPLICATION)
  }

  /** Setting AM Container Launcher Environment **/
  def setupLaunchEnv(yarnConf: YarnConfiguration): collection.mutable.Map[String, String] = {
    def setUpEnv(env: collection.mutable.Map[String, String], conf: YarnConfiguration) = {
      val classPath = conf.getStrings(YarnConfiguration.YARN_APPLICATION_CLASSPATH, YarnConfiguration.DEFAULT_YARN_APPLICATION_CLASSPATH: _*)
      for (c <- classPath) {
        Apps.addToEnvironment(env.asJava, Environment.CLASSPATH.name(), c.trim())
      }

      Apps.addToEnvironment(env.asJava,
        Environment.CLASSPATH.name(),
        Environment.PWD.$() + File.separator + "*")
    }

    val env = collection.mutable.Map[String, String]()
    setUpEnv(env, yarnConf)
    env
  }

  def main(args: Array[String]): Unit = {

    val parser = new scopt.OptionParser[Options]("glint-yarn-client") {
      head("glint-yarn-client", "0.1")
      opt[File]('c', "config") valueName "<file>" action { (x, c) =>
        c.copy(config = x)
      } optional() text "The .conf file for glint"
      /** Not Used Yet
      opt[String]('m', "master") valueName "<master>" action { (x, c) =>
        c.copy(master = Some(x))
      } text "The glint master hostname or ip"
      opt[Int]('c', "vcores") valueName "<vcores>" action { (x, c) =>
        c.copy(cores = Some(x))
      } text "Yarn Container Vcores"
      opt[Int]('v', "memory") valueName "<memory>" action { (x, c) =>
        c.copy(memory = Some(x))
      } text "Yarn Container Memory" **/
      opt[String]("path") valueName "Jar Path for Glint" action { (x, c) =>
        c.copy(jarPath = x)
      } text "Glint Jar Path on HDFS"
      opt[Int]('n', "containers") valueName "Number Of Container Instance" action { (x, c) =>
        c.copy(number = x)
      } text "Number Of Glint Master and Server Instances"
    }

    parser.parse(args, Options()) match {
      case Some(options) =>
//        logger.debug("Parsing conf file")
        val default = ConfigFactory
          .parseResourcesAnySyntax("glint")
        val config = options
          .conf.withFallback(default)
          .resolve()
        new AppClient(config, options).run()
      case None =>
//        logger.error("Parse Argument Error")
        parser.showUsage
        System.exit(1)
    }
  }

  private[glint] case class Options(config: File = new File(getClass.getClassLoader.getResource("glint.conf").getFile),
                                    cores: Option[Int] = None,
                                    memory: Option[Int] = None,
                                    master: Option[String] = None,
                                    jarPath: String = "",
                                    number: Int = 0) {
    def conf: Config = {
      val default = ConfigFactory
        .parseResourcesAnySyntax("glint")
      ConfigFactory
        .parseFile(config)
        .withFallback(default)
        .resolve()
    }
  }

}

