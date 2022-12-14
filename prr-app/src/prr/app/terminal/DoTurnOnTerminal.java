package prr.app.terminal;

import prr.Network;
import prr.terminals.Terminal;
import prr.exceptions.IllegalTerminalStatusException;
import prr.exceptions.UnreachableBusyTerminalException;
import pt.tecnico.uilib.menus.CommandException;

/**
 * Turn on the terminal.
 */
class DoTurnOnTerminal extends TerminalCommand {

    DoTurnOnTerminal(Network context, Terminal terminal) {
        super(Label.POWER_ON, context, terminal);
    }

    @Override
    protected final void execute() throws CommandException {
        try {
            _receiver.setOnIdle(_network);
        } catch (IllegalTerminalStatusException e) {
            _display.popup(Message.alreadyOn());
        } catch (UnreachableBusyTerminalException e) {
            // do nothing
        }
    }

}
