package org.neo4j.hop.steps.graph;


import org.apache.hop.core.annotations.Transform;
import org.neo4j.hop.core.value.ValueMetaGraph;
import org.apache.hop.core.Const;
import org.apache.hop.core.annotations.Transform;
import org.apache.hop.core.database.DatabaseMeta;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopXMLException;
import org.apache.hop.core.injection.Injection;
import org.apache.hop.core.injection.InjectionDeep;
import org.apache.hop.core.injection.InjectionSupported;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.xml.XMLHandler;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransformMeta;
import org.apache.hop.pipeline.transform.ITransformData;
import org.apache.hop.pipeline.transform.ITransform;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.hop.pipeline.transform.ITransformMeta;
import org.apache.hop.metastore.api.IMetaStore;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;


@Transform(
  id = "Neo4jGraphOutput",
  name = "Neo4j Graph Output",
  description = "Write to a Neo4j graph using an input field mapping",
  image = "neo4j_graph_output.svg",
  categoryDescription = "Neo4j",
  documentationUrl = "https://github.com/knowbi/knowbi-pentaho-pdi-neo4j-output/wiki/Neo4j-Graph-Output#description"
)
@InjectionSupported( localizationPrefix = "GraphOutput.Injection.", groups = { "MAPPINGS", } )
public class GraphOutputMeta extends BaseTransformMeta implements ITransformMeta<GraphOutput, GraphOutputData> {

  private static final String RETURNING_GRAPH = "returning_graph";
  private static final String RETURN_GRAPH_FIELD = "return_graph_field";
  public static final String CONNECTION = "connection";
  public static final String MODEL = "model";
  public static final String BATCH_SIZE = "batch_size";
  public static final String CREATE_INDEXES = "create_indexes";
  public static final String MAPPINGS = "mappings";
  public static final String MAPPING = "mapping";
  public static final String SOURCE_FIELD = "source_field";
  public static final String TARGET_TYPE = "target_type";
  public static final String TARGET_NAME = "target_name";
  public static final String TARGET_PROPERTY = "target_property";

  @Injection( name = CONNECTION )
  private String connectionName;

  @Injection( name = MODEL )
  private String model;

  @Injection( name = BATCH_SIZE )
  private String batchSize;

  @Injection( name = CREATE_INDEXES )
  private boolean creatingIndexes;

  @Injection( name = RETURNING_GRAPH )
  private boolean returningGraph;

  @Injection( name = RETURN_GRAPH_FIELD )
  private String returnGraphField;

  @InjectionDeep
  private List<FieldModelMapping> fieldModelMappings;


  public GraphOutputMeta() {
    super();
    fieldModelMappings = new ArrayList<>();
    creatingIndexes = true;
  }

  @Override public void setDefault() {

  }

  @Override public GraphOutput createTransform( TransformMeta transformMeta, GraphOutputData iTransformData, int i, PipelineMeta pipelineMeta, Pipeline pipeline ) {
    return new GraphOutput( transformMeta, this, iTransformData, i, pipelineMeta, pipeline );
  }

  @Override public GraphOutputData getTransformData() {
    return new GraphOutputData();
  }

  @Override public String getDialogClassName() {
    return GraphOutputDialog.class.getName();
  }

  @Override public void getFields( IRowMeta rowMeta, String name, IRowMeta[] info, TransformMeta nextStep, IVariables space,
                                   IMetaStore metaStore ) {

    if (returningGraph) {

      IValueMeta valueMetaGraph = new ValueMetaGraph( Const.NVL(returnGraphField, "graph") );
      valueMetaGraph.setOrigin( name );
      rowMeta.addValueMeta( valueMetaGraph );

    }
  }

  @Override public String getXML() {
    StringBuilder xml = new StringBuilder();
    xml.append( XMLHandler.addTagValue( CONNECTION, connectionName ) );
    xml.append( XMLHandler.addTagValue( MODEL, model ) );
    xml.append( XMLHandler.addTagValue( BATCH_SIZE, batchSize ) );
    xml.append( XMLHandler.addTagValue( CREATE_INDEXES, creatingIndexes ) );
    xml.append( XMLHandler.addTagValue( RETURNING_GRAPH, returningGraph ) );
    xml.append( XMLHandler.addTagValue( RETURN_GRAPH_FIELD, returnGraphField ) );

    xml.append( XMLHandler.openTag( MAPPINGS ) );
    for ( FieldModelMapping fieldModelMapping : fieldModelMappings ) {
      xml.append( XMLHandler.openTag( MAPPING ) );
      xml.append( XMLHandler.addTagValue( SOURCE_FIELD, fieldModelMapping.getField() ) );
      xml.append( XMLHandler.addTagValue( TARGET_TYPE, ModelTargetType.getCode( fieldModelMapping.getTargetType() ) ) );
      xml.append( XMLHandler.addTagValue( TARGET_NAME, fieldModelMapping.getTargetName() ) );
      xml.append( XMLHandler.addTagValue( TARGET_PROPERTY, fieldModelMapping.getTargetProperty() ) );
      xml.append( XMLHandler.closeTag( MAPPING ) );
    }
    xml.append( XMLHandler.closeTag( MAPPINGS ) );

    return xml.toString();
  }

  @Override public void loadXML( Node stepnode, IMetaStore metaStore ) throws HopXMLException {
    connectionName = XMLHandler.getTagValue( stepnode, CONNECTION );
    model = XMLHandler.getTagValue( stepnode, MODEL );
    batchSize = XMLHandler.getTagValue( stepnode, BATCH_SIZE );
    creatingIndexes = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, CREATE_INDEXES ) );
    returningGraph = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, RETURNING_GRAPH ) );
    returnGraphField = XMLHandler.getTagValue( stepnode, RETURN_GRAPH_FIELD );

    // Parse parameter mappings
    //
    Node mappingsNode = XMLHandler.getSubNode( stepnode, MAPPINGS );
    List<Node> mappingNodes = XMLHandler.getNodes( mappingsNode, MAPPING );
    fieldModelMappings = new ArrayList<>();
    for ( Node mappingNode : mappingNodes ) {
      String field = XMLHandler.getTagValue( mappingNode, SOURCE_FIELD );
      ModelTargetType targetType = ModelTargetType.parseCode( XMLHandler.getTagValue( mappingNode, TARGET_TYPE ) );
      String targetName = XMLHandler.getTagValue( mappingNode, TARGET_NAME );
      String targetProperty = XMLHandler.getTagValue( mappingNode, TARGET_PROPERTY );

      fieldModelMappings.add( new FieldModelMapping( field, targetType, targetName, targetProperty ) );
    }

    super.loadXML( stepnode, metaStore );
  }

  /**
   * Gets connectionName
   *
   * @return value of connectionName
   */
  public String getConnectionName() {
    return connectionName;
  }

  /**
   * @param connectionName The connectionName to set
   */
  public void setConnectionName( String connectionName ) {
    this.connectionName = connectionName;
  }

  /**
   * Gets model
   *
   * @return value of model
   */
  public String getModel() {
    return model;
  }

  /**
   * @param model The model to set
   */
  public void setModel( String model ) {
    this.model = model;
  }

  /**
   * Gets batchSize
   *
   * @return value of batchSize
   */
  public String getBatchSize() {
    return batchSize;
  }

  /**
   * @param batchSize The batchSize to set
   */
  public void setBatchSize( String batchSize ) {
    this.batchSize = batchSize;
  }

  /**
   * Gets creatingIndexes
   *
   * @return value of creatingIndexes
   */
  public boolean isCreatingIndexes() {
    return creatingIndexes;
  }

  /**
   * @param creatingIndexes The creatingIndexes to set
   */
  public void setCreatingIndexes( boolean creatingIndexes ) {
    this.creatingIndexes = creatingIndexes;
  }

  /**
   * Gets fieldModelMappings
   *
   * @return value of fieldModelMappings
   */
  public List<FieldModelMapping> getFieldModelMappings() {
    return fieldModelMappings;
  }

  /**
   * @param fieldModelMappings The fieldModelMappings to set
   */
  public void setFieldModelMappings( List<FieldModelMapping> fieldModelMappings ) {
    this.fieldModelMappings = fieldModelMappings;
  }

  /**
   * Gets returningGraph
   *
   * @return value of returningGraph
   */
  public boolean isReturningGraph() {
    return returningGraph;
  }

  /**
   * @param returningGraph The returningGraph to set
   */
  public void setReturningGraph( boolean returningGraph ) {
    this.returningGraph = returningGraph;
  }

  /**
   * Gets returnGraphField
   *
   * @return value of returnGraphField
   */
  public String getReturnGraphField() {
    return returnGraphField;
  }

  /**
   * @param returnGraphField The returnGraphField to set
   */
  public void setReturnGraphField( String returnGraphField ) {
    this.returnGraphField = returnGraphField;
  }
}
