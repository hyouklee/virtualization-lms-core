package scala.virtualization.lms
package common

import java.io.PrintWriter
import scala.virtualization.lms.internal.GenericNestedCodegen
import scala.reflect.SourceContext

trait StaticDataExp extends EffectExp {
  case class StaticData[T](x: T) extends Def[T]
  def staticData[T:Manifest](x: T): Exp[T] = StaticData(x)

  override def isWritableSym[A](w: Sym[A]): Boolean = findDefinition(w) match {
    case Some(TP(_, StaticData(_))) => true
    case _ => super.isWritableSym(w)
  }
  
  override def mirror[A:Manifest](e: Def[A], f: Transformer)(implicit pos: SourceContext): Exp[A] = (e match {
    case StaticData(x) => staticData(x)(mtype(manifest[A]))
    case _ => super.mirror(e,f)
  }).asInstanceOf[Exp[A]]   
}

trait BaseGenStaticData extends GenericNestedCodegen {
  val IR: StaticDataExp
  import IR._
  
  def getFreeDataExp[A](sym: Sym[A], rhs: Def[A]): List[(Sym[Any],Any)] = rhs match {
    case StaticData(x) => List((sym,x))
    case _ => Nil
  }
  
  override def getFreeDataBlock[A](start: Block[A]): List[(Sym[Any],Any)] = {
    focusBlock(start) {
      focusExactScope(start) { levelScope =>
        levelScope flatMap { 
          case TP(sym, rhs) =>
            getFreeDataExp(sym, rhs)
          case _ => Nil //static data is never fat
        }
      }
    }
  }
  
}

trait ScalaGenStaticData extends ScalaGenEffect with BaseGenStaticData {
  val IR: StaticDataExp
  import IR._
  
  override def emitNode(sym: Sym[Any], rhs: Def[Any]) = rhs match {
    case StaticData(x) => 
      emitValDef(sym, "p"+quote(sym) + " // static data: " + (x match { case x: Array[_] => "Array("+x.mkString(",")+")" case _ => x }))
    case _ => super.emitNode(sym, rhs)
  }
  
}

