package scala.meta
package syntactic

import org.scalameta.adt // NOTE: no underscore import!
import org.scalameta.tokens._
import org.scalameta.default._
import org.scalameta.default.Param._
import scala.reflect.ClassTag
import scala.language.experimental.macros

@root trait Token {
  def prototype: Token.Prototype
  def input: Input
  def dialect: Dialect
  def index: Int
  def prev: Token = if (index > 0) input.tokens(dialect).apply(index - 1) else this
  def nontrivialPrev: Token = if (prev.isInstanceOf[Token.Whitespace] || prev.isInstanceOf[Token.Comment]) prev.prev else prev
  def next: Token = if (index < input.tokens(dialect).length) input.tokens(dialect).apply(index + 1) else this
  def nontrivialNext: Token = if (next.isInstanceOf[Token.Whitespace] || next.isInstanceOf[Token.Comment]) next.next else next
  def start: Int
  def end: Int

  type ThisType <: Token
  def adjust(input: Input = this.input, dialect: Dialect = this.dialect, index: Param[Int] = Default, start: Param[Int] = Default, end: Param[Int] = Default, delta: Param[Int] = Default): ThisType

  def name: String
  def code: String = new String(input.content.slice(start, end + 1))
  final override def toString = this.show[Raw]
}

object Token {
  @branch trait Static extends Token
  @branch trait Dynamic extends Token

  @token class Ident(start: Int, end: Int) extends Dynamic { def name = "identifier" }

  object Interpolation {
    @token class Id(start: Int, end: Int) extends Dynamic { def name = "interpolation id" }
    @token class Start(start: Int, end: Int) extends Dynamic { def name = "interpolation start" }
    @token class Part(start: Int, end: Int) extends Dynamic { def name = "interpolation part" }
    @token class SpliceStart(start: Int) extends Static { def name = "splice start"; override def code = "$" }
    @token class SpliceEnd(start: Int) extends Static { def name = "splice end"; override def code = "" }
    @token class End(start: Int, end: Int) extends Dynamic { def name = "interpolation end" }
  }

  @branch trait Literal extends Token
  object Literal {
    @token class Int(start: scala.Int, end: scala.Int, value: Boolean => scala.Int) extends Literal with Dynamic { def name = "integer literal" }
    @token class Long(start: scala.Int, end: scala.Int, value: Boolean => scala.Long) extends Literal with Dynamic { def name = "long literal" }
    @token class Float(start: scala.Int, end: scala.Int, value: Boolean => scala.Float) extends Literal with Dynamic { def name = "float literal" }
    @token class Double(start: scala.Int, end: scala.Int, value: Boolean => scala.Double) extends Literal with Dynamic { def name = "double literal" }
    @token class Char(start: scala.Int, end: scala.Int, value: scala.Char) extends Literal with Dynamic { def name = "character literal" }
    @token class Symbol(start: scala.Int, end: scala.Int, value: scala.Symbol) extends Literal with Dynamic { def name = "symbol literal" }
    @token class String(start: scala.Int, end: scala.Int, value: Predef.String) extends Literal with Dynamic { def name = "string literal" }
    @token class `null`(start: scala.Int) extends Keyword with Literal with Static
    @token class `true`(start: scala.Int) extends Keyword with Literal with Static
    @token class `false`(start: scala.Int) extends Keyword with Literal with Static
  }

  @branch trait Keyword extends Static
  @token class `case`(start: Int) extends Keyword
  @token class `catch`(start: Int) extends Keyword
  @token class `class `(start: Int) extends Keyword
  @token class `def`(start: Int) extends Keyword
  @token class `do`(start: Int) extends Keyword
  @token class `else`(start: Int) extends Keyword
  @token class `extends`(start: Int) extends Keyword
  @token class `finally`(start: Int) extends Keyword
  @token class `for`(start: Int) extends Keyword
  @token class `forSome`(start: Int) extends Keyword
  @token class `if`(start: Int) extends Keyword
  @token class `import`(start: Int) extends Keyword
  @token class `match`(start: Int) extends Keyword
  @token class `macro`(start: Int) extends Keyword
  @token class `new`(start: Int) extends Keyword
  @token class `object`(start: Int) extends Keyword
  @token class `package `(start: Int) extends Keyword
  @token class `return`(start: Int) extends Keyword
  @token class `super`(start: Int) extends Keyword
  @token class `this`(start: Int) extends Keyword
  @token class `throw`(start: Int) extends Keyword
  @token class `trait`(start: Int) extends Keyword
  @token class `try`(start: Int) extends Keyword
  @token class `type`(start: Int) extends Keyword
  @token class `val`(start: Int) extends Keyword
  @token class `var`(start: Int) extends Keyword
  @token class `while`(start: Int) extends Keyword
  @token class `with`(start: Int) extends Keyword
  @token class `yield`(start: Int) extends Keyword

  @branch trait Modifier extends Keyword
  @token class `abstract`(start: Int) extends Modifier
  @token class `final`(start: Int) extends Modifier
  @token class `sealed`(start: Int) extends Modifier
  @token class `implicit`(start: Int) extends Modifier
  @token class `lazy`(start: Int) extends Modifier
  @token class `private`(start: Int) extends Modifier
  @token class `protected`(start: Int) extends Modifier
  @token class `override`(start: Int) extends Modifier

  @branch trait Delim extends Token
  @token class `(`(start: Int) extends Delim with Static
  @token class `)`(start: Int) extends Delim with Static
  @token class `[`(start: Int) extends Delim with Static
  @token class `]`(start: Int) extends Delim with Static
  @token class `{`(start: Int) extends Delim with Static
  @token class `}`(start: Int) extends Delim with Static
  @token class `,`(start: Int) extends Delim with Static
  @token class `;`(start: Int) extends Delim with Static
  @token class `:`(start: Int) extends Delim with Static
  @token class `.`(start: Int) extends Delim with Static
  @token class `=`(start: Int) extends Delim with Static
  @token class `@`(start: Int) extends Delim with Static
  @token class `#`(start: Int) extends Delim with Static
  @token class `_ `(start: Int) extends Delim with Static
  @token class `=>`(start: Int, end: Int) extends Delim with Dynamic { def name = "right arrow" }
  @token class `<-`(start: Int, end: Int) extends Delim with Dynamic { def name = "left arrow" }
  @token class `<:`(start: Int) extends Delim with Static
  @token class `>:`(start: Int) extends Delim with Static
  @token class `<%`(start: Int) extends Delim with Static

  @branch trait Whitespace extends Static
  @token class ` `(start: Int) extends Whitespace
  @token class `\t`(start: Int) extends Whitespace
  @token class `\r`(start: Int) extends Whitespace
  @token class `\n`(start: Int) extends Whitespace
  // TODO: \n\n is a virtual token emitted by TokIterator to appease the semi-ported scalac parser
  // it will never occur in a token stream produced by XtensionInputLike.tokens
  @token class `\n\n`(start: Int) extends Whitespace
  @token class `\f`(start: Int) extends Whitespace

  @token class Comment(start: Int, end: Int) extends Dynamic { def name = "comment" }

  // NOTE: in order to maintain conceptual compatibility with scala.reflect's implementation,
  // Ellipsis.rank = 1 means .., Ellipsis.rank = 2 means ..., etc
  @token class Ellipsis(start: Int, end: Int, rank: Int) extends Dynamic { def name = "ellipsis" }

  // TODO: after we bootstrap, Unquote.tree will become scala.meta.Tree
  // however, for now, we will keep it at Any in order to also support scala.reflect trees
  @token class Unquote(start: Int, end: Int, tree: Any) extends Dynamic { def name = "unquote" }

  @token class BOF() extends Static {
    def name = "beginning of file"
    override def code = ""
    def start = 0
    def end = -1
  }

  @token class EOF() extends Static {
    def name = "end of file"
    override def code = ""
    def start = input.content.length
    def end = input.content.length - 1
  }

  // TODO: implement XML literals
  @token class XMLStart(start: Int, end: Int) extends Dynamic { def name = ??? }

  @adt.root trait Prototype { def input: Input }
  object Prototype {
    @adt.leaf object None extends Prototype { def input = Input.None }
    @adt.leaf class Some(token: Token) extends Prototype { def input = token.input }
  }
}
