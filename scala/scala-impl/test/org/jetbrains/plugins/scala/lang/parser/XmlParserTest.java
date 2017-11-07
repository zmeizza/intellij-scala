package org.jetbrains.plugins.scala.lang.parser;

import junit.framework.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

@RunWith(AllTests.class)
public class XmlParserTest extends AbstractParserTest {

    public XmlParserTest() {
        super("parser", "data", "xml");
    }

    public static Test suite() {
        return new XmlParserTest();
    }
}
