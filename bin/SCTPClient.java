import com.sun.nio.sctp.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

// This object is an SCTPClient used to connect to SCTPServer.
class SCTPClient implements Runnable
{
	// Size of ByteBuffer to accept incoming messages
	private int MAX_MSG_SIZE = 4096;
	// Holds address of server
	InetSocketAddress addr;
	// Synchronizer
	SCTPClientServer cs;
	// Number of rounds for a node to send/receive messages.
	int numOfRounds = 0;

	// Holds server node information
	Node serverInfo;
	// Holds client node information
	Node clientInfo;

	// SCTP channel
	SctpChannel sctpChannel = null;

	// Constructor - initialize port and hostname of server to connect to
	public SCTPClient(SCTPClientServer cs, Node serverInfo, Node clientInfo, int numOfRounds) throws Exception
	{
		this.serverInfo = serverInfo;
		this.clientInfo = clientInfo;
		this.cs = cs;
		this.numOfRounds = numOfRounds;
	}

	/*
		Method: run()
		Description: Connect client to server specified at address hostname, port.
		Parameters: None
		Returns: Nothing
	 */
	@Override
	 public void run()
	 {
	 	// Start at round 0.
	 	int roundNumber = 0;
		// Get address of server using name and port number.
		addr = new InetSocketAddress(serverInfo.hostName, serverInfo.listeningPort);

		// Try to connect to server

		// Loops until connection is made.
		boolean connected = false;

		while(!connected) {

			try {
				Thread.sleep(3000);

				// Open SCTP channel to connect to server using the address
				sctpChannel = SctpChannel.open(addr, 0, 0);
				System.out.println("CHANNEL: Client connected to server " + serverInfo.hostName + " of node " + serverInfo.nodeID);
				connected = true;
			} catch (Exception e) {
				System.out.println("Server is offline..Attempting to reconnect.");
				e.printStackTrace();

				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}

		try {

			// Initial message exchange between client and server:

			// Send message to give server the client node information.
			// MessageInfo for SCTP layer
			MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
			Message msg = new Message("Sending client info to server node " + serverInfo.nodeID,
					clientInfo.nodeID);
			// Messages are sent over SCTP using ByteBuffer
			sctpChannel.send(msg.toByteBuffer(), messageInfo);
			System.out.println("INITIAL MSG SENT: " + msg.message);

			// Buffer to hold ACK message from server.
			ByteBuffer buf = ByteBuffer.allocateDirect(MAX_MSG_SIZE);
			// Messages are received over SCTP from client using ByteBuffer
			sctpChannel.receive(buf, null, null);

			// For each round, the client on this channel will send a message and receive a message from the server.
			// Loops until the node's current round number equals the number of rounds it is supposed to go.
			while(cs.getCurrentRoundNumber() < numOfRounds)
			{
				// If the local client's round number equals the node's current round number.
				if(roundNumber == cs.getCurrentRoundNumber()) {
					// If the local client's round number has reached the max number of rounds, then stop looping.
					if(roundNumber == numOfRounds)
					{
						break;
					}

					// Send message to server.
					send();
					// Update synchronizer that message was sent to the server node.
					cs.messageSent(serverInfo.nodeID);

					// Receive message from server.
					Message receivedMessage = receive();
					System.out.println("MSG RECEIVED: " + receivedMessage.message);

					// Update synchronizer that message was received from the server node.
					cs.messageReceived(serverInfo.nodeID, receivedMessage);

					// Increment local client's round number - client sends and receives one message with server per round.
					roundNumber++;
				}

			}

		}
		catch (Exception e) {
			e.printStackTrace();

		}
	}

	/*
		Method: send
		Description: Sends message to the server node.
		Parameters: None
		Returns: Nothing
	 */
	public void send()
	{
		try {
			// MessageInfo for SCTP layer.
			MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
			// Message to send to server. Message includes the client node's current round number, the client node ID,
			// server node ID, and the k-hop neighbors of the client node.
			Message msg = new Message("Message from node " + clientInfo.nodeID + " at round " +
					cs.getCurrentRoundNumber() + " to dest node " + serverInfo.nodeID,
					cs.getCurrentRoundNumber(), clientInfo.nodeID, serverInfo.nodeID, cs.getKHopNeighbors());
			// Messages are sent over SCTP using ByteBuffer
			sctpChannel.send(msg.toByteBuffer(), messageInfo);
			System.out.println("MSG SENT: " + msg.message);
		}
		catch(Exception e)
		{
			System.out.println("Error in sending message.");
			e.printStackTrace();
		}
	}

	/*
		Method: receive
		Description: Receives message from server node.
		Parameters: None
		Returns: Received message sent by server node.
	 */
	public Message receive()
	{
		try {
			// Buffer to hold message from client.
			ByteBuffer buf = ByteBuffer.allocateDirect(MAX_MSG_SIZE);
			// Messages are received over SCTP from client using ByteBuffer
			sctpChannel.receive(buf, null, null);

			// Return the message received.
			return Message.fromByteBuffer(buf);
		}
		catch(Exception e)
		{
			System.out.println("Error in receiving message.");
			e.printStackTrace();
		}

		return null;
	}
}

