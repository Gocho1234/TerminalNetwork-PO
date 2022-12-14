package prr.tariffs;

import java.io.Serial;

import prr.clients.ClientNormalLevel;
import prr.clients.ClientGoldLevel;
import prr.clients.ClientPlatinumLevel;
import prr.communications.TextCommunication;
import prr.communications.VideoCommunication;
import prr.communications.VoiceCommunication;

public class BasePlan extends TariffPlan {

    /** Serial number for serialization. */
    @Serial
    private static final long serialVersionUID = 202210192349L;

    @Override
    public double computePrice(ClientNormalLevel level,
      TextCommunication communication) {
        int units = communication.getUnits();
        double price = 0D;

        if (units < 50) {
            price += 10D;
        } else if (units < 100) {
            price += 16D;
        } else {
            price += units * 2;
        }

        return price;
    }

    @Override
    public double computePrice(ClientNormalLevel level,
      VoiceCommunication communication) {
        return 20D * communication.getUnits() * (isFriend() ? 0.50 : 1D);
    }

    @Override
    public double computePrice(ClientNormalLevel level,
      VideoCommunication communication) {
        return 30D * communication.getUnits() * (isFriend() ? 0.50 : 1D);
    }

    @Override
    public double computePrice(ClientGoldLevel level,
      TextCommunication communication) {
        int units = communication.getUnits();
        double price = 0D;

        if (units < 100) {
            price += 10D;
        } else {
            price += units * 2;
        }

        return price;
    }

    @Override
    public double computePrice(ClientGoldLevel level,
      VoiceCommunication communication) {
        return 10D * communication.getUnits() * (isFriend() ? 0.50 : 1D);
    }

    @Override
    public double computePrice(ClientGoldLevel level,
      VideoCommunication communication) {
        return 20D * communication.getUnits() * (isFriend() ? 0.50 : 1D);
    }

    @Override
    public double computePrice(ClientPlatinumLevel level,
      TextCommunication communication) {
        int units = communication.getUnits();
        double price = 0D;

        if (units >= 50) {
            price += 4D;
        }

        return price;
    }

    @Override
    public double computePrice(ClientPlatinumLevel level,
      VoiceCommunication communication) {
        return 10D * communication.getUnits() * (isFriend() ? 0.50 : 1D);
    }

    @Override
    public double computePrice(ClientPlatinumLevel level,
      VideoCommunication communication) {
        return 10D * communication.getUnits() * (isFriend() ? 0.50 : 1D);
    }

}
