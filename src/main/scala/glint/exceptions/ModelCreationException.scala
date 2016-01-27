package glint.exceptions

/**
  * An exception that occurs when model creation fails
  *
  * @param message A specific error message detailing what failed
  */
class ModelCreationException(message: String) extends Exception(message)
