package glint.exceptions

/**
  * An exception that occurs when a push fails
  *
  * @param message A specific error message detailing what failed
  */
class PushFailedException(message: String) extends Exception(message)
