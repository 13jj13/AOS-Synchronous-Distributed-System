import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

import java.nio.ByteBuffer;

// ClientHandler class to manage SCTPServer connection to a SCTPClient.
class SCTPClientHandler implements Runnable
{
    // Size of ByteBuffer to accept incoming messages
    private int MAX_MSG_SIZE = 4096;

    // Synchronizer
    SCTPClientServer cs;

    // SCTP channel
    SctpChannel sctpChannel;
    // Holds client node ID
    int clientNodeID;
    // Holds server node information
    Node serverInfo;

    // Holds number of rounds for a node to send/receive messages
    int numOfRounds = 0;

    // Constructor
    public SCTPClientHandler(SCTPClientServer cs, SctpChannel sctpChannel, Node serverInfo, int numOfRounds)
    {
        this.cs = cs;
        this.sctpChannel = sctpChannel;
        this.serverInfo = serverInfo;
        this.numOfRounds = numOfRounds;
    }

    /*
		Method: run()
		Description: Send and receive messages with SCTP client.
		Parameters: None
		Returns: Nothing
	 */
    @Override
    public void run()
    {
        try {
            // Start at round 0
            int roundNumber = 0;

            // Handle initial message exchange between client and server:

            // Buffer to hold message from client.
            ByteBuffer buf = ByteBuffer.allocateDirect(MAX_MSG_SIZE);
            // Messages are received over SCTP from client using ByteBuffer
            sctpChannel.receive(buf, null, null);
            // Store client information.
            clientNodeID = Message.fromByteBuffer(buf).sourceNodeID;

            System.out.println("INITIAL MSG RECEIVED: Client node is " + clientNodeID);

            // Send back acknowledgement that initial message was received.
            // MessageInfo for SCTP layer
            MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
            Message msg = new Message("ACK");
            // Messages are sent over SCTP using ByteBuffer
            sctpChannel.send(msg.toByteBuffer(), messageInfo);

            // For each round, the server on this channel will receive and send a message with the client.
            // Loops until the node's current round number equals the number of rounds it is supposed to go.
            while(cs.getCurrentRoundNumber() < numOfRounds)
            {
                // If the local server's round number or previous round number equals the node's current round number.
                // The roundNumber - 1 allows to accept message from client from "future" round (that will be buffered until
                // server's node reaches that round).
                if(roundNumber == cs.getCurrentRoundNumber() || roundNumber - 1 == cs.getCurrentRoundNumber()) {

                    // If the local server's round number has reached the max number of rounds, then stop looping.
                    if(roundNumber == numOfRounds)
                    {
                        break;
                    }
                    // Receive message from client.
                    Message receivedMessage = receive();
                    // If server's local round number is greater than the server node's current round number
                    if(receivedMessage.roundNumber > cs.getCurrentRoundNumber())
                    {
                        // Server received a "future" message so updates synchronizer to buffer this message.
                        cs.bufferMessage();
                        System.out.println("BUFFERED MSG RECEIVED: " + receivedMessage.message);
                    }
                    // Otherwise, message is for the current round.
                    else
                    {
                        System.out.println("MSG RECEIVED: " + receivedMessage.message);
                    }
                    // Update synchronizer that message was received from client.
                    cs.messageReceived(clientNodeID, receivedMessage);

                    // If server's local round number is same as server node's current round number
                    if(roundNumber == cs.getCurrentRoundNumber()) {
                        // Send message to client.
                        send();
                        // Update syncrhonizer that message sent to client.
                        cs.messageSent(clientNodeID);
                        // Increment local server's round number - server sends and receives one message with client per round.
                        roundNumber++;
                    }
                }

            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /*
        Method: send
        Description: Sends message to the client node.
        Parameters: None
        Returns: Nothing
    */
    public void send()
    {
        try {
            // MessageInfo for SCTP layer
            MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
            // Message to send to client. Message includes the server node's current round number, the server node ID,
            // client node ID, and the k-hop neighbors of the server node.
            Message msg = new Message("Message from node " + serverInfo.nodeID + " at round " +
                    cs.getCurrentRoundNumber() + " to dest node " + clientNodeID,
                    cs.getCurrentRoundNumber(), serverInfo.nodeID, clientNodeID, cs.getKHopNeighbors());
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
		Description: Receives message from client node.
		Parameters: None
		Returns: Received message sent by client node.
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
