package com.phenan.scalalr
package cli

import shared._

import java.io._

import shapeless._
import shapeless.ops.coproduct.Inject

import scala.util.parsing.combinator.JavaTokenParsers

trait SyntaxFileParserModule {
  this: CLISyntaxRuleModule with SyntaxRuleModule =>

  case class SyntaxDefinition (qualifiedName: List[String], syntax: Syntax)

  object SyntaxParsers extends JavaTokenParsers {

    def runParser (file: File): Either[String, SyntaxDefinition] = {
      val reader = new BufferedReader(new FileReader(file))
      val parseResult = parseAll(syntax, reader)
      reader.close()
      parseResult match {
        case Success(r, _)   => Right(r)
        case NoSuccess(m, _) => Left(m)
      }
    }

    def syntax: Parser[SyntaxDefinition] = "syntax" ~> rep1sep(ident, ".") ~ ("(" ~> nonTerminal <~ ")" ) ~ ("{" ~> rule.* <~ "}" ) ^^ {
      case name ~ start ~ rules => SyntaxDefinition(name, Syntax(start, rules.flatten))
    }

    def rule: Parser[List[Rule]] = branch | derivation

    def branch: Parser[List[Rule]] = ( nonTerminal <~ "=" ) ~ rep1sep(nonTerminal, "|") <~ ";" ^^ {
      case left ~ right => right.map(nt => Rule(left, List(Symbol(nt)), Branch))
    }

    def derivation: Parser[List[Rule]] = ( nonTerminal <~ "=" ) ~ choice[Symbol](terminal, nonTerminal).+ <~ ";" ^^ {
      case left ~ right => List(Rule(left, right, Derivation))
    }

    def nonTerminal: Parser[NonTerminal] = ident ^^ { id => NonTerminalImpl(id.capitalize) }

    def terminal: Parser[Terminal] = choice[Terminal](literal, keyword)

    def literal: Parser[LiteralToken] = "(" ~> ((ident <~ ":").? ~ ident) <~ ")" ^^ { case id ~ t => LiteralTokenImpl(id, t) }

    def keyword: Parser[Keyword] = stringLiteral ^^ { lit => Keyword(lit.substring(1, lit.length - 1)) }

    private def choice[R <: Coproduct]: Choice[R] = new Choice[R]

    private class Choice [R <: Coproduct] {
      def apply [T1, T2] (p1: => Parser[T1], p2: => Parser[T2]) (implicit inj1: Inject[R, T1], inj2: Inject[R, T2]): Parser[R] = {
        p1.map(inj1(_)) | p2.map(inj2(_))
      }
      def apply [T1, T2, T3] (p1: => Parser[T1], p2: => Parser[T2], p3: => Parser[T3]) (implicit inj1: Inject[R, T1], inj2: Inject[R, T2], inj3: Inject[R, T3]): Parser[R] = {
        p1.map(inj1(_)) | p2.map(inj2(_)) | p3.map(inj3(_))
      }
    }
  }
}
