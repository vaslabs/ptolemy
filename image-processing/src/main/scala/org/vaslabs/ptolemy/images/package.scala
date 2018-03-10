package org.vaslabs.ptolemy

import scala.collection.mutable.ListBuffer

package object images {

  object eitherUtils {
    implicit final class EitherConcat[A, B](val listOfEithers: List[Either[A, B]]) extends AnyVal {
      def concat: Either[A, List[B]] = {
        val correctResults = listOfEithers.takeWhile(_.isRight).map(_.right.get)
        if (correctResults.size == listOfEithers.size)
          Right(correctResults)
        else {
          Left(listOfEithers.find(_.isLeft).get.left.get)
        }
      }
    }
  }

}
