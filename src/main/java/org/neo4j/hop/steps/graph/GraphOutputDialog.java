package org.neo4j.hop.steps.graph;

import org.apache.commons.lang.StringUtils;
import org.apache.hop.core.Const;
import org.apache.hop.core.SourceToTargetMapping;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopTransformException;
import org.apache.hop.core.logging.LogChannel;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.metastore.persist.MetaStoreFactory;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransformMeta;
import org.apache.hop.pipeline.transform.ITransformDialog;
import org.apache.hop.ui.core.dialog.EnterMappingDialog;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.widget.ColumnInfo;
import org.apache.hop.ui.core.widget.MetaSelectionLine;
import org.apache.hop.ui.core.widget.TableView;
import org.apache.hop.ui.core.widget.TextVar;
import org.apache.hop.ui.hopgui.HopGui;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.neo4j.hop.model.GraphModel;
import org.neo4j.hop.model.GraphModelUtils;
import org.neo4j.hop.model.GraphNode;
import org.neo4j.hop.model.GraphProperty;
import org.neo4j.hop.model.GraphRelationship;
import org.neo4j.hop.shared.NeoConnection;
import org.neo4j.hop.shared.NeoConnectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GraphOutputDialog extends BaseTransformDialog implements ITransformDialog {

  private static Class<?> PKG = GraphOutputMeta.class; // for i18n purposes, needed by Translator2!!

  private Text wTransformName;

  private MetaSelectionLine<NeoConnection> wConnection;
  private MetaSelectionLine<GraphModel> wModel;

  private Label wlBatchSize;
  private TextVar wBatchSize;
  private Label wlCreateIndexes;
  private Button wCreateIndexes;
  private Button wReturnGraph;
  private Label wlReturnGraphField;
  private TextVar wReturnGraphField;

  private TableView wFieldMappings;


  private GraphOutputMeta input;

  private GraphModel activeModel;

  public GraphOutputDialog( Shell parent, Object inputMetadata, PipelineMeta pipelineMeta, String transformName ) {
    super( parent, (BaseTransformMeta) inputMetadata, pipelineMeta, transformName );
    input = (GraphOutputMeta) inputMetadata;

    // Hack the metastore...
    //
    metaStore = HopGui.getInstance().getMetaStore();
  }

  @Override
  public String open() {
    Shell parent = getParent();
    Display display = parent.getDisplay();

    shell = new Shell( parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN );
    props.setLook( shell );
    setShellImage( shell, input );

    ModifyListener lsMod = e -> input.setChanged();
    changed = input.hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout( formLayout );
    shell.setText( "Neo4j GraphOutput" );

    int middle = props.getMiddlePct();
    int margin = Const.MARGIN;

    // Step name line
    //
    Label wlTransformName = new Label( shell, SWT.RIGHT );
    wlTransformName.setText( "Transform name" );
    props.setLook( wlTransformName );
    fdlTransformName = new FormData();
    fdlTransformName.left = new FormAttachment( 0, 0 );
    fdlTransformName.right = new FormAttachment( middle, -margin );
    fdlTransformName.top = new FormAttachment( 0, margin );
    wlTransformName.setLayoutData( fdlTransformName );
    wTransformName = new Text( shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wTransformName );
    wTransformName.addModifyListener( lsMod );
    fdTransformName = new FormData();
    fdTransformName.left = new FormAttachment( middle, 0 );
    fdTransformName.top = new FormAttachment( wlTransformName, 0, SWT.CENTER );
    fdTransformName.right = new FormAttachment( 100, 0 );
    wTransformName.setLayoutData( fdTransformName );
    Control lastControl = wTransformName;

    wConnection = new MetaSelectionLine<>( pipelineMeta, metaStore, NeoConnection.class, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER, "Neo4j Connection", "The name of the Neo4j connection to use" );
    props.setLook( wConnection );
    wConnection.addModifyListener( lsMod );
    FormData fdConnection = new FormData();
    fdConnection.left = new FormAttachment( 0, 0 );
    fdConnection.right = new FormAttachment( 100, 0 );
    fdConnection.top = new FormAttachment( lastControl, margin );
    wConnection.setLayoutData( fdConnection );
    try {
      wConnection.fillItems();
    } catch ( Exception e ) {
      new ErrorDialog( shell, "Error", "Error getting list of connections", e );
    }


    wModel = new MetaSelectionLine<>( pipelineMeta, metaStore, GraphModel.class, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER, "Graph model", "The name of the Neo4j logical Graph Model to use" );
    props.setLook( wModel );
    wModel.addModifyListener( lsMod );
    FormData fdModel = new FormData();
    fdModel.left = new FormAttachment( 0, 0 );
    fdModel.right = new FormAttachment( 100, 0 );
    fdModel.top = new FormAttachment( lastControl, 0, SWT.CENTER );
    wModel.setLayoutData( fdModel );
    try {
      wModel.fillItems();
    } catch ( Exception e ) {
      new ErrorDialog( shell, "Error", "Error getting list of models", e );
    }

    wlBatchSize = new Label( shell, SWT.RIGHT );
    wlBatchSize.setText( "Batch size (rows)" );
    props.setLook( wlBatchSize );
    FormData fdlBatchSize = new FormData();
    fdlBatchSize.left = new FormAttachment( 0, 0 );
    fdlBatchSize.right = new FormAttachment( middle, -margin );
    fdlBatchSize.top = new FormAttachment( lastControl, 2 * margin );
    wlBatchSize.setLayoutData( fdlBatchSize );
    wBatchSize = new TextVar( pipelineMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wBatchSize );
    wBatchSize.addModifyListener( lsMod );
    FormData fdBatchSize = new FormData();
    fdBatchSize.left = new FormAttachment( middle, 0 );
    fdBatchSize.right = new FormAttachment( 100, 0 );
    fdBatchSize.top = new FormAttachment( wlBatchSize, 0, SWT.CENTER );
    wBatchSize.setLayoutData( fdBatchSize );
    lastControl = wBatchSize;

    wlCreateIndexes = new Label( shell, SWT.RIGHT );
    wlCreateIndexes.setText( "Create indexes? " );
    wlCreateIndexes.setToolTipText( "Create index on first row using label field and primary key properties." );
    props.setLook( wlCreateIndexes );
    FormData fdlCreateIndexes = new FormData();
    fdlCreateIndexes.left = new FormAttachment( 0, 0 );
    fdlCreateIndexes.right = new FormAttachment( middle, -margin );
    fdlCreateIndexes.top = new FormAttachment( lastControl, 2 * margin );
    wlCreateIndexes.setLayoutData( fdlCreateIndexes );
    wCreateIndexes = new Button( shell, SWT.CHECK | SWT.BORDER );
    wCreateIndexes.setToolTipText( "Create index on first row using label field and primary key properties." );
    props.setLook( wCreateIndexes );
    FormData fdCreateIndexes = new FormData();
    fdCreateIndexes.left = new FormAttachment( middle, 0 );
    fdCreateIndexes.right = new FormAttachment( 100, 0 );
    fdCreateIndexes.top = new FormAttachment( wlCreateIndexes, 0, SWT.CENTER );
    wCreateIndexes.setLayoutData( fdCreateIndexes );
    lastControl = wCreateIndexes;

    Label wlReturnGraph = new Label( shell, SWT.RIGHT );
    wlReturnGraph.setText( "Return graph data?" );
    String returnGraphTooltipText = "The update data to be updated in the form of Graph a value in the output of this step";
    wlReturnGraph.setToolTipText( returnGraphTooltipText );
    props.setLook( wlReturnGraph );
    FormData fdlReturnGraph = new FormData();
    fdlReturnGraph.left = new FormAttachment( 0, 0 );
    fdlReturnGraph.right = new FormAttachment( middle, -margin );
    fdlReturnGraph.top = new FormAttachment( lastControl, 2 * margin );
    wlReturnGraph.setLayoutData( fdlReturnGraph );
    wReturnGraph = new Button( shell, SWT.CHECK | SWT.BORDER );
    wReturnGraph.setToolTipText( returnGraphTooltipText );
    props.setLook( wReturnGraph );
    FormData fdReturnGraph = new FormData();
    fdReturnGraph.left = new FormAttachment( middle, 0 );
    fdReturnGraph.right = new FormAttachment( 100, 0 );
    fdReturnGraph.top = new FormAttachment( wlReturnGraph, 0, SWT.CENTER );
    wReturnGraph.setLayoutData( fdReturnGraph );
    wReturnGraph.addListener( SWT.Selection, e -> enableFields() );
    lastControl = wReturnGraph;

    wlReturnGraphField = new Label( shell, SWT.RIGHT );
    wlReturnGraphField.setText( "Graph output field name" );
    props.setLook( wlReturnGraphField );
    FormData fdlReturnGraphField = new FormData();
    fdlReturnGraphField.left = new FormAttachment( 0, 0 );
    fdlReturnGraphField.right = new FormAttachment( middle, -margin );
    fdlReturnGraphField.top = new FormAttachment( lastControl, 2 * margin );
    wlReturnGraphField.setLayoutData( fdlReturnGraphField );
    wReturnGraphField = new TextVar( pipelineMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wReturnGraphField );
    wReturnGraphField.addModifyListener( lsMod );
    FormData fdReturnGraphField = new FormData();
    fdReturnGraphField.left = new FormAttachment( middle, 0 );
    fdReturnGraphField.right = new FormAttachment( 100, 0 );
    fdReturnGraphField.top = new FormAttachment( wlReturnGraphField, 0, SWT.CENTER );
    wReturnGraphField.setLayoutData( fdReturnGraphField );
    lastControl = wReturnGraphField;

    // Some buttons at the bottom...
    //
    wOK = new Button( shell, SWT.PUSH );
    wOK.setText( BaseMessages.getString( PKG, "System.Button.OK" ) );
    Button wMapping = new Button( shell, SWT.PUSH );
    wMapping.setText( "Map fields" );
    wCancel = new Button( shell, SWT.PUSH );
    wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) );

    // Position the buttons at the bottom of the dialog.
    //
    setButtonPositions( new Button[] { wOK, wMapping, wCancel }, margin, null );

    String[] fieldNames;
    try {
      fieldNames = pipelineMeta.getPrevTransformFields( transformName ).getFieldNames();
    } catch ( Exception e ) {
      logError( "Unable to get fields from previous steps", e );
      fieldNames = new String[] {};
    }

    // Table: field to model mapping
    //
    ColumnInfo[] parameterColumns =
      new ColumnInfo[] {
        new ColumnInfo( "Field", ColumnInfo.COLUMN_TYPE_CCOMBO, fieldNames, false ),
        new ColumnInfo( "Target type", ColumnInfo.COLUMN_TYPE_CCOMBO, ModelTargetType.getNames(), false ),
        new ColumnInfo( "Target", ColumnInfo.COLUMN_TYPE_CCOMBO, new String[ 0 ], false ),
        new ColumnInfo( "Property", ColumnInfo.COLUMN_TYPE_CCOMBO, new String[ 0 ], false ),
      };

    Label wlFieldMappings = new Label( shell, SWT.LEFT );
    wlFieldMappings.setText( "Mappings..." );
    props.setLook( wlFieldMappings );
    FormData fdlFieldMappings = new FormData();
    fdlFieldMappings.left = new FormAttachment( 0, 0 );
    fdlFieldMappings.right = new FormAttachment( middle, -margin );
    fdlFieldMappings.top = new FormAttachment( lastControl, margin );
    wlFieldMappings.setLayoutData( fdlFieldMappings );
    wFieldMappings = new TableView( pipelineMeta, shell, SWT.FULL_SELECTION | SWT.MULTI, parameterColumns, input.getFieldModelMappings().size(), lsMod, props );
    props.setLook( wFieldMappings );
    wFieldMappings.addModifyListener( lsMod );
    FormData fdFieldMappings = new FormData();
    fdFieldMappings.left = new FormAttachment( 0, 0 );
    fdFieldMappings.right = new FormAttachment( 100, 0 );
    fdFieldMappings.top = new FormAttachment( wlFieldMappings, margin );
    fdFieldMappings.bottom = new FormAttachment( wOK, -margin * 2 );
    wFieldMappings.setLayoutData( fdFieldMappings );
    lastControl = wFieldMappings;

    // Add listeners
    lsCancel = e -> cancel();
    lsOK = e -> ok();

    wCancel.addListener( SWT.Selection, lsCancel );
    wOK.addListener( SWT.Selection, lsOK );
    wMapping.addListener( SWT.Selection, ( e ) -> enterMapping() );

    lsDef = new SelectionAdapter() {
      public void widgetDefaultSelected( SelectionEvent e ) {
        ok();
      }
    };

    wConnection.addSelectionListener( lsDef );
    wTransformName.addSelectionListener( lsDef );
    wBatchSize.addSelectionListener( lsDef );
    wReturnGraph.addSelectionListener( lsDef );


    // Detect X or ALT-F4 or something that kills this window...
    shell.addShellListener( new ShellAdapter() {
      public void shellClosed( ShellEvent e ) {
        cancel();
      }
    } );

    // Set the shell size, based upon previous time...
    setSize();

    getData();
    input.setChanged( changed );

    shell.open();
    while ( !shell.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }
    return transformName;

  }

  private void enableFields() {

    boolean toNeo = !wReturnGraph.getSelection();

    wConnection.setEnabled( toNeo );
    wlBatchSize.setEnabled( toNeo );
    wBatchSize.setEnabled( toNeo );
    wlCreateIndexes.setEnabled( toNeo );
    wCreateIndexes.setEnabled( toNeo );

    wlReturnGraphField.setEnabled( !toNeo );
    wReturnGraphField.setEnabled( !toNeo );
  }

  private void enterMapping() {
    // Map input field names to Node/Property values
    //
    try {
      MetaStoreFactory<GraphModel> modelFactory = GraphModelUtils.getModelFactory( metaStore );

      if ( activeModel == null ) {
        if ( StringUtils.isEmpty( wModel.getText() ) ) {
          return;
        }
        activeModel = modelFactory.loadElement( wModel.getText() );
      }

      // Input fields
      //
      IRowMeta inputRowMeta = pipelineMeta.getPrevTransformFields( transformMeta );
      String[] inputFields = inputRowMeta.getFieldNames();

      // Node properties
      //
      String separator = " . ";
      List<String> targetPropertiesList = new ArrayList<>();
      for ( GraphNode node : activeModel.getNodes() ) {
        for ( GraphProperty property : node.getProperties() ) {
          String combo = node.getName() + " . " + property.getName();
          targetPropertiesList.add( combo );
        }
      }
      for ( GraphRelationship relationship : activeModel.getRelationships() ) {
        for ( GraphProperty property : relationship.getProperties() ) {
          String combo = relationship.getName() + " . " + property.getName();
          targetPropertiesList.add( combo );
        }
      }

      String[] targetProperties = targetPropertiesList.toArray( new String[ 0 ] );

      // Preserve mappings...
      //
      List<SourceToTargetMapping> mappings = new ArrayList<>();
      for ( int i = 0; i < wFieldMappings.nrNonEmpty(); i++ ) {
        TableItem item = wFieldMappings.getNonEmpty( i );
        int sourceIndex = Const.indexOfString( item.getText( 1 ), inputFields );
        int targetIndex = Const.indexOfString( item.getText( 3 ) + separator + item.getText( 4 ), targetProperties );
        if ( sourceIndex >= 0 && targetIndex >= 0 ) {
          mappings.add( new SourceToTargetMapping( sourceIndex, targetIndex ) );
        }
      }

      EnterMappingDialog dialog = new EnterMappingDialog( shell, inputFields, targetProperties, mappings );
      mappings = dialog.open();
      if ( mappings != null ) {
        wFieldMappings.clearAll();
        for ( SourceToTargetMapping mapping : mappings ) {
          String field = mapping.getSourceString( inputFields );
          String target = mapping.getTargetString( targetProperties );
          int index = target.indexOf( separator );
          String targetName = target.substring( 0, index );
          String property = target.substring( index + separator.length() );

          String targetType = null;
          if ( activeModel.findNode( targetName ) != null ) {
            targetType = "Node";
          } else if ( activeModel.findRelationship( targetName ) != null ) {
            targetType = "Relationship";
          } else {
            throw new HopException( "Neither node nor transformation found for target '" + targetName + ": internal error" );
          }

          wFieldMappings.add( field, targetType, targetName, property );
        }
        wFieldMappings.removeEmptyRows();
        wFieldMappings.setRowNums();
        wFieldMappings.optWidth( true );
      }


    } catch ( Exception e ) {
      new ErrorDialog( shell, "Error", "Error mapping input fields to node properties", e );
    }

  }

  private void cancel() {
    transformName = null;
    input.setChanged( changed );
    dispose();
  }

  public void getData() {

    wTransformName.setText( Const.NVL( transformName, "" ) );
    wConnection.setText( Const.NVL( input.getConnectionName(), "" ) );
    updateConnectionsCombo();

    wModel.setText( Const.NVL( input.getModel(), "" ) );
    updateModelsCombo();

    wBatchSize.setText( Const.NVL( input.getBatchSize(), "" ) );
    wCreateIndexes.setSelection( input.isCreatingIndexes() );

    for ( int i = 0; i < input.getFieldModelMappings().size(); i++ ) {
      FieldModelMapping mapping = input.getFieldModelMappings().get( i );
      TableItem item = wFieldMappings.table.getItem( i );
      int idx = 1;
      item.setText( idx++, Const.NVL( mapping.getField(), "" ) );
      item.setText( idx++, ModelTargetType.getCode( mapping.getTargetType() ) );
      item.setText( idx++, Const.NVL( mapping.getTargetName(), "" ) );
      item.setText( idx++, Const.NVL( mapping.getTargetProperty(), "" ) );
    }
    wFieldMappings.removeEmptyRows();
    wFieldMappings.setRowNums();
    wFieldMappings.optWidth( true );

    wReturnGraph.setSelection( input.isReturningGraph() );
    wReturnGraphField.setText( Const.NVL( input.getReturnGraphField(), "" ) );

    enableFields();
  }

  private void updateModelsCombo() {
    // List of models...
    //
    try {
      MetaStoreFactory<GraphModel> modelFactory = GraphModelUtils.getModelFactory( metaStore );
      List<String> modelNames = modelFactory.getElementNames();
      Collections.sort( modelNames );
      wModel.setItems( modelNames.toArray( new String[ 0 ] ) );

      // Importer the active model...
      //
      if ( StringUtils.isNotEmpty( wModel.getText() ) ) {
        activeModel = modelFactory.loadElement( wModel.getText() );
        if ( activeModel != null ) {
          // Set combo boxes in the mappings...
          //
          List<String> targetNames = new ArrayList<>();
          targetNames.addAll( Arrays.asList( activeModel.getNodeNames() ) );
          targetNames.addAll( Arrays.asList( activeModel.getRelationshipNames() ) );

          wFieldMappings.getColumns()[ 2 ].setComboValues( activeModel.getNodeNames() );
        }
      } else {
        activeModel = null;
      }

    } catch ( Exception e ) {
      new ErrorDialog( shell, "Error", "Unable to list Neo4j Graph Models", e );
    }
  }

  private void updateConnectionsCombo() {
    // List of connections...
    //
    try {
      wConnection.fillItems();
    } catch ( Exception e ) {
      new ErrorDialog( shell, "Error", "Unable to list Neo4j connections", e );
    }
  }

  private void ok() {
    if ( StringUtils.isEmpty( wTransformName.getText() ) ) {
      return;
    }

    transformName = wTransformName.getText(); // return value
    input.setConnectionName( wConnection.getText() );
    input.setBatchSize( wBatchSize.getText() );
    input.setCreatingIndexes( wCreateIndexes.getSelection() );
    input.setModel( wModel.getText() );

    input.setReturningGraph( wReturnGraph.getSelection() );
    input.setReturnGraphField( wReturnGraphField.getText() );

    List<FieldModelMapping> mappings = new ArrayList<>();
    for ( int i = 0; i < wFieldMappings.nrNonEmpty(); i++ ) {
      TableItem item = wFieldMappings.getNonEmpty( i );
      int idx = 1;
      String sourceField = item.getText( idx++ );
      ModelTargetType targetType = ModelTargetType.parseCode( item.getText( idx++ ) );
      String targetName = item.getText( idx++ );
      String targetProperty = item.getText( idx++ );

      mappings.add( new FieldModelMapping( sourceField, targetType, targetName, targetProperty ) );
    }
    input.setFieldModelMappings( mappings );


    dispose();
  }


  private IRowMeta getInputRowMeta() {
    IRowMeta inputRowMeta = null;
    try {
      inputRowMeta = pipelineMeta.getPrevTransformFields( transformName );
    } catch ( HopTransformException e ) {
      LogChannel.GENERAL.logError( "Unable to find step input field", e );
    }
    return inputRowMeta;
  }
}
