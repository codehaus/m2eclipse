package org.maven.ide.eclipse.wizards;

/*
 * Licensed to the Codehaus Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Simple class representing a directory of the default Maven2 directory
 * structure.
 *
 * Each Maven2 directory is mainly caracterized by its path and its ouput path
 * in which any potential output created during the processing of the directory
 * is placed.
 */
final class Maven2Directory {

  /** The path of this Maven2 directory. */
  private String path = null;

  /** The output path of this Maven2 directory. */
  private String outputPath = null;

  /** Default marker */
  private final boolean isDefault;

  /**
   * Constructor.
   *
   * Creates a Maven2 directory for the given <code>path</code> having the
   * given <code>outputPath</code>.
   *
   * @param path           The relative path of the Maven2 directory.
   * @param outputPath     The relative output path of this Maven2 directory
   *                       or <code>null</code> if processing this directory
   *                       does not produce any output.
   * @param isSourceEntry  Whether this Maven2 directory is a source or source
   *                       related resource directory. Directories having this
   *                       flag set will have an appropriate classpath entry.
   *                       The following Maven2 directories should have this
   *                       flag set:
   *                       <ul>
   *                         <li>src/main/java</li>
   *                         <li>src/main/resources</li>
   *                         <li>src/test/java</li>
   *                         <li>src/test/resources</li>
   *                       </ul>
   */
  Maven2Directory( String path, String outputPath, boolean isDefault ) {
    this.path = path;
    this.outputPath = outputPath;
    this.isDefault = isDefault;
  }

  /**
   * Returns the relative path of the Maven2 directory as a <code>String</code>.
   *
   * @return  The relative path of the Maven2 directory.
   *          Is never <code>null</code>.
   */
  String getPath() {
    return path;
  }

  /**
   * Returns the relative output path in which resources resulting from the
   * processing of this Maven2 directory, if any, should be placed.
   *
   * @return  The relative output path of this Maven2 directory or
   *          <code>null</code> if processing this directory does not produce
   *          any output.
   */
  String getOutputPath() {
    return outputPath;
  }

  /**
   * Returns whether this Maven2 directory is a source or source related
   * resource directory.
   *
   * @return  Whether this directory is a source or source related resource
   *          directory.
   */
  boolean isSourceEntry() {
    return this.getOutputPath()!=null;
  }
  
  boolean isDefault() {
    return this.isDefault;
  }
  
}

