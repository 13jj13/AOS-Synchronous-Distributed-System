import java.util.LinkedList;

// This class holds all the information for a node/process.
public class Node {

    // Holds nodeID for the node.
    int nodeID;
    // Holds hostname for the node.
    String hostName;
    // Holds listening port for the node.
    int listeningPort;
    // Holds a list of the neighbor IDs for the node.
    LinkedList<Integer> neighbors;

    // Constructor - initialize all values.
    public Node(int nodeID, String hostName, int listeningPort)
    {
        // Initialize the nodeID, hostname, and listening port for the node.
        this.nodeID = nodeID;
        this.hostName = hostName;
        this.listeningPort = listeningPort;
    }

    /*
        Method: toString
        Description: Gives the node ID as the string version of a Node object.
        Parameters: None
        Returns: String NodeID of the node
     */
    @Override
    public String toString()
    {
        return "" + nodeID;
    }

    /*
        Method: addNeighbors
        Description: Provides the list of node IDs of the neighbors for this node.
        Parameters: LinkedList of neighbor node IDs (integers)
        Returns: Nothing
     */
    public void addNeighbors(LinkedList<Integer> neighbors)
    {
        this.neighbors = neighbors;
    }

}
