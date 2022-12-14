package prr.clients;

import java.util.Collection;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;
import java.io.Serial;

import prr.util.Visitor;
import prr.util.Visitable;
import prr.communications.TextCommunication;
import prr.communications.VideoCommunication;
import prr.communications.VoiceCommunication;
import prr.notifications.Notification;
import prr.notifications.NotificationDeliveryMethod;
import prr.tariffs.BasePlan;
import prr.tariffs.TariffPlan;
import prr.terminals.Terminal;

public class Client implements Serializable, Visitable {

    /** Serial number for serialization. */
    @Serial
    private static final long serialVersionUID = 202210150050L;

    private final String _id;
    private String _name;
    private int _taxId;
    private Map<String, Terminal> _terminals;
    private Level _level;
    private boolean _receiveNotifications;
    private Set<Notification> _notifications;
    private NotificationDeliveryMethod _deliveryMethod;

    public Client(String id, String name, int taxId) {
        _id = id;
        _name = name;
        _taxId = taxId;
        _terminals = new HashMap<String, Terminal>();
        _level = new ClientNormalLevel(this, 0D, 0D, new BasePlan());
        _receiveNotifications = true;
        _notifications = new LinkedHashSet<Notification>();
        _deliveryMethod = new DefaultDeliveryMethod();
    }

    public String getId() {
        return _id;
    }

    public String getName() {
        return _name;
    }

    public int getTaxId() {
        return _taxId;
    }

    public int getNumberOfTerminals() {
        return _terminals.size();
    }

    public boolean isOwnerOf(Terminal terminal) {
        return _terminals.containsKey(terminal.getTerminalId());
    }

    public void addTerminal(Terminal terminal) {
        _terminals.put(terminal.getTerminalId(), terminal);
    }

    public String getLevelType() {
        return _level.getLevelType();
    }

    public boolean hasNotificationsEnabled() {
        return _receiveNotifications;
    }

    public void setNotificationState(boolean notificationState) {
        _receiveNotifications = notificationState;
    }

    public void setNotificationDeliveryMethod(
      NotificationDeliveryMethod deliveryMethod) {
        _deliveryMethod = deliveryMethod;
    }

    public Collection<Notification> readNotifications() {
        Collection<Notification> notifications =
            new LinkedHashSet<>(_notifications);
        _notifications.clear();
        return notifications;
    }

    public void notify(Notification notification) {
        if (hasNotificationsEnabled()) {
            _deliveryMethod.deliver(notification);
        }
    }

    public double getPayments() {
        return _level.getPayments();
    }

    public double getDebts() {
        return _level.getDebts();
    }

    public void updateBalance(double delta) {
        _level.updateBalance(delta);
    }

    public void resetNumberOfConsecutiveCommunications() {
        _level.resetNumberOfConsecutiveCommunications();
    }

    public void increaseNumberOfConsecutiveTextCommunications() {
        _level.increaseNumberOfConsecutiveTextCommunications();
    }

    public void increaseNumberOfConsecutiveVideoCommunications() {
        _level.increaseNumberOfConsecutiveVideoCommunications();
    }

    public TariffPlan getTariffPlan() {
        return _level.getTariffPlan();
    }

    public void setTariffPlan(TariffPlan plan) {
        _level.setTariffPlan(plan);
    }

    public double computePrice(TextCommunication communication) {
        return _level.computePrice(communication);
    }

    public double computePrice(VoiceCommunication communication) {
        return _level.computePrice(communication);
    }

    public double computePrice(VideoCommunication communication) {
        return _level.computePrice(communication);
    }

    public void verifyLevelUpdateConditions(boolean hasPayed) {
        _level.verifyLevelUpdateConditions(hasPayed);
    }

    public String accept(Visitor visitor) {
        return visitor.visit(this);
    }

    public class DefaultDeliveryMethod extends NotificationDeliveryMethod {

        /** Serial number for serialization. */
        @Serial
        private static final long serialVersionUID = 202210192341L;

        @Override
        public void deliver(Notification notification) {
            _notifications.add(notification);
        }

    }

    public abstract class Level implements Serializable {

        /** Serial number for serialization. */
        @Serial
        private static final long serialVersionUID = 202210150052L;

        private double _payments;
        private double _debts;
        private int _numberOfConsecutiveTextCommunications;
        private int _numberOfConsecutiveVideoCommunications;
        private TariffPlan _plan;

        public Level(double payments, double debts,
          TariffPlan plan) {
            _payments = payments;
            _debts = debts;
            _numberOfConsecutiveTextCommunications = 0;
            _numberOfConsecutiveVideoCommunications = 0;
            _plan = plan;
        }

        protected Client getClient() {
            return Client.this;
        }

        protected void updateLevel(Level level) {
            Client.this._level = level;
        }

        protected abstract String getLevelType();

        protected double getPayments() {
            return _payments;
        }

        protected double getDebts() {
            return _debts;
        }

        protected double getBalance() {
            return getPayments() - getDebts();
        }

        private void updateBalance(double delta) {
            if (delta < 0) {
                _debts += (delta * -1);
            } else {
                _debts -= delta;
                _payments += delta;
            }
        }

        protected int getNumberOfConsecutiveTextCommunications() {
            return _numberOfConsecutiveTextCommunications;
        }

        protected int getNumberOfConsecutiveVideoCommunications() {
            return _numberOfConsecutiveVideoCommunications;
        }

        protected void resetNumberOfConsecutiveCommunications() {
            _numberOfConsecutiveTextCommunications = 0;
            _numberOfConsecutiveVideoCommunications = 0;
        }

        private void increaseNumberOfConsecutiveTextCommunications() {
            _numberOfConsecutiveVideoCommunications = 0;
            _numberOfConsecutiveTextCommunications += 1;
        }

        private void increaseNumberOfConsecutiveVideoCommunications() {
            _numberOfConsecutiveTextCommunications = 0;
            _numberOfConsecutiveVideoCommunications += 1;
        }

        protected TariffPlan getTariffPlan() {
            return _plan;
        }

        private void setTariffPlan(TariffPlan plan) {
            _plan = plan;
        }

        protected abstract double computePrice(TextCommunication communication);

        protected abstract double computePrice(VoiceCommunication communication);

        protected abstract double computePrice(VideoCommunication communication);

        protected abstract void verifyLevelUpdateConditions(boolean hasPayed);

    }

}
