package glint.messages

/**
 * Message send by a parameter server to the manager when it attempts to register itself
 * @param host The hostname
 * @param port The port
 * @param systemName The system name
 */
case class Register(host: String, port: Int, systemName: String)
