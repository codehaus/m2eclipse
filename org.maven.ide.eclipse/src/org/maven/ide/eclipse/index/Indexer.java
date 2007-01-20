
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
import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexModifier;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;
import org.apache.maven.model.Dependency;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;


/**
 * Indexer utility for prototyping purposes
 * 
 * @author Eugene Kuleshov
 */
public class Indexer {
  public static final String REPOSITORY = "r";
  public static final String JAR_NAME = "j";
  public static final String JAR_SIZE = "s";
  public static final String JAR_DATE = "d";
  public static final String NAMES = "c";
  
  public long totalClasses = 0;
  public long totalFiles = 0;
  public long totalSize = 0;
  
  private long lastTime = System.currentTimeMillis();

  public Map search( File[] indexes, String query, String field) throws IOException {
    if(query==null || query.length()==0) {
      return Collections.EMPTY_MAP;
    }
    
    Query q;
    if(query.indexOf('*') > -1) {
      q = new WildcardQuery(new Term(field, query));
    } else {
      String[] terms = query.split("[\\. -/\\\\]");
      int len = terms.length;
      if(len > 1) {
        q = new PhraseQuery();
        for(int i = 0; i < len; i++ ) {
          ((PhraseQuery) q).add(new Term(field, terms[i]));
        }
      } else {
        // q = new TermQuery(new Term(field, query));
        q = new WildcardQuery(new Term(field, query + "*"));
      }
    }
      
    IndexReader[] readers = new IndexReader[indexes.length];
    try {
      for(int i = 0; i < indexes.length; i++ ) {
        readers[i] = IndexReader.open(indexes[i]);
      }

      IndexSearcher searcher = new IndexSearcher(new MultiReader(readers));
      Hits hits = searcher.search(q);

      if(hits == null || hits.length() == 0) {
        return Collections.EMPTY_MAP;
      }
      return sortResults(query, field, hits);

    } finally {
      for(int i = 0; i < readers.length; i++ ) {
        try {
          readers[i].close();
        } catch(IOException ex) {
          // ignore
        }
      }
    }
  }

  private TreeMap sortResults(String query, String field, Hits hits) throws IOException {
    TreeMap res = new TreeMap();
    for( int i = 0; i < hits.length(); i++) {
      Document doc = hits.doc( i);
      FileInfo fileInfo = FileInfo.getFileInfo(doc);
      if(fileInfo==null) continue;

      if(JAR_NAME.equals(field)) {
        addFile(res, fileInfo, null, null);

      } else if(NAMES.equals( field )) {
        String[] entries = doc.get(NAMES).split( "\n");
        for( int j = 0; j < entries.length; j++) {
          String entry = entries[j];
          if(query.indexOf( '.' )==-1) {
            // class name
            int n = entry.lastIndexOf("/");
            String className = entry.substring(n==-1 ? 0 : n+1);
            String packageName = n==-1 ? "" : entry.substring( 0, n).replace('/', '.');
            
            if( query.endsWith( "*")) {
              int queryLength = query.length();
              if(query.charAt( 0 )=='*' ? 
                  className.toLowerCase().indexOf( query.substring( 1, queryLength-2 ) )>1 : 
                  className.toLowerCase().startsWith( query.substring( 0, queryLength-2 ) ) ) {
                addFile( res, fileInfo, className, packageName );
              }
            } else {
              if(query.charAt( 0 )=='*' ? 
                  className.toLowerCase().endsWith( query.substring( 1 ) ) : 
                  className.equalsIgnoreCase( query ) ) {
                addFile( res, fileInfo, className, packageName );
              }
            }
            
          } else {
            // qualified class or package
            if( entry.equals(query.replace( '.', '/' )) ) {
              // qualified class name
              int n = entry.lastIndexOf("/");
              String className = entry.substring(n==-1 ? 0 : n+1);
              String packageName = n==-1 ? "" : entry.substring( 0, n).replace('/', '.');
              addFile( res, fileInfo, className, packageName );
            
            } else if( entry.startsWith(query.replace( '.', '/' )) ) {
              // package name
              addFile( res, fileInfo, null, query );
            
            }
          }
        }
      }
    }
    return res;
  }

  private void addFile( TreeMap res, FileInfo fileInfo, String className, String packageName ) {
    // String key = group + " : "+artifact + " : " + className+" : "+packageName;
    String key = className + " : "+packageName + " : " + fileInfo.group + " : "+fileInfo.artifact;
    ArtifactInfo info = ( ArtifactInfo) res.get(key);
    if(info==null) {
      info = new ArtifactInfo(fileInfo.group, fileInfo.artifact, packageName, className);
      res.put(key, info);
    }
    info.addFile(fileInfo);
  }
  
  
  public static void main( String[] args) throws IOException {
    if( args.length<2) {
      printUsage();
      return;
    }
    
    Indexer indexer = new Indexer();
    String command = args[ 0];
    if( "index".equals( command)) {
      String repositoryName = args[ 1];  
      String repositoryPath = args[ 1];  
      String indexPath = args.length>2 ? args[ 2] : "index";
      
      long l1 = System.currentTimeMillis();
      
      indexer.reindex( indexPath, repositoryPath, repositoryName, new NullProgressMonitor() {
          public void beginTask(String name, int totalWork) {
            System.err.println(name);
          }
          
          public void subTask(String name) {
            System.err.println(name);;
          }
        });
      
      long l2 = System.currentTimeMillis();
      System.err.println("Total time: "+((l2-l1)/1000f));
      System.err.println("Total files: "+indexer.totalFiles);
      System.err.println("Total classes: "+indexer.totalClasses);
      System.err.println("Total size: "+indexer.totalSize);
      
    } else if( "search".equals( command)) {
      String query = args[ 1];
      String indexPath = args.length==2 ? "index" : args[ 2];

      Map res = indexer.search(new File[] { new File( indexPath)}, query, JAR_NAME);
      
      for( Iterator it = res.entrySet().iterator(); it.hasNext();) {
        Map.Entry e = ( Map.Entry) it.next();
        System.err.println( e);
      }
    }
  }

  private static void printUsage() {
    System.err.println( "indexer <command> <args>");
    System.err.println( "  index <repository name> <repository path> <index path>");
    System.err.println( "  search <query> <index path>");
  }

  
  private IndexerAdapter createIndexerAdapter( String indexPath, boolean create ) throws IOException {
    final File indexDir = new File(indexPath);
    try {
      return new IndexerModifierAdapter( indexDir, create );
    } catch( IOException ex ) {
      return new IndexerWriterAdapter(indexDir, true);
    }
  }

  public void reindex( String indexPath, String repositoryPath, String repositoryName, IProgressMonitor monitor) throws IOException {
    IndexerAdapter w = createIndexerAdapter( indexPath, false );
    monitor.beginTask( "Indexing "+repositoryName, IProgressMonitor.UNKNOWN );
    try {
      processDir( new File( repositoryPath ), w, repositoryPath, repositoryName, monitor );

      w.optimize();
      monitor.worked( 1 );

    } catch(IOException ex) {
      throw ex;

    } finally {
      w.close();
      monitor.worked( 1 );
      monitor.done();
    }
  }

  private void processDir( File dir, IndexerAdapter w, String repositoryPath, String repositoryName, IProgressMonitor monitor) throws IOException {
    if(monitor.isCanceled() || dir==null || !dir.exists()) return;

    File[] files = dir.listFiles();
    if(files==null) return;
    // monitor.beginTask( "Processing "+dir.getAbsolutePath(), files.length );
    monitor.worked( 1 );
    try {
      long time = System.currentTimeMillis();
      if((time-lastTime)>1000) {
        monitor.subTask( dir.getAbsolutePath() );
        lastTime = time;
      }
      for( int i = 0; i < files.length; i++) {
        File f = files[ i];
        if(f.isDirectory()) {
          // processDir(f, w, repositoryPath, repositoryName, new SubProgressMonitor(monitor, 1) );
          processDir(f, w, repositoryPath, repositoryName, monitor );
        } else if(f.isFile()) {
          processFile(f, w, repositoryPath, repositoryName, monitor);
        }
      }
    } finally {
      // monitor.done();
    }
  }

  private void processFile( File f, IndexerAdapter w, String repositoryPath, String repositoryName, IProgressMonitor monitor) throws IOException {
    if(monitor.isCanceled()) return;

    totalFiles++;

    String name = f.getName();

    String absolutePath = f.getAbsolutePath();
    String jarName = absolutePath.substring(repositoryPath.length()).replace( '\\', '/');
    
    FileInfo fileInfo = w.get(jarName);
    if(fileInfo!=null) {
      return;  // TODO compare date and size
    }

    long size;
    String names = null;
    
    if(name.endsWith( ".jar" )) {
      size = f.length();
      names = readNames(f);
      totalSize += size;
      
    } else if(name.endsWith( ".pom" )) {
      File jarFile = new File( f.getParent(), name.substring( 0, name.length() - 4) + ".jar");
      if(jarFile.exists()) {
        return;
      }      
      size = 0;

    } else {
      return;
    }
    
    addDocument( repositoryName, jarName, size, f.lastModified(), names, w );
    
//      if(( totalFiles % 100)==0) {
//        System.err.println( "Indexing "+totalFiles+" "+f.getParentFile().getAbsolutePath().substring( repositoryPath.length()));
//      }
      // monitor.subTask( totalFiles+" "+f.getParentFile().getAbsolutePath().substring( repositoryPath.length()) );
    // monitor.worked( 1 );
  }

  public void addDocument( String repository, String name, long size, long date, String names, String indexPath ) throws IOException {
    IndexerAdapter w = createIndexerAdapter( indexPath, false );
    try {
      addDocument( repository, name, size, date, names, w );
      w.optimize();
    } catch( Exception e ) {
      w.close();
    }
  }
  
  public static void addDocument( String repository, String name, long size, long date, String names, IndexerAdapter w ) throws IOException {
    if(name.charAt(0)=='/') {
      name = name.substring(1);
    }
    
    Document doc = new Document();
    doc.add( new Field( REPOSITORY, repository, Field.Store.YES, Field.Index.NO));
    doc.add( new Field( JAR_NAME, name, Field.Store.YES, Field.Index.TOKENIZED));
    doc.add( new Field( JAR_DATE, DateTools.timeToString( date, DateTools.Resolution.MINUTE), Field.Store.YES, Field.Index.NO));
    doc.add( new Field( JAR_SIZE, Long.toString(size), Field.Store.YES, Field.Index.NO));
    
    if(names!=null) {
      doc.add( new Field( NAMES, names, Field.Store.COMPRESS, Field.Index.TOKENIZED));
    }
    // TODO calculate jar's sha1 or md5

    w.add(doc);
  }

  public String readNames(File jarFile) {
    ZipFile jar = null;
    try {
      jar = new ZipFile( jarFile);
      
      StringBuffer sb = new StringBuffer();
      for( Enumeration en = jar.entries(); en.hasMoreElements();) {
        ZipEntry e = ( ZipEntry) en.nextElement();
        String name = e.getName();
        if( name.endsWith( ".class")) {
          totalClasses++;
          // TODO verify if class is public or protected
          // TODO skipp all inner classes for now
          int i = name.lastIndexOf( "$");
          if( i==-1) {
            sb.append( name.substring( 0, name.length() - 6)).append( "\n");
          }
        }
      }
      return sb.toString();
      
    } catch( Exception e) {
      // System.err.println( "Error for file "+jarFile.getAbsolutePath());
      // System.err.println( "  "+e.getMessage());
      return null;
      
    } finally {
      if(jar!=null) {
        try {
          jar.close();
        } catch( Exception e) {
        }
      }
    }
  }

  
  public static final FileInfoComparator FILE_INFO_COMPARATOR = new FileInfoComparator();
  public static class FileInfoComparator implements Comparator {

    public int compare( Object o1, Object o2) {
      FileInfo f1 = ( FileInfo) o1;
      FileInfo f2 = ( FileInfo) o2;
      return -f1.version.compareTo( f2.version);
    }
  }
  
  public static class ArtifactInfo {
    public final String group;
    public final String artifact;
    public final String packageName;
    public final String className;
    public final Set files = new TreeSet(FILE_INFO_COMPARATOR);
    
    public ArtifactInfo( String group, String artifact, String packageName, String className) {
      this.group = group;
      this.artifact = artifact;
      this.packageName = packageName;
      this.className = className;
    }

    public void addFile(FileInfo fileInfo) {
      files.add(fileInfo);
    }
    
    public String toString() {
      StringBuffer sb = new StringBuffer( className+"  "+packageName +"  "+group+"\n");
      for( Iterator it = files.iterator(); it.hasNext();) {
        FileInfo f = ( FileInfo) it.next();
        sb.append( "  "+f.version+"  "+f.name+"\n");
      }
      return sb.toString();
    }
    
  }

  
  public static class FileInfo {
    public final String repository;
    public final String group;
    public final String artifact;
    public final String name;
    public final String version;
    public final long size;
    public final Date date;

    private FileInfo( String repository, String group, String artifact, String version, String name, long size, Date date ) {
      this.repository = repository;
      this.group = group;
      this.artifact = artifact;
      this.version = version;
      this.name = name;
      this.size = size;
      this.date = date;
    }

    public Dependency getDependency() {
      Dependency dependency = new Dependency();
      dependency.setArtifactId( artifact);
      dependency.setGroupId( group);
      dependency.setVersion( version);
      dependency.setType( "jar");  // TODO
      return dependency;
    }
    
    public static FileInfo getFileInfo(Document doc) {
      String repository = doc.get( REPOSITORY);
      
      String jarName = doc.get( JAR_NAME);    
      int n1 = jarName.lastIndexOf( '/');
      if(n1==-1) return null;
      int n2 = jarName.substring( 0, n1).lastIndexOf( '/');
      if(n2==-1) return null;
      int n3 = jarName.substring( 0, n2).lastIndexOf( '/');
      if(n3==-1) return null;
      
      String group = jarName.substring( 0, n3).replace('/', '.');
      String artifact = jarName.substring( n3+1, n2);
      String version = jarName.substring( n2+1, n1);
      String name = jarName.substring( n1+1);

      long size;
      try {
        size = Long.parseLong(doc.get( JAR_SIZE));
      } catch( NumberFormatException ex1 ) {
        return null;
      }
      
      Date date = null;
      try {
        date = DateTools.stringToDate(doc.get( JAR_DATE));
      } catch( ParseException ex ) {
        return null;
      }
      
      return new FileInfo(repository, group, artifact, version, name, size, date);
    }
  }

  
  private static interface IndexerAdapter {
    void add( Document doc ) throws IOException;
    FileInfo get( String jarName );
    void optimize() throws IOException;
    void close() throws IOException;
  }
  
  private static class IndexerModifierAdapter implements IndexerAdapter {
    private IndexModifier m;
    private HashMap documents = new HashMap();

    public IndexerModifierAdapter( File indexDir, boolean create ) throws IOException {
      boolean shouldCreate = create || !IndexReader.indexExists( indexDir );
      if(!shouldCreate) {
        IndexReader r = null;
        try {
          r = IndexReader.open( indexDir );
          int n = r.numDocs();
          for( int i = 0; i < n; i++ ) {
            Document doc = r.document( i );
            String jarName = doc.get( JAR_NAME );
            documents.put( jarName, FileInfo.getFileInfo(doc));            
          }
        } catch(IOException ex) {
          shouldCreate = true;
        } finally {
          if(r!=null) {
            r.close();
          }
        }
      }
      
      m = new IndexModifier(indexDir, new StandardAnalyzer(), shouldCreate);
    }

    public void add( Document doc ) throws IOException {
      m.addDocument( doc );
    }

    public FileInfo get( String jarName ) {
      return ( FileInfo ) documents.get( jarName.charAt(0)=='/' ? jarName.substring(1) : jarName );
    }

    public void optimize() throws IOException {
      m.optimize();
      m.flush();
    }

    public void close() throws IOException {
      m.close();
    }
    
  }

  
  private static class IndexerWriterAdapter implements IndexerAdapter {
    private IndexWriter w;

    public IndexerWriterAdapter(File indexDir, boolean create) throws IOException {
      w = new IndexWriter(indexDir, new StandardAnalyzer(), create);
    }
    
    public void add( Document doc ) throws IOException {
      w.addDocument(doc);
      
    }

    public FileInfo get( String jarName ) {
      return null;
    }
    
    public void close() throws IOException {
      w.close();
    }

    public void optimize() throws IOException {
      w.optimize();
    }
    
  }
  
}

