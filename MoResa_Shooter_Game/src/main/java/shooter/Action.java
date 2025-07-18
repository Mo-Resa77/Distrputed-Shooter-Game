/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// " second class "
package shooter;

import java.io.Serializable;
import java.util.UUID;


/*Tracks who sent the action and who is the target
Uses logical clocks for ordering actions
Implements Comparable to sort actions in the queue*/
//Comparable<Action> عشان سورتينج للبريورتي كيو - Serializable for sending data in bytes//
public class Action implements Comparable<Action> /*interface*/, Serializable {

    private static final long serialVersionUID = 1L; // version control of serzz

    public static final int SHOOT = 1;
    public static final int HEAL = 2;
    public static final int MOVE = 3;

    public String actionId, sender, target;
    public int actionType, logicalClock, pid;
    public long receiptTime;

    public Action(String sender, String target, int actionType) {
        this(sender, target, actionType, 0, 0);  // Default logical clock and pid & calls second constructor for flexibilaty // 
    }

    /*actionId: Unique identifier for each action (UUID)
sender: ID of the player performing the action
target: ID of the player being targeted
actionType: Type of action (SHOOT/HEAL/MOVE)
logicalClock: For ordering actions
pid: Process ID of the sender
receiptTime: Timestamp when action is received*/
    // second constructor //
    public Action(String sender, String target, int actionType, int logicalClock, int pid) {
        this.actionId = UUID.randomUUID().toString();
        this.sender = sender;
        this.target = target;
        this.actionType = actionType;
        this.logicalClock = logicalClock;
        this.pid = pid;
        this.receiptTime = System.nanoTime();
    }

    @Override // from comparable interface 
    public int compareTo(Action o) {
        if (this.logicalClock != o.logicalClock) { //sequence of actions in the game ,ncreases by 1 for each new action
            return Integer.compare(this.logicalClock, o.logicalClock);
        }
        if (this.receiptTime != o.receiptTime) { //: Records when an action is received ,Uses system's nanosecond timer
            return Long.compare(this.receiptTime, o.receiptTime);
        }
        return Integer.compare(this.pid, o.pid); //index detrmined in main class
    }

    @Override //from object class parent of all classes
    public String toString() {
        return sender + "->" + target + ":"
                + (actionType == SHOOT ? "SHOOT" : actionType == HEAL ? "HEAL" : "MOVE")
                + " (clock:" + logicalClock + ")";
    }
}
