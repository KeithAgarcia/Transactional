import java.sql.Date;

/**
 * Created by Keith on 5/16/17.
 */
public class Order {
    int id;
    Date dateTime;

    public Order() {
    }

    public Order(int id, Date dateTime) {
        this.id = id;
        this.dateTime = dateTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime;
    }
}