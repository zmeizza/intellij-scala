package org.jetbrains.plugins.scala.lang.parser;

import junit.framework.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

@RunWith(AllTests.class)
public class PackageParserTest extends AbstractParserTest {

    public PackageParserTest() {
        super("parser", "data", "package");
    }

    public static Test suite() {
        return new PackageParserTest();
    }
}
