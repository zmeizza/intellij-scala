/*
 * Copyright 2000-2008 JetBrains s.r.o.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.plugins.scala.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.testFramework.EditorTestUtil;
import com.intellij.testFramework.ThreadTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.scala.Console;
import org.jetbrains.plugins.scala.debugger.DebuggerTestUtil$;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Ilya.Sergey
 */
public class TestUtils {
  private static final Logger LOG = Logger.getInstance("org.jetbrains.plugins.scala.util.TestUtils");

  public static final String BEGIN_MARKER = "<begin>";
  public static final String END_MARKER = "<end>";

  private static String TEST_DATA_PATH = null;

  @NotNull
  public static String getTestDataPath() {
    if (TEST_DATA_PATH == null) {
      ClassLoader loader = TestUtils.class.getClassLoader();
      URL resource = loader.getResource("testdata");
      try {
        File f1 = new File("community/scala/scala-impl", "testdata");
        if (f1.exists()) {
          TEST_DATA_PATH = f1.getAbsolutePath();
        } else {
          File f2 = findTestDataDir(new File("scala/scala-impl").getCanonicalFile());
          TEST_DATA_PATH = f2.getAbsolutePath();
        }
        if (resource != null) {
          TEST_DATA_PATH = new File(resource.toURI()).getPath().replace(File.separatorChar, '/');
        }
      } catch (URISyntaxException e) {
        LOG.error(e);
        throw new RuntimeException(e);
        // just rethrowing here because that's a clearer way to make tests fail than some NPE somewhere else
      } catch (IOException e) {
        LOG.error(e);
        throw new RuntimeException(e);
      }
    }

    return TEST_DATA_PATH;
  }

  /** Go upwards to find testdata, because when running test from IDEA, the launching dir might be some subdirectory. */
  @NotNull
  private static File findTestDataDir(File here) throws IOException {
    File testdata = new File(here,"testdata").getCanonicalFile();
    if (testdata.exists()) return testdata;
    else {
      File parent = here.getParentFile();
      if (parent == null) throw new RuntimeException("no testdata directory found");
      else return findTestDataDir(parent);
    }
  }

  public static Sdk createJdk() {
    String path = DebuggerTestUtil$.MODULE$.discoverJDK18().get();
    VfsRootAccess.allowRootAccess(path);
    return JavaSdk.getInstance().createJdk("java sdk", path, false);
  }

  public static String getScalaLibrarySrc() {
    return getIvyCachePath() + "/org.scala-lang/scala-library/srcs/scala-library-2.10.6-sources.jar";
  }

  public static String getIvyCachePath() {
    String homePath = System.getProperty("user.home") + "/.ivy2/cache";
    String ivyCachePath = System.getProperty("sbt.ivy.home");
    String result = ivyCachePath != null ? ivyCachePath + "/cache" : homePath;
    return result.replace("\\", "/");
  }

  public static String getScalaLibraryPath() {
    return getIvyCachePath() + "/org.scala-lang/scala-library/jars/scala-library-2.10.6.jar";
  }

    public static String removeBeginMarker(String text, int offset) {
        return removeMarker(text, BEGIN_MARKER, offset);
    }

    public static String removeEndMarker(String text, int offset) {
        return removeMarker(text, END_MARKER, offset);
    }

    public static String removeCaretMarker(String text, int offset) {
        return removeMarker(text, EditorTestUtil.CARET_TAG, offset);
  }

    public static String removeMarker(String text, String marker, int offset) {
        return text.substring(0, offset) + text.substring(offset + marker.length());
  }

  private static final long ETALON_TIMING = 438;

  private static final boolean COVERAGE_ENABLED_BUILD = "true".equals(System.getProperty("idea.coverage.enabled.build"));

  private static void assertTiming(String message, long expected, long actual) {
    if (COVERAGE_ENABLED_BUILD) return;
    long expectedOnMyMachine = expected * Timings.MACHINE_TIMING / ETALON_TIMING;
    final double acceptableChangeFactor = 1.1;

    // Allow 10% more in case of test machine is busy.
    // For faster machines (expectedOnMyMachine < expected) allow nonlinear performance rating:
    // just perform better than acceptable expected
    if (actual > expectedOnMyMachine * acceptableChangeFactor &&
        (expectedOnMyMachine > expected || actual > expected * acceptableChangeFactor)) {
      int percentage = (int)(((float)100 * (actual - expectedOnMyMachine)) / expectedOnMyMachine);
      Assert.fail(message + ". Operation took " + percentage + "% longer than expected. Expected on my machine: " + expectedOnMyMachine +
                  ". Actual: " + actual + ". Expected on Etalon machine: " + expected + "; Actual on Etalon: " +
                  (actual * ETALON_TIMING / Timings.MACHINE_TIMING));
    }
    else {
      int percentage = (int)(((float)100 * (actual - expectedOnMyMachine)) / expectedOnMyMachine);
      Console.println(message + ". Operation took " + percentage + "% longer than expected. Expected on my machine: " +
                         expectedOnMyMachine + ". Actual: " + actual + ". Expected on Etalon machine: " + expected +
                         "; Actual on Etalon: " + (actual * ETALON_TIMING / Timings.MACHINE_TIMING));
    }
  }

  public static void assertTiming(String message, long expected, @NotNull Runnable actionToMeasure) {
    int attempts = 4;
    while (true) {
      attempts--;
      long start = System.currentTimeMillis();
      actionToMeasure.run();
      long finish = System.currentTimeMillis();
      try {
        assertTiming(message, expected, finish - start);
        break;
      } catch (AssertionError e) {
        if (attempts == 0) throw e;
        System.gc();
        System.gc();
        System.gc();
      }
    }
  }
  
  public static List<String> readInput(String filePath) throws IOException {
    String content = new String(FileUtil.loadFileText(new File(filePath)));
    Assert.assertNotNull(content);

    List<String> input = new ArrayList<String>();

    int separatorIndex;
    content = StringUtil.replace(content, "\r", ""); // for MACs

    // Adding input  before -----
    while ((separatorIndex = content.indexOf("-----")) >= 0) {
      input.add(content.substring(0, separatorIndex - 1));
      content = content.substring(separatorIndex);
      while (StringUtil.startsWithChar(content, '-')) {
        content = content.substring(1);
      }
      if (StringUtil.startsWithChar(content, '\n')) {
        content = content.substring(1);
      }
    }
    // Result - after -----
    if (content.endsWith("\n")) {
      content = content.substring(0, content.length() - 1);
    }
    input.add(content);

    Assert.assertTrue("No data found in source file", input.size() > 0);
    Assert.assertNotNull("Test output points to null", input.size() > 1);

    return input;
  }


  public static void disableTimerThread() {
    ThreadTracker.longRunningThreadCreated(ApplicationManager.getApplication(), "Timer");
    ThreadTracker.longRunningThreadCreated(ApplicationManager.getApplication(), "BaseDataReader");
    ThreadTracker.longRunningThreadCreated(ApplicationManager.getApplication(), "ProcessWaitFor");
  }
}
