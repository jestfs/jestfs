package jestfs.spec

import jestfs.lang.*
import jestfs.lang.util.*
import jestfs.spec.util.*
import org.jsoup.nodes.Element

/** abstract algorithms in ECMA-262 */
case class Algorithm(
  head: Head,
  elem: Element,
  body: Step,
  code: String,
) extends SpecElem {

  /** check whether it is incomplete */
  lazy val complete: Boolean = incompleteSteps.isEmpty

  /** algorithm name */
  lazy val name: String = head.fname

  /** normalized algorithm name */
  lazy val normalizedName: String = name.replace("/", "").replace("`", "")

  /** return types */
  lazy val retTy: Type = head.retTy

  /** get all steps */
  lazy val steps: List[Step] = StepCollector(body)

  /** get incomplete algorithm steps */
  lazy val incompleteSteps: List[Step] =
    steps.filter(_.isInstanceOf[YetStep])

  /** get complete algorithm steps */
  lazy val completeSteps: List[Step] =
    steps.filter(!_.isInstanceOf[YetStep])
}
