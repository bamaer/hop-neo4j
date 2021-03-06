package org.neo4j.hop.transforms.split;

import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.pipeline.transform.BaseTransformData;
import org.apache.hop.pipeline.transform.ITransformData;

public class SplitGraphData extends BaseTransformData implements ITransformData {

  public IRowMeta outputRowMeta;
  public int graphFieldIndex;
  public String typeField;
  public String idField;
  public String propertySetField;
}
