package main;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class check_data {

	public static void main(String args[]) {
				
 		Connection conn = sql_conn.sqlconn();
		try{
 			Statement st = conn.createStatement();
 			ResultSet rs = st.executeQuery("SELECT * FROM tm_risk_10k_subtitle where filing_date < '2011-01-01'"
 											+ " and subtitle != '' order by random() limit 20");
 			int docN = 0;
 			while (rs.next()){
 				String acc = rs.getString("filing_number");
 				String[] sub = rs.getString("subtitle").split(",");
 				docN ++;
 				System.out.println();
 				System.out.print("#" + docN + "   " + acc);
 				System.out.println("  Total title: " + sub.length);
 				for(int i = 0; i < sub.length; i ++){
 					if(!sub[i].isEmpty()){
 						System.out.println(sub[i]);
 					}					
 				}
 			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
