package org.jetbrains.plugins.scala.lang.parser;

import junit.framework.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

@RunWith(AllTests.class)
public class PackagingParserTest extends AbstractParserTest {

    public PackagingParserTest() {
        super("parser", "data", "packaging");
    }

    public static Test suite() {
        return new PackagingParserTest();
    }
}
