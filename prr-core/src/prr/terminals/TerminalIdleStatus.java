package prr.terminals;

public class TerminalIdleStatus extends Terminal.Status {

    public TerminalIdleStatus(Terminal terminal) {
        terminal.super();
    }

    @Override
    public String getStatusType() {
        return "IDLE";
    }

}
