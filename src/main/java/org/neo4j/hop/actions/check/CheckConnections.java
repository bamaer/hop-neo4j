package org.neo4j.hop.actions.check;

import org.apache.hop.core.Result;
import org.apache.hop.core.annotations.Action;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.exception.HopXmlException;
import org.apache.hop.core.xml.XmlHandler;
import org.apache.hop.metadata.api.IHopMetadataProvider;
import org.apache.hop.metadata.api.IHopMetadataSerializer;
import org.apache.hop.workflow.action.ActionBase;
import org.apache.hop.workflow.action.IAction;
import org.neo4j.driver.Session;
import org.neo4j.hop.shared.NeoConnection;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

@Action(
  id = "NEO4J_CHECK_CONNECTIONS",
  name = "Check Neo4j Connections",
  description = "Check to see if we can connect to the listed Neo4j databases",
  image = "neo4j_check.svg",
  categoryDescription = "i18n:org.apache.hop.workflow:ActionCategory.Category.Conditions",
  documentationUrl = "https://github.com/mattcasters/hop-neo4j/"
)
public class CheckConnections extends ActionBase implements IAction {

  private List<String> connectionNames;

  public CheckConnections() {
    this.connectionNames = new ArrayList<>();
  }

  public CheckConnections( String name ) {
    this( name, "" );
  }

  public CheckConnections( String name, String description ) {
    super( name, description );
    connectionNames = new ArrayList<>();
  }

  @Override public String getXml() {
    StringBuilder xml = new StringBuilder();
    // Add action name, type, ...
    //
    xml.append( super.getXml() );

    xml.append( XmlHandler.openTag( "connections" ) );

    for ( String connectionName : connectionNames ) {
      xml.append( XmlHandler.addTagValue( "connection", connectionName ) );
    }

    xml.append( XmlHandler.closeTag( "connections" ) );
    return xml.toString();
  }

  @Override public void loadXml( Node node, IHopMetadataProvider metadataProvider ) throws HopXmlException {

    super.loadXml( node );

    connectionNames = new ArrayList<>();
    Node connectionsNode = XmlHandler.getSubNode( node, "connections" );
    List<Node> connectionNodes = XmlHandler.getNodes( connectionsNode, "connection" );
    for ( Node connectionNode : connectionNodes ) {
      String connectionName = XmlHandler.getNodeValue( connectionNode );
      connectionNames.add( connectionName );
    }
  }


  @Override public Result execute( Result result, int nr ) throws HopException {

    IHopMetadataSerializer<NeoConnection> serializer = metadataProvider.getSerializer( NeoConnection.class );

    // Replace variables & parameters
    //
    List<String> realConnectionNames = new ArrayList<>();
    for ( String connectionName : connectionNames ) {
      realConnectionNames.add( environmentSubstitute( connectionName ) );
    }

    // Check all the connections.  If any one fails, fail the transform
    // Check 'm all though, report on all, nr of errors is nr of failed connections
    //
    int testCount = 0;
    for ( String connectionName : realConnectionNames ) {
      testCount++;
      try {
        NeoConnection connection = serializer.load( connectionName );
        if ( connection == null ) {
          throw new HopException( "Unable to find connection with name '" + connectionName + "'" );
        }
        connection.initializeVariablesFrom( this );

        Session session = connection.getSession( log );
        session.close();

      } catch ( Exception e ) {
        // Something bad happened, log the error, flag error
        //
        result.increaseErrors( 1 );
        result.setResult( false );
        logError( "Error on connection: " + connectionName, e );
      }
    }

    if ( result.getNrErrors() == 0 ) {
      logBasic( testCount + " Neo4j connections tested without error" );
    } else {
      logBasic( testCount + " Neo4j connections tested with " + result.getNrErrors() + " error(s)" );
    }

    return result;
  }

  @Override public String getDialogClassName() {
    return super.getDialogClassName();
  }

  /**
   * Gets connectionNames
   *
   * @return value of connectionNames
   */
  public List<String> getConnectionNames() {
    return connectionNames;
  }

  /**
   * @param connectionNames The connectionNames to set
   */
  public void setConnectionNames( List<String> connectionNames ) {
    this.connectionNames = connectionNames;
  }

  @Override public boolean evaluates() {
    return true;
  }

  @Override public boolean isUnconditional() {
    return false;
  }
}
