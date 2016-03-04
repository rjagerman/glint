package glint.exceptions

/**
  * An exception that occurs when a pull fails
  *
  * @param message A specific error message detailing what failed
  */
class PullFailedException(message: String) extends Exception(message)
