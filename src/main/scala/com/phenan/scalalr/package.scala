package com.phenan

import com.phenan.scalalr.internal._

import scala.language.implicitConversions

package object scalalr {

  implicit class LiteralOps [T] (t: T) {
    def apply [U] (value: TokenListCons[Literal[U], TokenListSentinel]): TokenListCons[Literal[U], TokenListCons[Literal[T], TokenListSentinel]] = TokenListCons(value.head, singleToken(Literal(t)))
  }

  implicit class TokenListOps [T <: TokenList] (t: T) {
    def apply [U] (value: TokenListCons[Literal[U], TokenListSentinel]): TokenListCons[Literal[U], T] = TokenListCons(value.head, t)
  }

  def singleToken [T] (t: T): TokenListCons[T, TokenListSentinel] = TokenListCons(t, TokenListSentinel)

  implicit def shift_transition [T, N1, N2] (implicit shift: Shift[T, N1, N2]): Transition[T, N1, N2] = Transition ({ (n: N1, t: T) => shift.shift(n, t) })
  implicit def reduce_transition [T, N1, N2, N3] (implicit reduce: Reduce[T, N1, N2], transition: Transition[T, N2, N3]): Transition[T, N1, N3] = Transition { (state, terminal) => transition.transit(reduce.reduce(state), terminal) }
  implicit def accept_transition [NX, R] (implicit accept: Accept[NX, R]): Transition[EoI.type, NX, R] = Transition { (n, _) => accept.accept(n) }
  implicit def accept_ast [NX, R] (node: NX)(implicit transition: Transition[EoI.type, NX, R]): R = transition.transit(node, EoI)
  implicit def simple_transition [T, N1, N2] (implicit transition: Transition[T, N1, N2]): Transitions[TokenListCons[T, TokenListSentinel], N1, N2] = Transitions((n, h) => transition.transit(n, h.head))
  implicit def composed_transitions [T, L <: TokenList, N1, N2, N3] (implicit transitions: Transitions[L, N1, N2], transition: Transition[T, N2, N3]): Transitions[TokenListCons[T, L], N1, N3] = Transitions((n, h) => transition.transit(transitions.transit(n, h.tail), h.head))

  implicit def literal [T] (value: T): TokenListCons[Literal[T], TokenListSentinel] = singleToken(Literal(value))

  implicit def acceptLiteral [T, N, R] (token: T) (implicit transition1: Transition[Literal[T], StartNode.type, N], transition2: Transition[EoI.type, N, R]): R = transition2.transit(transition1.transit(StartNode, Literal(token)), EoI)
  implicit def acceptTokenList [L <: TokenList, N, R] (tokens: L) (implicit transitions: Transitions[L, StartNode.type, N], transition: Transition[EoI.type, N, R]): R = transition.transit(transitions.transit(StartNode, tokens), EoI)

  implicit class LiteralTransition [N1, N2] (node: N1) {
    def literal [T](value: T)(implicit transition: Transition[Literal[T], N1, N2]): N2 = transition.transit(node, Literal[T](value))
  }

  def $$semicolon : TokenListCons[EoI.type, TokenListSentinel] = singleToken(EoI)

  val scaLALRVersion: String = "2.3.3"
}
