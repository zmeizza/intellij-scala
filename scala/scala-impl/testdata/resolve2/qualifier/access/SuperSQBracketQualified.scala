trait Base

class A {
  trait T1 extends Base
}

class B extends A {
  trait T3 extends super[A]./* */T1
}
