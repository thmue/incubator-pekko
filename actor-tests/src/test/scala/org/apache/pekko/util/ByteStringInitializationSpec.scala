/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) 2020-2022 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.util

import java.io.InputStream

import scala.collection.mutable
import scala.collection.mutable.ArrayBuilder

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ByteStringInitializationSpec extends AnyWordSpec with Matchers {
  "ByteString intialization" should {
    "not get confused by initializing CompactByteString before ByteString" in {
      // a classloader that creates a new universe of classes for everything beneath akka
      // that prevents that this test interacts with any tests
      val cleanCl = new ClassLoader(null) {
        val outerCl = ByteStringInitializationSpec.this.getClass.getClassLoader
        val buffer = new Array[Byte](1000000)
        override def loadClass(name: String): Class[_] =
          if (!name.startsWith("org.apache.pekko")) outerCl.loadClass(name)
          else {
            val classFile = name.replace(".", "/") + ".class"
            val is = outerCl.getResourceAsStream(classFile)
            val res = slurp(is, new mutable.ArrayBuilder.ofByte)
            defineClass(name, res, 0, res.length)
          }

        def slurp(is: InputStream, res: ArrayBuilder[Byte]): Array[Byte] = {
          val read = is.read(buffer)
          if (read == 0) throw new IllegalStateException
          else if (read > 0) slurp(is, res ++= buffer.take(read))
          else res.result()
        }
      }

      import scala.language.reflectiveCalls
      type WithRun = { def run(): Unit }
      cleanCl
        .loadClass("org.apache.pekko.util.ByteStringInitTest")
        .getDeclaredConstructor()
        .newInstance()
        .asInstanceOf[WithRun]
        .run()
    }
  }
}

class ByteStringInitTest {
  def run(): Unit = {
    require(CompactByteString.empty ne null)
    require(ByteString.empty ne null)
  }
}
