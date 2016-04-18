package main;

import java.sql.Connection;
import java.sql.DriverManager;

public class sql_conn {
	
	private static String db_name = "xbrl";
	private static String db_user = "xbrl";
	private static String db_pwd = "xbrl1234";
	private static String db_host = "155.246.104.69";
	private static String db_port = "5432";
	
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