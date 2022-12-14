package prr.app.clients;

import prr.Network;
import prr.app.util.ToStringer;
import prr.app.exceptions.UnknownClientKeyException;
import pt.tecnico.uilib.menus.Command;
import pt.tecnico.uilib.menus.CommandException;

/**
 * Show specific client: also show previous notifications.
 */
class DoShowClient extends Command<Network> {

    DoShowClient(Network receiver) {
        super(Label.SHOW_CLIENT, receiver);
        addStringField("clientId", Prompt.key());
    }

    @Override
    protected final void execute() throws CommandException {
        ToStringer toStringer = new ToStringer();
        String clientId = stringField("clientId");
        try {
            _display.popup(_receiver.getClient(clientId)
                                    .accept(toStringer));
            _receiver.getClientNotifications(clientId)
                    .stream()
                    .map(o -> o.accept(toStringer))
                    .forEach(_display::popup);
        } catch (prr.exceptions.UnknownClientKeyException e) {
            throw new UnknownClientKeyException(e.getKey());
        }
    }

}
