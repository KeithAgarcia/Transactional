import org.h2.tools.Server;
import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
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

    public static void checkoutOrder(Connection conn, int user_id, Date dateTime) throws SQLException{
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO orders VALUES(NULL, ?, ?)");
        stmt.setInt(1, user_id);
        stmt.setDate(2, dateTime);
        stmt.execute();
    }



    public static ArrayList<Item> selectItem (Connection conn, int id) throws SQLException {
        ArrayList<Item> items = new ArrayList<>();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM items WHERE order_id = (select id from customers where userName = ?)");
        stmt.setInt(1, id);
        ResultSet results = stmt.executeQuery();

        while (results.next()) {
            String name = results.getString("items.name");
            int cost = results.getInt("items.cost");
            int quantity =results.getInt("items.quantity");
            Item item = new Item(id, name, cost, quantity);
            items.add(item);
        }
        return items;
    }

    public static ArrayList<Order> selectOrder (Connection conn, int user_id, Date dateTime) throws SQLException{
        ArrayList<Order> orders = new ArrayList<>();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM oders WHERE item_id = (select id from customers where userName =?)");
        stmt.setInt(1, user_id);
        stmt.setObject(2, dateTime);
        Order order = new Order(user_id, dateTime);
        orders.add(order);

        return orders;
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

//                    ArrayList<Order> orders = selectOrder(conn, );


                    m.put("userName", userName);
                    m.put("items", items);
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
                    insertItem(conn, customer.getId(), name, cost, quantity);


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
                    checkoutOrder(conn, customer.getId(), time);

                    response.redirect("/");
                    return"";
                })
        );
    }
}
