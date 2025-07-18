MoResa Shooter Game

MoResa Shooter Game is a distributed, multiplayer shooting game implemented in Java using Remote Method Invocation (RMI) for network communication. The game operates on a 10x10 grid where up to four players (A, B, C, D) can engage in actions such as shooting, healing, or moving. The system ensures fault tolerance through a leader election mechanism and secures communication with AES encryption.

Table of Contents





Features



Architecture



Prerequisites



Installation



Running the Game



Game Rules



Code Structure



Contributing



License

Features





Multiplayer Support: Supports up to four players, each running on a separate node.



Distributed System: Utilizes Java RMI for node-to-node communication.



Action Types:





Shoot: Reduces the target player's health by 10 if within a 3-unit range.



Heal: Increases the target player's health by 10 if within a 3-unit range.



Move: Allows players to move up, down, left, or right on the grid.



Fault Tolerance: Implements a leader election algorithm to ensure continuous gameplay if the host node fails.



Secure Communication: Uses AES encryption to secure action messages between nodes.



Logical Clock: Ensures proper ordering of game actions using Lamport logical clocks.



Thread-Safe Operations: Utilizes concurrent data structures like ConcurrentHashMap and PriorityQueue for reliable state management.

Architecture

The game is built as a distributed system with the following components:





NodeI: RMI interface defining communication methods (sendAction, multicastAction, receiveAction).



Node: Main game logic implementation, handling player data, action processing, and leader election.



Action: Represents game actions (SHOOT, HEAL, MOVE) with logical clock ordering and serialization support.



FighterData: Manages player information, including position, health, and network details.



Encryptor: Handles AES encryption and decryption of action messages.



MoResa_Shooter_Game: Main class for initializing and running the game nodes.

The system uses a leader-based approach where the host node (ID 0) initializes the game state and processes actions. Non-host nodes forward actions to the leader, which multicasts them to all nodes for consistency.
