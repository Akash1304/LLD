package elevator.command;

import elevator.service.ElevatorControlSystem;

// Command: a passenger request reified as an object. Requests arrive from
// many buttons (threads) at once but must be applied to the bank in a
// defined order -- so they are queued as commands and drained by the
// control loop, instead of each button thread reaching into elevator
// state directly.
//
// Why Command and not just calling dispatchHallCall(floor, dir) from the
// button: a request that is an object can be queued (BlockingQueue), logged
// for audit, replayed after a controller restart, prioritized (fire alarm
// recall jumps the queue), or cancelled -- none of which a bare method
// call supports.
public interface ElevatorCommand {
    void execute(ElevatorControlSystem system);
    String describe();
}
