class A {
  def f = 42
}
object A
object B
class B extends A {
  B.super[<ref>A].f
}