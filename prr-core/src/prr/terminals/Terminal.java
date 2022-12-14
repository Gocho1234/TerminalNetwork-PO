package prr.terminals;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.io.Serializable;
import java.io.Serial;

import prr.Network;
import prr.util.Visitor;
import prr.util.Visitable;
import prr.clients.Client;
import prr.communications.Communication;
import prr.communications.TextCommunication;
import prr.communications.InteractiveCommunication;
import prr.communications.VoiceCommunication;
import prr.notifications.Notification;
import prr.exceptions.IllegalTerminalStatusException;
import prr.exceptions.InvalidCommunicationException;
import prr.exceptions.InvalidFriendException;
import prr.exceptions.UnknownTerminalKeyException;
import prr.exceptions.UnreachableBusyTerminalException;
import prr.exceptions.UnreachableOffTerminalException;
import prr.exceptions.UnreachableSilentTerminalException;
import prr.exceptions.UnsupportedCommunicationAtOriginException;
import prr.exceptions.UnsupportedCommunicationAtDestinationException;

/**
 * Abstract terminal.
 */
abstract public class Terminal implements Serializable, Visitable {

    /** Serial number for serialization. */
    @Serial
    private static final long serialVersionUID = 202208091753L;

    private final String _id;
    private Client _owner;
    private double _payments;
    private double _debts;
    private InteractiveCommunication _ongoingCommunication;
    private Map<Integer, Communication> _communications;
    private Map<String, Terminal> _terminalFriends;
    private Status _status;
    private List<Client> _clientsToNotify;

    public Terminal(String id, Client owner) {
        _id = id;
        _owner = owner;
        _payments = 0D;
        _debts = 0D;
        _ongoingCommunication = null;
        _communications = new HashMap<Integer, Communication>();
        _terminalFriends = new TreeMap<String, Terminal>();
        _status = new TerminalIdleStatus(this);
        _clientsToNotify = new LinkedList<Client>();
        _owner.addTerminal(this);
    }

    public abstract String getTerminalType();

    public String getTerminalId() {
        return _id;
    }

    public String getClientId() {
        return _owner.getId();
    }

    public Client getOwner() {
        return _owner;
    }

    public double getPayments() {
        return _payments;
    }

    public double getDebts() {
        return _debts;
    }

    public double getBalance() {
        return getPayments() - getDebts();
    }

    public void updateBalance(double delta) {
        if (delta < 0) {
            _debts += (delta * -1);
        } else {
            _debts -= delta;
            _payments += delta;
        }
        _owner.updateBalance(delta);
    }

    public void performPayment(int communicationId, Network network)
      throws InvalidCommunicationException {
        Communication communication = network.getCommunication(communicationId);
        if (!this.equals(communication.getTerminalSender()) ||
          communication.isOngoing() || communication.isPaid()) {
            throw new InvalidCommunicationException();
        }
        double price = communication.pay();
        updateBalance(price);
        getOwner().verifyLevelUpdateConditions(true);
        network.changed();
    }

    public Communication getOngoingCommunication()
      throws InvalidCommunicationException {
        if (_ongoingCommunication == null) {
            throw new InvalidCommunicationException();
        }
        return _ongoingCommunication;
    }

    public void setOngoingCommunication(
      InteractiveCommunication communication) {
        _ongoingCommunication = communication;
    }

    public boolean isUnused() {
        return _communications.isEmpty();
    }

    /**
     * Checks if this terminal can end the current interactive communication.
     *
     * @return true if this terminal is busy (i.e., it has an active interactive
     *         communication) and it was the originator of this communication.
     **/
    public boolean canEndCurrentCommunication() {
        return _ongoingCommunication != null &&
            this.equals(_ongoingCommunication.getTerminalSender());
    }

    /**
     * Checks if this terminal can start a new communication.
     *
     * @return true if this terminal is neither off neither busy, false otherwise.
     **/
    public boolean canStartCommunication() {
        return _status.canStartCommunication();
    }

    protected void assertTextCommunicationReception(Client clientToNotify)
      throws UnreachableOffTerminalException {
        _status.assertTextCommunicationReception(clientToNotify);
    }

    protected void assertInteractiveCommunicationReception(
      Client clientToNotify) throws UnreachableOffTerminalException,
      UnreachableBusyTerminalException, UnreachableSilentTerminalException {
        _status.assertInteractiveCommunicationReception(clientToNotify);
    }

    public void addCommunication(Communication communication) {
        _communications.put(communication.getId(), communication);
    }

    public double endOngoingCommunication(int duration, Network network) {
        double communicationPrice = 0D;
        if (canEndCurrentCommunication()) {
            communicationPrice =
                _ongoingCommunication.finishCommunication(duration);
            getOwner().verifyLevelUpdateConditions(false);
            network.changed();
        }
        return communicationPrice;
    }

    public void sendSMS(String terminalReceiverId, Network network,
      String message) throws UnknownTerminalKeyException,
      UnreachableOffTerminalException {
        if (canStartCommunication()) {
            network.changed();
            Terminal receiver = network.getTerminal(terminalReceiverId);
            receiver.receiveSMS(this, network, message);
        }
    }

    private void receiveSMS(Terminal sender, Network network,
      String newMessage) throws UnreachableOffTerminalException {
        assertTextCommunicationReception(sender.getOwner());
        int newId = network.getNextCommunicationId();
        TextCommunication communication =
            new TextCommunication(newMessage, newId, this, sender);
        network.registerCommunication(communication);
        sender.getOwner().verifyLevelUpdateConditions(false);
    }

    public void makeVoiceCall(String terminalReceiverId, Network network)
      throws UnknownTerminalKeyException, UnreachableOffTerminalException,
      UnreachableBusyTerminalException, UnreachableSilentTerminalException,
      InvalidCommunicationException {
        if (terminalReceiverId.equals(getTerminalId())) {
            throw new InvalidCommunicationException();
        }
        if (canStartCommunication()) {
            network.changed();
            Terminal receiver = network.getTerminal(terminalReceiverId);
            receiver.receiveVoiceCall(this, network);
        }
    }

    private void receiveVoiceCall(Terminal sender, Network network)
      throws UnreachableOffTerminalException, UnreachableBusyTerminalException,
      UnreachableSilentTerminalException {
        assertInteractiveCommunicationReception(sender.getOwner());
        int newId = network.getNextCommunicationId();
        VoiceCommunication communication =
            new VoiceCommunication(newId, this, sender);
        network.registerCommunication(communication);
    }

    public abstract void makeVideoCall(String terminalReceiverId,
      Network network) throws UnsupportedCommunicationAtOriginException,
      UnsupportedCommunicationAtDestinationException, UnknownTerminalKeyException,
      UnreachableOffTerminalException, UnreachableBusyTerminalException,
      UnreachableSilentTerminalException, InvalidCommunicationException;

    protected abstract void receiveVideoCall(Terminal sender, Network network)
      throws UnsupportedCommunicationAtDestinationException,
      UnreachableOffTerminalException, UnreachableBusyTerminalException,
      UnreachableSilentTerminalException;

    protected void addToNotify(Client client) {
        _clientsToNotify.add(client);
    }

    protected void notifyAllClients(Notification notification) {
        for (Client client : _clientsToNotify) {
            client.notify(notification);
        }
        _clientsToNotify.clear();
    }

    public String getFriendsIds() {
        return _terminalFriends.keySet()
                                .stream()
                                .collect(Collectors.joining(","));
    }

    public boolean hasFriends() {
        return !_terminalFriends.isEmpty();
    }

    public boolean isFriend(Terminal terminal) {
        return _terminalFriends.containsKey(terminal.getTerminalId());
    }

    public void addFriend(String terminalFriendId, Network network)
      throws UnknownTerminalKeyException, InvalidFriendException {
        Terminal terminalFriend = network.getTerminal(terminalFriendId);
        if (this.equals(terminalFriend) ||
          this.isFriend(terminalFriend)) {
            throw new InvalidFriendException();
        }
        _terminalFriends.put(terminalFriendId, terminalFriend);
        network.changed();
    }

    public void removeFriend(String terminalFriendId, Network network)
      throws UnknownTerminalKeyException, InvalidFriendException {
        Terminal terminalFriend = network.getTerminal(terminalFriendId);
        if (!this.isFriend(terminalFriend)) {
            throw new InvalidFriendException();
        }
        _terminalFriends.remove(terminalFriendId);
        network.changed();
    }

    public String getStatusType() {
        return _status.getStatusType();
    }

    public void setStatus(String status) throws IllegalTerminalStatusException {
        _status.setStatus(status);
    }

    public void setOnIdle(Network network)
      throws IllegalTerminalStatusException, UnreachableBusyTerminalException {
        _status.setOnIdle();
        network.changed();
    }

    public void setOnSilent(Network network)
      throws IllegalTerminalStatusException, UnreachableBusyTerminalException {
        _status.setOnSilent();
        network.changed();
    }

    public void turnOff(Network network)
      throws IllegalTerminalStatusException, UnreachableBusyTerminalException {
        _status.turnOff();
        network.changed();
    }

    public void setOnBusy() {
        _status.setOnBusy();
    }

    public void unBusy() {
        _status.unBusy();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Terminal) {
            Terminal terminal = (Terminal) o;
            return getTerminalId().compareTo(terminal.getTerminalId()) == 0;
        }
        return false;
    }

    public String accept(Visitor visitor) {
        return visitor.visit(this);
    }

    public abstract class Status implements Serializable {

        /** Serial number for serialization. */
        @Serial
        private static final long serialVersionUID = 202210150058L;

        protected Terminal getTerminal() {
            return Terminal.this;
        }

        protected void updateStatus(Status status) {
            Terminal.this._status = status;
        }

        protected void setStatus(String status)
          throws IllegalTerminalStatusException {
            Terminal.this._status = switch(status) {
                case "ON" -> new TerminalIdleStatus(getTerminal());
                case "OFF" -> new TerminalOffStatus(getTerminal());
                case "SILENCE" -> new TerminalSilentStatus(getTerminal());
                default -> throw new IllegalTerminalStatusException(status);
            };
        }

        protected abstract String getStatusType();

        protected abstract boolean canStartCommunication();

        protected abstract void assertTextCommunicationReception(
          Client clientToNotify) throws UnreachableOffTerminalException;

        protected abstract void assertInteractiveCommunicationReception(
          Client clientToNotify) throws UnreachableOffTerminalException,
          UnreachableBusyTerminalException, UnreachableSilentTerminalException;

        protected abstract void setOnIdle()
          throws IllegalTerminalStatusException,
          UnreachableBusyTerminalException;

        protected abstract void setOnSilent()
          throws IllegalTerminalStatusException,
          UnreachableBusyTerminalException;

        protected abstract void turnOff()
          throws IllegalTerminalStatusException,
          UnreachableBusyTerminalException;

        protected abstract void setOnBusy();

        protected abstract void unBusy();

    }

}
