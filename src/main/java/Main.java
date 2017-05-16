import org.h2.tools.Server;
import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Keith on 4/30/17.
 */
public class Main {
    public static void createTables(Connection conn) throws SQLException{
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS items (id IDENTITY, order_id VARCHAR, name VARCHAR, cost INT, quantity INT)");
        stmt.execute("CREATE TABLE IF NOT EXISTS orders(id IDENTITY, user_id INT)");
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



    public static ArrayList<Item> selectItem (Connection conn, String name) throws SQLException {
        ArrayList<Item> items = new ArrayList<>();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM items WHERE order_id = (select id from customers where userName = ?)");
        stmt.setString(1, name);
        ResultSet results = stmt.executeQuery();

        while (results.next()) {
            int id = results.getInt("items.id");
            int cost = results.getInt("items.cost");
            int quantity =results.getInt("items.quantity");
            Item item = new Item(id, name, cost, quantity);
            items.add(item);
        }
        return items;
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
                    String name = session.attribute("name");


//                    String name = session.attribute("name");
//                    int quantity = Integer.valueOf(session.attribute("quantity"));
//                    int cost = Integer.valueOf(session.attribute("cost"));

                    HashMap m = new HashMap();
                    ArrayList<Item> items =selectItem(conn, userName);
                    m.put("userName", userName);
                    m.put("items", items);

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

                    session.attribute("cost", cost);
                    session.attribute("name", name);
                    session.attribute("quantity", quantity);


                    response.redirect("/");
                    return "";
                })
        );
    }



}
