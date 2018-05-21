package scala.meta.internal.semanticdb.scalac

import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import scala.collection.mutable
import scala.{meta => m}
import scala.meta.internal.io._
import scala.reflect.internal.util.{Position => GPosition, SourceFile => GSourceFile}
import scala.reflect.io.VirtualFile
import scala.reflect.io.{PlainFile => GPlainFile}

trait InputOps { self: SemanticdbOps =>

  private lazy val gSourceFileInputCache = mutable.Map[GSourceFile, m.Input]()
  implicit class XtensionGSourceFileInput(gsource: GSourceFile) {
    def toUri: String = toInput match {
      case input: m.Input.File =>
        config.sourceroot.toURI.relativize(input.path.toURI).toString
      case input: m.Input.VirtualFile =>
        input.path
      case _ =>
        ""
    }
    def toText: String = toInput match {
      case _: m.Input.File =>
        "" // slim mode, don't embed contents
      case input: m.Input.VirtualFile =>
        input.value
      case _ =>
        ""
    }
    def toInput: m.Input =
      gSourceFileInputCache.getOrElseUpdate(gsource, {
        gsource.file match {
          case gfile: GPlainFile =>
            import SemanticdbMode._
            config.mode match {
              case Slim =>
                m.Input.File(gfile.file)
              case Fat =>
                val path = m.AbsolutePath(gfile.file)
                val label = config.sourceroot.toURI.relativize(path.toURI).toString
                // NOTE: Can't use gsource.content because it's preprocessed by scalac.
                val contents = FileIO.slurp(path, UTF_8)
                m.Input.VirtualFile(label, contents)
              case Disabled =>
                m.Input.None
            }
          case gfile: VirtualFile =>
            val uri = URLEncoder.encode(gfile.path, UTF_8.name)
            m.Input.VirtualFile(uri, gsource.content.mkString)
          case _ =>
            m.Input.None
        }
      })
  }

  implicit class XtensionGPositionMPosition(pos: GPosition) {
    def toMeta: m.Position = {
      // NOTE: Even with -Yrangepos enabled we cannot be guaranteed that all positions are
      // range positions. In the case we encounter a non-range position we assume start == end.
      val input = pos.source.toInput
      if (input == m.Input.None) m.Position.None
      else if (!pos.isDefined) m.Position.None
      else if (pos.isRange) m.Position.Range(input, pos.start, pos.end)
      else m.Position.Offset(input, pos.point)
    }
  }
}
