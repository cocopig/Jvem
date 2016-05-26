package main;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.opencsv.CSVReader;

public class parameters {
	
	
	public int seed = 1234;
	public String beta_file = "";
	public String doc_topic_file = "";
	public String sent_topic_file = "";
	public String db_addr = "";
	public String db_user = "";
	public String db_pwd = "";
	public String read_table = "";

	
	public parameters(String db_csv, String file_csv) throws IOException{
		CSVReader reader = null;
		try {
			reader = new CSVReader(new FileReader(file_csv));
			String [] nextLine;
			String [] temp = new String[3];
			int i = 0;
			while ((nextLine = reader.readNext()) != null) {
				temp[i] = nextLine[1] + seed + ".csv";				
			}
			
			beta_file = temp[0];
			doc_topic_file = temp[1];
			sent_topic_file = temp[2];
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			reader = new CSVReader(new FileReader(db_csv));
			String [] nextLine;
			String [] temp = new String[3];
			int i = 0;
			while ((nextLine = reader.readNext()) != null) {
				temp[i] = nextLine[1];				
			}
			
			db_addr = temp[0];
			db_user = temp[1];
			db_pwd = temp[2];
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
}
