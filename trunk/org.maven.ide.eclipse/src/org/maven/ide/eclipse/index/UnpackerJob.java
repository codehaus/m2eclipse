
package org.maven.ide.eclipse.index;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.maven.ide.eclipse.Maven2Plugin;
import org.osgi.framework.Bundle;


/**
 * UnpackerJob
 *
 * @author Eugene Kuleshov
 */
class UnpackerJob extends Job {
  private final Bundle bundle;
  private final File indexDir;
  private final Set indexes;
  private final String[] indexNames;

  public UnpackerJob(Bundle bundle, File indexDir, String[] indexNames, Set indexes) {
    super("Initializing indexes");
    this.bundle = bundle;
    this.indexDir = indexDir;
    this.indexNames = indexNames;
    this.indexes = indexes;

    setPriority(Job.LONG);
  }

  protected IStatus run(IProgressMonitor monitor) {
    for(int i = 0; i < indexNames.length; i++ ) {
      String name = indexNames[i];

      File index = new File(indexDir, name);
      if(!index.exists()) {
        index.mkdirs();
      } else {
        File[] files = index.listFiles();
        for(int j = 0; j < files.length; j++ ) {
          files[j].delete();
        }
      }

      monitor.subTask(name);
      URL indexArchive = bundle.getEntry(name + ".zip");
      InputStream is = null;
      ZipInputStream zis = null;
      try {
        is = indexArchive.openStream();
        zis = new ZipInputStream(is);
        ZipEntry entry;
        byte[] buf = new byte[4096];
        while((entry = zis.getNextEntry()) != null) {
          File indexFile = new File(index, entry.getName());
          FileOutputStream fos = null;
          try {
            fos = new FileOutputStream(indexFile);
            int n = 0;
            while((n = zis.read(buf)) != -1) {
              fos.write(buf, 0, n);
            }
          } finally {
            close(fos);
          }
        }
        indexes.add(name);
      } catch(Exception ex) {
        Maven2Plugin.log(new Status(IStatus.ERROR, Maven2Plugin.PLUGIN_ID, -1, "Unable to initialize indexes", ex));

      } finally {
        close(zis);
        close(is);
      }
    }
    return Status.OK_STATUS;
  }

  private void close(InputStream is) {
    try {
      if(is != null)
        is.close();
    } catch(IOException ex) {
    }
  }

  private void close(OutputStream os) {
    try {
      if(os != null)
        os.close();
    } catch(IOException ex) {
    }
  }

}
