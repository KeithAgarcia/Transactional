import org.h2.tools.Server;
import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Keith on 4/30/17.
 */
public class Main {
    public static void createTables(Connection conn) throws SQLException{
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS items (id IDENTITY, order_id INT, name VARCHAR, cost INT, quantity INT)");
        stmt.execute("CREATE TABLE IF NOT EXISTS orders(id IDENTITY, user_id INT, dateTime DATE)");
        stmt.execute("CREATE TABLE IF NOT EXISTS customers (id IDENTITY, userName VARCHAR)");

    }

    public static void addUser(Connection conn, String userName) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO customers VALUES (NULL, ?, )");
        stmt.setString(1, userName);
        stmt.execute();
    }
    public static Customer selectUser(Connection conn, String userName) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM customers WHERE username = ?");
        stmt.setString(1, userName);
        ResultSet results = stmt.executeQuery();
        if (results.next()) {
            int id = results.getInt("id");;
            return new Customer(id, userName);
        }
        return null;
    }

    public static void insertItem(Connection conn, int order_id, String name, int cost, int quantity) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO items VALUES (NULL, ?, ? ,?, ?)");
        stmt.setInt(1, order_id);
        stmt.setString(2, name);
        stmt.setInt(3, cost);
        stmt.setInt(4, quantity);
         stmt.execute();
    }

    public static void insertOrder(Connection conn, int user_id) throws SQLException{
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO orders VALUES(NULL, ?, NULL)");
        stmt.setInt(1, user_id);
        stmt.execute();
    }

    private static void checkoutOrder(Connection conn, Order order) throws SQLException {
        // update the orders table
        // where the order id is order.id:
        // set the date to right now's date.
        PreparedStatement stmt = conn.prepareStatement("update orders set dateTime = ? where id = ?");

        // replacing the first question mark with the date object for right now
        stmt.setDate(1, new Date(Instant.now().toEpochMilli()));

        // id for the order we're looking to update.
        stmt.setInt(2, order.getId());

        stmt.execute();
    }


    public static ArrayList<Item> selectItems(Connection conn, String userName) throws SQLException {
        ArrayList<Item> items = new ArrayList<>();
        PreparedStatement stmt = conn.prepareStatement("select items.* from orders inner join items on orders.id = items.order_id inner join customers on customers.id = orders.user_id where username = ? and orders.datetime is null");
        stmt.setString(1, userName);
        ResultSet results = stmt.executeQuery();

        while (results.next()) {
            String name = results.getString("name");
            int cost = results.getInt("cost");
            int quantity =results.getInt("quantity");
            int id = results.getInt("id");
            Item item = new Item(id, name, cost, quantity);
            items.add(item);
        }
        return items;
    }

    public static Order selectOrder (Connection conn, int user_id) throws SQLException{
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM orders WHERE user_id = ? AND dateTime is null");
        stmt.setInt(1, user_id);

        ResultSet results = stmt.executeQuery();

        // this represents the order we're going to return.
        Order order = null;

        // if the results has data, e.g. there is a current order
        if (results.next()) {
            // build new order object based off what we get in our resultset.
            order = new Order(results.getInt("id"), results.getDate("dateTime"));
        }

        return order;
    }
    public static void main(String[] args) throws SQLException {
        Server.createWebServer().start();
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        createTables(conn);
        Spark.init();

        Spark.get(
                "/",
                ((request, response) -> {
                    Session session = request.session();
                    String userName = session.attribute("userName");


                    HashMap m = new HashMap();

//                    Order order = selectOrder(conn, id);
                    ArrayList<Item> items = selectItems(conn,userName );
                    m.put("items", items);
                    m.put("userName", userName);
//                    m.put("")


                    return new ModelAndView(m, "home.html");
                }),
                new MustacheTemplateEngine()
        );

        Spark.post(
                "/login",
                ((request, response) -> {
                    // get the username from a post request
                    String userName = request.queryParams("loginName");
                    if (userName == null) {
                        throw new Exception("Login name not found.");
                    }

                    // try to find customer by username
                    Customer customer = selectUser(conn, userName);
                    if (customer == null) {
                        // ..insert the customer if there is none
                        addUser(conn, userName);
                    }

                    // store username in session for future requests.
                    Session session = request.session();
                    session.attribute("userName", userName);

                    response.redirect("/");
                    return "";
                })
        );

        Spark.post(
                "/logout",
                ((request, response) -> {
                    Session session = request.session();
                    session.invalidate();
                    response.redirect("/");
                    return "";
                })
        );

        Spark.post(
                "/add-to-cart",
                ((request, response) -> {
                    Session session = request.session();
                    String userName = session.attribute("userName");

                    if (userName == null){
                        throw new Exception("Not logged in.");
                    }

                    String name = request.queryParams("name");
                    int cost = Integer.valueOf(request.queryParams("cost"));
                    int quantity = Integer.valueOf(request.queryParams("quantity"));

                    Customer customer = selectUser(conn, userName);

                    // if there is no current order, we need to insert one.
                    if (selectOrder(conn, customer.getId()) == null) {
                        insertOrder(conn, customer.getId());
                    }

                    // we know at this point that the order must exist.
                    // so we get the reference to the current order...
                    Order currentOrder = selectOrder(conn, customer.getId());

                    // and then we can use that order's id here.
                    insertItem(conn, currentOrder.getId(), name, cost, quantity);

                    response.redirect("/");
                    return "";
                })
        );

        Spark.post(
                "/checkout",
                ((request, response) ->{
                    Session session = request.session();
                    String userName = session.attribute("userName");

                    if(userName == null) {
                        throw new Exception(("Not logged in."));
                    }

                    Date time = new Date(Instant.now().toEpochMilli());
                    Customer customer = selectUser(conn, userName);
                    Order currentOrder = selectOrder(conn, customer.getId());

                    // we need a method that sets the datetime field to RIGHT NOW
                    checkoutOrder(conn, currentOrder);

                    response.redirect("/");
                    return"";
                })
        );
    }
}
// int total =0;
//for every item;
//sum + = item.cost * item.quantity