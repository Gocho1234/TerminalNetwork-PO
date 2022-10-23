package prr.communications;

import java.io.Serial;

import prr.terminals.Terminal;

public abstract class InteractiveCommunication extends Communication {

    /** Serial number for serialization. */
    @Serial
    private static final long serialVersionUID = 202210192344L;

    private double _duration;

    public InteractiveCommunication(double duration, int id,
      Terminal terminalReceiver, Terminal terminalSender) {
        super(id, terminalReceiver, terminalSender);
        _duration = duration;
    }

    public abstract String getCommunicationType();

    @Override
    public double getUnits() {
        return _duration;
    }

}
