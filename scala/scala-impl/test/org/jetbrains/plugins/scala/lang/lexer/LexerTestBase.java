package org.jetbrains.plugins.scala.lang.lexer;

import com.intellij.lexer.Lexer;
import org.jetbrains.plugins.scala.testcases.ScalaFileSetTestCase;

/**
 * User: Dmitry Naidanov
 * Date: 11/21/11
 */
abstract public class LexerTestBase extends ScalaFileSetTestCase {

  protected LexerTestBase(String... pathSegments) {
    super(pathSegments);
  }

  protected abstract Lexer createLexer();
  
  @Override
  public String transform(String testName, String[] data) {
    super.transform(testName, data);

    String fileText = data[0].replaceAll("\n+$", "");

    Lexer lexer = createLexer();
    lexer.start(fileText);

    StringBuilder buffer = new StringBuilder();

    while (lexer.getTokenType() != null) {
      buffer.append(prettyPrintToken(lexer));
      lexer.advance();
      if (lexer.getTokenType() != null) {
        buffer.append("\n");
      }
    }

    return buffer.toString();
  }

  protected String prettyPrintToken(Lexer lexer) {
    if (lexer.getTokenType() == null) return "null";

    CharSequence s = lexer.getBufferSequence();
    s = s.subSequence(lexer.getTokenStart(), lexer.getTokenEnd());
    return lexer.getTokenType().toString() + " {" + s + "}";
  }
}