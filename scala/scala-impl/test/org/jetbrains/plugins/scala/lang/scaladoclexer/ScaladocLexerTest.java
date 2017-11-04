
package org.jetbrains.plugins.scala.lang.scaladoclexer;

import com.intellij.lexer.Lexer;
import junit.framework.Test;
import org.jetbrains.plugins.scala.lang.lexer.LexerTestBase;
import org.jetbrains.plugins.scala.lang.scaladoc.lexer.ScalaDocLexer;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;


@RunWith(AllTests.class)
public class ScaladocLexerTest extends LexerTestBase {

  public ScaladocLexerTest() {
    super("lexer", "scaladocdata", "scaladoc");
  }

  @Override
  protected Lexer createLexer() {
    return new ScalaDocLexer();
  }

  public static Test suite() {
    return new ScaladocLexerTest();
  }
}

