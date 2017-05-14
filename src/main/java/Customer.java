/**
 * Created by Keith on 5/14/17.
 */
    public class Customer {
        private int id;
        private String userName;

        public Customer(Integer id, String userName) {
            this.id = id;
            this.userName = userName;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

}

