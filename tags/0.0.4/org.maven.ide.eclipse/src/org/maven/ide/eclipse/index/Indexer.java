
package org.maven.ide.eclipse.index;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;
import org.apache.maven.model.Dependency;

import org.eclipse.core.runtime.IProgressMonitor;


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
  
  private static long totalClasses = 0;
  private static long totalFiles = 0;
  private static long totalSize = 0;
  
  private Analyzer analyzer;
  
  private final File[] indexes;
  

  public Indexer( File[] indexes) {
    this.indexes = indexes;
    this.analyzer = new StandardAnalyzer();
  }

  public Map search( String query, String field) throws ParseException, IOException {
    if(query==null || query.length()==0) {
      return Collections.EMPTY_MAP;
    }
    
    Query q;
    if(NAMES.equals( field)) {
      q = new WildcardQuery(new Term(NAMES, "*"+query+"*"));
    } else {
      q = new WildcardQuery(new Term(JAR_NAME, "*"+query+"*"));
    }

    IndexReader[] readers = new IndexReader[ indexes.length];
    for( int i = 0; i < indexes.length; i++ ) {
      readers[ i] = IndexReader.open( indexes[ i]);
    }
    
    IndexSearcher searcher = new IndexSearcher( new MultiReader( readers));
    Hits hits = searcher.search( q);
    if( hits.length()==0) {
      return Collections.EMPTY_MAP;
    }

    TreeMap res = new TreeMap();
    
    for( int i = 0; i < hits.length(); i++) {
      Document doc = hits.doc( i);
      String repository = doc.get( REPOSITORY);
      String jarSize = doc.get( JAR_SIZE);
      String jarDate = doc.get( JAR_DATE);
      String jarName = doc.get( JAR_NAME);
      
      int n1 = jarName.lastIndexOf( '/');
      int n2 = jarName.substring( 0, n1).lastIndexOf( '/');
      int n3 = jarName.substring( 0, n2).lastIndexOf( '/');
      
      String group = jarName.substring( 0, n3).replace('/', '.');
      String artifact = jarName.substring( n3+1, n2);
      String jarVersion = jarName.substring( n2+1, n1);
      String jarFile = jarName.substring( n1+1);

      if(JAR_NAME.equals(field)) {
        addFile(res, repository, jarSize, jarDate, group, artifact, jarVersion, jarFile, null, null);
      } else {
        String[] s = doc.get( NAMES).split( "\n");
        for( int i1 = 0; i1 < s.length; i1++) {
          String t = s[ i1];
          int n = t.lastIndexOf("/");
          String className = t.substring(n==-1 ? 0 : n+1);
          if( className.toLowerCase().indexOf(query)>-1) {
            String packageName = n==-1 ? "" : t.substring( 0, n).replace('/', '.');
            addFile( res, repository, jarSize, jarDate, group, artifact, jarVersion, jarFile, className, packageName );
          }
        }
      }
    }

    return res;
  }

  private void addFile( TreeMap res, String repository, String jarSize, String jarDate, 
      String group, String artifact, String jarVersion, 
      String jarFile, String className, String packageName ) {
    String key = group + " : "+artifact + " : " + className+" : "+packageName;
    ArtifactInfo info = ( ArtifactInfo) res.get(key);
    if(info==null) {
      info = new ArtifactInfo(group, artifact, packageName, className);
      res.put(key, info);
    }
    info.addFile(repository, group, jarFile, jarVersion, jarSize, jarDate);
  }
  
  
  public static void main( String[] args) throws IOException, ParseException {
    if( args.length<2) {
      printUsage();
      return;
    }
    
    Indexer indexer;
    String command = args[ 0];
    if( "index".equals( command)) {
      String repositoryName = args[ 1];  
      String repositoryPath = args[ 1];  
      String indexPath = args.length>2 ? "index" : args[ 2];
      
      Indexer.reindex( indexPath, repositoryPath, repositoryName, null);

    } else if( "search".equals( command)) {
      String query = args[ 1];
      String indexPath = args.length==2 ? "index" : args[ 2];

      indexer = new Indexer( new File[] { new File( indexPath)});
      Map res = indexer.search( query, JAR_NAME);
      
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

  
  public static IndexWriter createIndexWriter( String indexPath, boolean create ) throws IOException {
    Analyzer analyzer = new StandardAnalyzer();
    return new IndexWriter( indexPath, analyzer, create);
  }

  public static void reindex( String indexPath, String repositoryPath, String repositoryName, IProgressMonitor monitor) throws IOException {
    IndexWriter w = createIndexWriter( indexPath, true );
    
    long l1 = System.currentTimeMillis();
    processDir(new File( repositoryPath), w, repositoryPath, repositoryName, monitor);
    long l2 = System.currentTimeMillis();
    // System.err.println( "Done. "+((l2-l1)/1000f));
  
    long l3 = System.currentTimeMillis();
    // System.err.println( "Optimizing...");
    w.optimize();
    w.close();
    long l4 = System.currentTimeMillis();
    // System.err.println( "Done. "+((l4-l3)/1000f));
    
    // System.err.println( "Total classes: " + totalClasses);
    // System.err.println( "Total jars:    " + totalFiles);
    // System.err.println( "Total size:    " + ( totalSize / 1024 / 1024)+" Mb");
    // System.err.println( "Speed:         " + ( totalSize / ((l2-l1) / 1000f)) + " b/sec");
    monitor.subTask( "Completed." );
  }

  private static void processDir( File dir, IndexWriter w, String repositoryPath, String repositoryName, IProgressMonitor monitor) throws IOException {
    if(dir==null) return;
    if(monitor!=null && monitor.isCanceled()) return;
    
    File[] files = dir.listFiles();
    for( int i = 0; files!=null && i < files.length; i++) {
      File f = files[ i];
      if(f.isDirectory()) processDir(f, w, repositoryPath, repositoryName, monitor);
      else processFile(f, w, repositoryPath, repositoryName, monitor);
    }
  }

  private static void processFile( File f, IndexWriter w, String repositoryPath, String repositoryName, IProgressMonitor monitor) {
    if(monitor!=null && monitor.isCanceled()) return;

    if(f.isFile() && f.getName().endsWith( ".pom")) {  // TODO
      String name = f.getName();
      File jarFile = new File( f.getParent(), name.substring( 0, name.length() - 4) + ".jar");
      
      String absolutePath = f.getAbsolutePath();
      long size = 0;
      if( jarFile.exists()) {
        size = jarFile.length();
        absolutePath = jarFile.getAbsolutePath();
      }
      String jarName = absolutePath.substring( repositoryPath.length()).replace( '\\', '/');
      addDocument( w, repositoryName, jarName, size, f.lastModified() );
      
      totalFiles++;
      totalSize += size;
//      if(( totalFiles % 100)==0) {
//        System.err.println( "Indexing "+totalFiles+" "+f.getParentFile().getAbsolutePath().substring( repositoryPath.length()));
//      }
      monitor.subTask( totalFiles+" "+f.getParentFile().getAbsolutePath().substring( repositoryPath.length()) );
    }
  }

  public static void addDocument( IndexWriter w, String repository, String name, long size, long date ) {
    Document doc = new Document();
    doc.add( Field.UnIndexed( REPOSITORY, repository));
    doc.add( Field.UnIndexed( JAR_DATE, DateField.timeToString( date)));
    doc.add( Field.UnIndexed( JAR_SIZE, Long.toString(size)));
    doc.add( Field.Text( JAR_NAME, name.charAt(0)=='/' ? name.substring(  1 ) : name));
    // TODO calculate jar's sha1 or md5

    // store class names into the index
//      ZipFile jar = null;
    try {
/*      
      jar = new ZipFile( f);
      
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
      doc.add( Field.Text( NAMES, sb.toString()));
      

    } finally {
      try {
        jar.close();
      } catch( Exception e) {
      }
*/    
      w.addDocument(doc);
    } catch( Exception e) {
//      e.printStackTrace();
//      System.err.println( "Error for file "+f);
//      System.err.println( "  "+e.getMessage());
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

    public void addFile( String repository, String group, String name, String version, String size, String date) {
      files.add( new FileInfo( repository, group, artifact, name, version, size, date));
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
    public final String size;
    public final Date date;

    public FileInfo(String repository, String group, String artifact, String name, String version, String size, String date) {
      this.repository = repository;
      this.group = group;
      this.artifact = artifact;
      this.name = name;
      this.version = version;
      this.size = size;
      this.date = DateField.stringToDate(date);
    }

    public Dependency getDependency() {
      Dependency dependency = new Dependency();
      dependency.setArtifactId( artifact);
      dependency.setGroupId( group);
      dependency.setVersion( version);
      dependency.setType( "jar");  // TODO
      return dependency;
    }
  }

}

