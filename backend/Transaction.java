public class Transaction{
    private Bank sender;
    private Bank receiver;
    private double amount;

    public Transaction(Bank sender, Bank receiver, double amount){
        if(!Double.isFinite(amount) || amount <= 0 || !hasAtMostTwoDecimalPlaces(amount)){
            throw new IllegalArgumentException("Transaction amount must be finite, positive, and have at most two decimal places.");
        }

        if(sender == receiver){
            throw new IllegalArgumentException("Sender and receiver must be different banks.");
        }

        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
    }

    public Bank getSender(){
        return sender;
    }

    public Bank getReceiver(){
        return receiver;
    }

    public double getAmount(){
        return amount;
    }

    private boolean hasAtMostTwoDecimalPlaces(double value){
        double scaledValue = value * 100.0;

        if(!Double.isFinite(scaledValue) || scaledValue >= Long.MAX_VALUE || scaledValue <= Long.MIN_VALUE){
            return false;
        }

        long cents = Math.round(scaledValue);
        return value == cents / 100.0;
    }
}
