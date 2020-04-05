package org.neo4j.hop.steps.gencsv;

import org.apache.commons.lang.StringUtils;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.pipeline.transform.BaseTransformData;
import org.apache.hop.pipeline.transform.ITransformData;

import java.util.Map;

public class GenerateCsvData extends BaseTransformData implements ITransformData {

  public IRowMeta outputRowMeta;
  public String importFolder;
  public int graphFieldIndex;
  public IndexedGraphData indexedGraphData;
  public String baseFolder;

  public String filesPrefix;
  public String filenameField;
  public String fileTypeField;

  public Map<String, CsvFile> fileMap;

  public static String getPropertySetKey( String sourceTransformation, String sourceStep, String propertySetId ) {
    StringBuilder key = new StringBuilder();
    if ( StringUtils.isNotEmpty( sourceTransformation ) ) {
      key.append( sourceTransformation );
    }
    if ( StringUtils.isNotEmpty( sourceStep ) ) {
      if ( key.length() > 0 ) {
        key.append( "-" );
      }
      key.append( sourceStep );
    }
    if ( StringUtils.isNotEmpty( propertySetId ) ) {
      if ( key.length() > 0 ) {
        key.append( "-" );
      }
      key.append( propertySetId );
    }

    // Replace troublesome characters from the key for filename purposes
    //
    String setKey = key.toString();
    setKey = setKey.replace( "*", "" );
    setKey = setKey.replace( ":", "" );
    setKey = setKey.replace( ";", "" );
    setKey = setKey.replace( "[", "" );
    setKey = setKey.replace( "]", "" );
    setKey = setKey.replace( "$", "" );
    setKey = setKey.replace( "/", "" );
    setKey = setKey.replace( "{", "" );
    setKey = setKey.replace( "}", "" );

    return setKey;
  }
}
