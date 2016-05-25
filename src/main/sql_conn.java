package main;

import java.sql.Connection;
import java.sql.DriverManager;

public class sql_conn {
	
	private static String db_name = "";
	private static String db_user = "";
	private static String db_pwd = "";
	private static String db_host = "";
	private static String db_port = "";
	
	public static Connection sqlconn(){
		Connection c = null;
	    try {
	       Class.forName("org.postgresql.Driver");
	       c = DriverManager
	          .getConnection("jdbc:postgresql://" + db_host + ":" + db_port + "/" + db_name,
	          db_user, db_pwd);
	    } catch (Exception e) {
	       e.printStackTrace();
	       System.err.println(e.getClass().getName()+": "+e.getMessage());
	       System.exit(0);
	    }
	    System.out.println("Success");
	    return(c);
	}
}
