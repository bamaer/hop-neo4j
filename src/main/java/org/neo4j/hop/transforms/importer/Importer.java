package org.neo4j.hop.transforms.importer;

import org.neo4j.hop.transforms.gencsv.StreamConsumer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.logging.LogLevel;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransform;
import org.apache.hop.pipeline.transform.ITransform;
import org.apache.hop.pipeline.transform.TransformMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Importer extends BaseTransform<ImporterMeta, ImporterData> implements ITransform<ImporterMeta, ImporterData> {

  public Importer( TransformMeta transformMeta, ImporterMeta meta, ImporterData data, int copyNr, PipelineMeta pipelineMeta,
                   Pipeline pipeline ) {
    super( transformMeta, meta, data, copyNr, pipelineMeta, pipeline );
  }

  @Override public boolean processRow() throws HopException {

    Object[] row = getRow();
    if ( row == null ) {
      if ( ( data.nodesFiles!=null && !data.nodesFiles.isEmpty() ) || ( data.relsFiles!=null && !data.relsFiles.isEmpty() ) ) {
        runImport();
      }
      setOutputDone();
      return false;
    }

    if ( first ) {
      first = false;

      data.nodesFiles = new ArrayList<>();
      data.relsFiles = new ArrayList<>();

      data.filenameFieldIndex = getInputRowMeta().indexOfValue( meta.getFilenameField() );
      if ( data.filenameFieldIndex < 0 ) {
        throw new HopException( "Unable to find filename field " + meta.getFilenameField() + "' in the transform input" );
      }
      data.fileTypeFieldIndex = getInputRowMeta().indexOfValue( meta.getFileTypeField() );
      if ( data.fileTypeFieldIndex < 0 ) {
        throw new HopException( "Unable to find file type field " + meta.getFileTypeField() + "' in the transform input" );
      }

      if ( StringUtils.isEmpty( meta.getAdminCommand() ) ) {
        data.adminCommand = "neo4j-admin";
      } else {
        data.adminCommand = environmentSubstitute( meta.getAdminCommand() );
      }

      data.databaseFilename = environmentSubstitute( meta.getDatabaseFilename() );
      data.reportFile = environmentSubstitute( meta.getReportFile() );

      data.baseFolder = environmentSubstitute( meta.getBaseFolder() );
      if ( !data.baseFolder.endsWith( File.separator ) ) {
        data.baseFolder += File.separator;
      }

      data.importFolder = data.baseFolder + "import/";

      data.readBufferSize = environmentSubstitute( meta.getReadBufferSize() );

      data.maxMemory = environmentSubstitute( meta.getMaxMemory() );

    }

    String filename = getInputRowMeta().getString( row, data.filenameFieldIndex );
    String fileType = getInputRowMeta().getString( row, data.fileTypeFieldIndex );

    if ( StringUtils.isNotEmpty( filename ) && StringUtils.isNotEmpty( fileType ) ) {

      if ( "Node".equalsIgnoreCase( fileType ) ||
        "Nodes".equalsIgnoreCase( fileType ) ||
        "N".equalsIgnoreCase( fileType ) ) {
        data.nodesFiles.add( filename );
      }
      if ( "Relationship".equalsIgnoreCase( fileType ) ||
        "Relationships".equalsIgnoreCase( fileType ) ||
        "Rel".equalsIgnoreCase( fileType ) ||
        "Rels".equalsIgnoreCase( fileType ) ||
        "R".equalsIgnoreCase( fileType ) ||
        "Edge".equalsIgnoreCase( fileType ) ||
        "Edges".equalsIgnoreCase( fileType ) ||
        "E".equalsIgnoreCase( fileType ) ) {
        data.relsFiles.add( filename );
      }
    }

    // Pay it forward
    //
    putRow( getInputRowMeta(), row );

    return true;
  }


  private void runImport() throws HopException {

    // See if we need to delete the existing database folder...
    //
    String targetDbFolder = data.baseFolder + "data/databases/" + data.databaseFilename;
    try {
      if ( new File( targetDbFolder ).exists() ) {
        log.logBasic( "Removing exsting folder: " + targetDbFolder );
        FileUtils.deleteDirectory( new File( targetDbFolder ) );
      }
    } catch ( Exception e ) {
      throw new HopException( "Unable to remove old database files from '" + targetDbFolder + "'", e );
    }

    List<String> arguments = new ArrayList<>();

    arguments.add( data.adminCommand );
    arguments.add( "--database=" + data.databaseFilename );
    arguments.add( "--into=data/databases/"+data.databaseFilename);
    arguments.add( "--id-type=STRING" );
    for ( String nodesFile : data.nodesFiles ) {
      arguments.add( "--nodes=" + nodesFile );
    }
    for ( String relsFile : data.relsFiles) {
      arguments.add( "--relationships=" + relsFile );
    }
    arguments.add( "--report-file=" + data.reportFile );
    arguments.add( "--high-io=" + ( meta.isHighIo() ? "true" : "false" ) );
    arguments.add( "--ignore-extra-columns=" + ( meta.isIgnoringExtraColumns() ? "true" : "false" ) );
    arguments.add( "--ignore-duplicate-nodes=" + ( meta.isIgnoringDuplicateNodes() ? "true" : "false" ) );
    arguments.add( "--ignore-missing-nodes=" + ( meta.isIgnoringMissingNodes() ? "true" : "false" ) );
    arguments.add( "--skip-bad-relationships=" + ( meta.isSkippingBadRelationships() ? "true" : "false" ) );
    arguments.add( "--multiline-fields=" + ( meta.isMultiLine() ? "true" : "false" ) );
    if (StringUtils.isNotEmpty( data.readBufferSize )) {
      arguments.add("--read-buffer-size="+data.readBufferSize);
    }
    if (StringUtils.isNotEmpty( data.maxMemory)) {
      arguments.add("--max-memory="+data.maxMemory);
    }

    StringBuffer command = new StringBuffer();
    for ( String argument : arguments ) {
      command.append( argument ).append( " " );
    }
    log.logBasic( "Running command : " + command );
    log.logBasic( "Running from base folder: " + data.baseFolder );

    ProcessBuilder pb = new ProcessBuilder( arguments );
    pb.directory( new File( data.baseFolder ) );
    try {
      Process process = pb.start();

      StreamConsumer errorConsumer = new StreamConsumer( getLogChannel(), process.getErrorStream(), LogLevel.ERROR );
      errorConsumer.start();
      StreamConsumer outputConsumer = new StreamConsumer( getLogChannel(), process.getInputStream(), LogLevel.BASIC );
      outputConsumer.start();

      boolean exited = process.waitFor( 10, TimeUnit.MILLISECONDS );
      while ( !exited && !isStopped() ) {
        exited = process.waitFor( 10, TimeUnit.MILLISECONDS );
      }
      if ( !exited && isStopped() ) {
        process.destroyForcibly();
      }
    } catch ( Exception e ) {
      throw new HopException( "Error running command: " + arguments, e );
    }
  }
}
