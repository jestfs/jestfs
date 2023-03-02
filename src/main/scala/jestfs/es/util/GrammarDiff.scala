package jestfs.es.util

import jestfs.spec.{ButNot, Nonterminal, Production, Rhs, Symbol}
import jestfs.util.BaseUtils

/** making use of the grammar difference among EcmaScript versions. */
object GrammarDiff {

  def chooseRhs(prod: Production, pairs: Iterable[(Rhs, Int)]): (Rhs, Int) = {
    val rhsZipWithWeight = pairs.map {
      case (rhs, idx) =>
        ((rhs, idx), scala.math.pow(2, getScore(prod, idx)).toInt)
    }
    BaseUtils.weightedChoose(rhsZipWithWeight)
  }

  // ---------------------------------------------------------------------------
  // private helpers
  // ---------------------------------------------------------------------------

  // The newer the grammar element, the higher the score.
  private def getScore(prod: Production, rhsIdx: Int): Int =
    math.max(getAddScore(prod, rhsIdx), getExtScore(prod, rhsIdx))

  private def getAddScore(prod: Production, rhsIdx: Int): Int = {
    prod
      .rhsVec(rhsIdx)
      .symbols
      .map {
        case Nonterminal(name, _, _)            => getAddScore(name)
        case ButNot(Nonterminal(name, _, _), _) => getAddScore(name)
        case _                                  => 0
      }
      .max
  }

  private def getAddScore(name: String): Int = {
    addProds.zipWithIndex.foldLeft(0) {
      case (score, (set, idx)) =>
        if set.contains(name) then idx + 1 else score
    }
  }

  private def getExtScore(prod: Production, rhsIdx: Int): Int = {
    extProds.zipWithIndex.foldLeft(0) {
      case (score, (map, idx)) =>
        if map.getOrElse(prod.name, Set.empty).contains(rhsIdx) then idx + 1
        else score
    }
  }

  private lazy val addProds: List[Set[String]] =
    List(add15, add16, add17, add18, add19, add20, add21, add22)

  private lazy val extProds: List[Map[String, Set[Int]]] =
    List(ext15, ext16, ext17, ext18, ext19, ext20, ext21, ext22)

  // productions newly added in es2015
  private val add15: Set[String] = Set(
    "GeneratorExpression",
    "GeneratorMethod",
    "ForInOfStatement",
    "LexicalDeclaration",
    "SuperCall",
    "SuperProperty",
    "SpreadElement",
    "BindingPattern",
    "AssignmentPattern",
    "ComputedPropertyName",
    "ArrowFunction",
    "YieldExpression",
    "TemplateLiteral",
    "ClassDeclaration",
    "ClassExpression",
    "MetaProperty",
  )

  // productions extended in es2015
  private val ext15: Map[String, Set[Int]] = Map(
    "CoverParenthesizedExpressionAndArrowParameterSet" -> Set(3, 4, 5, 6),
    "ArgumentSet" -> Set(1, 3),
    "PropertyDefinition" -> Set(0, 1, 3),
    "MethodDefinition" -> Set(0, 4, 5),
  )

  // productions newly added in es2016
  private val add16: Set[String] = Set(
    "ExponentiationExpression",
  )

  // productions extended in es2016
  private val ext16: Map[String, Set[Int]] = Map(
    "AssignmentOperator" -> Set(11),
  )

  // productions newly added in es2017
  private val add17: Set[String] = Set(
    "AsyncFunctionExpression",
    "AsyncGeneratorExpression",
    "AsyncFunctionDeclaration",
    "AsyncGeneratorDeclaration",
    "AsyncArrowFunction",
    "AsyncMethod",
    "AsyncGeneratorMethod",
    "AwaitExpression",
  )

  // productions extended in es2017
  private val ext17: Map[String, Set[Int]] = Map.empty

  // productions newly added in es2018
  private val add18: Set[String] = Set.empty

  // productions extended in es2018
  private val ext18: Map[String, Set[Int]] = Map(
    "ForInOfStatement" -> Set(6, 7, 8),
    "PropertyDefinition" -> Set(4),
    "ObjectAssignmentPattern" -> Set(1, 3),
  )

  // productions newly added in es2019
  private val add19: Set[String] = Set.empty

  // productions extended in es2019
  private val ext19: Map[String, Set[Int]] = Map(
    "Catch" -> Set(1),
  )

  // productions newly added in es2020
  private val add20: Set[String] = Set(
    "DecimalBigIntegerLiteral",
    "BigIntLiteralSuffix",
    "OptionalExpression",
    "CoalesceExpression",
  )

  // productions extended in es2020
  private val ext20: Map[String, Set[Int]] = Map.empty

  // productions newly added in es2021
  private val add21: Set[String] = Set.empty

  // productions extended in es2021
  private val ext21: Map[String, Set[Int]] = Map(
    "AssignmentExpression" -> Set(6, 7, 8),
  )

  // productions newly added in es2022
  private val add22: Set[String] = Set(
    "FieldDefinition",
    "ClassStaticBlock",
    "PrivateIdentifier",
  )

  // productions extended in es2022
  private val ext22: Map[String, Set[Int]] = Map.empty
}
