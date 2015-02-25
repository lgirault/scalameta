package scala.meta
package syntactic

import org.scalameta.adt._
import org.scalameta.convert._
import scala.collection.mutable
import scala.meta.internal.tokenizers._

@root trait Input {
  def content: Array[Char]
  private val tokenCache = mutable.Map[Dialect, Vector[Token]]()
  def tokens(implicit dialect: Dialect): Vector[Token] = tokenCache.getOrElseUpdate(dialect, tokenize(this))
}

object Input {
  @leaf object None extends Input {
    lazy val content = new Array[Char](0)
  }
  @leaf class String(s: scala.Predef.String) extends Input {
    lazy val content = s.toArray
  }
  @leaf class File(f: java.io.File) extends Input {
    lazy val content = scala.io.Source.fromFile(f).mkString.toArray
  }
  object File {
    def apply(path: Predef.String): Input.File = Input.File(new java.io.File(path))
  }
  implicit val stringToOrigin: Convert[scala.Predef.String, Input] = Convert.apply(Input.String(_))
  implicit val fileToOrigin: Convert[java.io.File, Input] = Convert.apply(Input.File(_))
}
