package glint.messages.master

/**
  * Send to master when trying to obtain a model by name
  */
case class GetModel(name: String)
