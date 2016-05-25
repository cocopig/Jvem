package main;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Random;

import com.opencsv.CSVWriter;

import coco.lda.vem.*;
import coco.lda.conf.*;

public class main {

	public static void main(String [ ] args){
		
		parameters par = new parameters();
		
		//Step 0: define parameter and initial parameters.
		int Numdoc = 0, Numtopic = 30;
		double ALPHA = 1.0;
		double[][] BETA;
		String[][][] doc_sent_word = null;
		double[][] doc_topic = null;
		String[][] docinfo = null;
		HashMap<int[], double[]> sent_topic = new HashMap<int[], double[]>();
		
		HashMap <String, Integer> vocabulary = new HashMap <String, Integer>();
		
		int seed = par.seed;
		Random generator = new Random(seed);
		String beta_file = par.beta_file;
		String doc_topic_file = par.doc_topic_file;
		String sent_topic_file = par.sent_topic_file;
		
		vemConfig conf = new vemConfig();
		
		
		//Step 1: Read data and get dws, initial beta.
		String db_addr = par.db_addr;
		String db_user = par.db_user;
		String db_pwd = par.db_pwd;
		String read_table = par.read_table;
		String wherecondition = " WHERE filing_date < '2015-01-01' and filing_date > '2014-01-01' and subtitle != ''";
		
		
		try{
			Connection connt = DriverManager.getConnection(db_addr, db_user, db_pwd);
			System.out.println("Success connecting ...");					
			Statement st = connt.createStatement();	
			
			String select_query = "SELECT count(filing_number) as tot FROM " + read_table 
								+ wherecondition;
			ResultSet rst = st.executeQuery(select_query);
			
			while(rst.next()){
				Numdoc = rst.getInt("tot");
			}	
//			Numdoc = 3;
			doc_sent_word = new String[Numdoc][][];
			doc_topic = new double[Numdoc][Numtopic];
			docinfo = new String[Numdoc][5];
			System.out.println("Step0 - Finish Reading: " + Numdoc);
			
			
			select_query = "SELECT * FROM " + read_table 
							+ wherecondition;
			rst = st.executeQuery(select_query);
			
			cleanDATA clean = new cleanDATA();

			int doc = 0;
			int v = 0;
			while(rst.next()){
				docinfo[doc][0] = rst.getString("filing_number");
				docinfo[doc][1] = rst.getString("filing_date");
				docinfo[doc][2] = rst.getString("name");
				docinfo[doc][3] = String.valueOf(rst.getInt("cik"));
				docinfo[doc][4] = String.valueOf(rst.getInt("sic"));
				
				String[] temp = clean.cleanString(rst.getString("subtitle")).split(",");
//				System.out.println(clean.cleanString(rst.getString("subtitle")));
				doc_sent_word[doc] = new String[temp.length][];
				
				for(int sent = 0; sent < temp.length; sent ++){
					doc_sent_word[doc][sent] = temp[sent].split("_");
					for(int j = 0; j < temp[sent].split("_").length; j ++){
						String word = doc_sent_word[doc][sent][j];		
						if(vocabulary.containsKey(word)){
							continue;
						}else{
							vocabulary.put(word, v);
							v ++ ;
						}
					}
				}			
				doc ++;
			}
			connt.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		
		System.out.println("Step0 - Finish Reading: " + vocabulary.size());
		
		//Initial BETA
		BETA = new double[Numtopic][vocabulary.size()];
		for(int topic = 0; topic < Numtopic; topic ++){
			double betasum = 0.0;
			for(int word = 0; word < vocabulary.size(); word ++){
				BETA[topic][word] = 1.0/vocabulary.size() + generator.nextDouble();
				betasum = betasum + BETA[topic][word];
			}
			for(int word = 0; word < vocabulary.size(); word ++){
				BETA[topic][word] = 1.0 * BETA[topic][word]/betasum;
			}
		}
		
		System.out.println("Step0 - Finish initialization");
		
		//Step 2: Run EM
		
		int totite = 0;
		double totconverged = 0;
		double oldlikelihood = 1;
		System.out.println("Step1 - Start EM.........................");
		while(((totconverged < 0) || (totconverged > conf.EM_CONVERGED) || totite < 2) && totite <= conf.EM_MAX_ITER){
			vemEstep vemE = new vemEstep(Numdoc, Numtopic, BETA, doc_sent_word, vocabulary, ALPHA);
			vemMstep vemM = new vemMstep(Numdoc, Numtopic);
			totite ++;
			double likelihood = vemE.runEstep();
			totconverged = Math.abs(likelihood - oldlikelihood)/oldlikelihood;
			oldlikelihood = likelihood;
			ALPHA = vemM.MstepALPHA(vemE.getsuffALPHA());
			BETA = vemM.MstepBETA(vemE.getsuffBETA());
			doc_topic = vemM.MstepDOCT(vemE.getGAMMA());
			sent_topic = vemM.MstepSENTT(vemE.getPHI());
			
			System.out.println("Step1 - Iteration: " + totite);
			System.out.println("       likelihood: " + oldlikelihood 
								+ " --- converge: " + totconverged 
								+ " --- alpha: " + ALPHA);
		}
		
		
		
		
		//Step 2: save beta
		try {
			CSVWriter writer = new CSVWriter(new FileWriter(beta_file));
	        String[] input = new String[Numtopic + 1];
	        //Header
	        input[0] = "Vocab";
			for(int topic = 0; topic < Numtopic; topic ++){
				input[topic + 1] = "Topic" + topic;
			}
			
			//Content
			for(String key : vocabulary.keySet()){
				input[0] = key;
				int ind = vocabulary.get(key);
				for(int topic = 0; topic < Numtopic; topic ++){
					input[topic + 1] = String.valueOf(BETA[topic][ind]);
				}
				writer.flush();
			    writer.writeNext(input);
			    writer.flush();
			}
			
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Finish writting beta.");
		
		try {
			CSVWriter writer = new CSVWriter(new FileWriter(sent_topic_file));
	        String[] input = new String[Numtopic + 5];
	        //Header
	        input[0] = "Acc";
	        input[1] = "date";
	        input[2] = "cik";
	        
			for(int topic = 0; topic < Numtopic; topic ++){
				input[topic + 5] = "Topic" + topic;
			}
			
			//Content
			for(int[] key : sent_topic.keySet()){
				input[0] = docinfo[key[0]][0];
		        input[1] = docinfo[key[0]][1];
		        input[2] = docinfo[key[0]][3];
		        input[3] = String.valueOf(key[0]);
		        input[4] = String.valueOf(key[1]);

				for(int topic = 0; topic < Numtopic; topic ++){
					input[topic + 5] = String.valueOf(sent_topic.get(key)[topic]);
				}
				writer.flush();
			    writer.writeNext(input);
			    writer.flush();
			}
			
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Finish writting sent_topic.");

		
		try {
			CSVWriter writer = new CSVWriter(new FileWriter(doc_topic_file));
	        String[] input = new String[Numtopic + 5];
	        //Header
	        input[0] = "Acc";
	        input[1] = "date";
	        input[2] = "cik";
	        input[3] = "doc";
	        input[4] = "sent";
	        
			for(int topic = 0; topic < Numtopic; topic ++){
				input[topic + 5] = "Topic" + topic;
			}
			
			//Content
			for(int doc = 0; doc < Numdoc; doc ++){
				input[0] = docinfo[doc][0];
		        input[1] = docinfo[doc][1];
		        input[2] = docinfo[doc][2];
		        input[3] = docinfo[doc][3];
		        input[4] = docinfo[doc][4];

				for(int topic = 0; topic < Numtopic; topic ++){
					input[topic + 5] = String.valueOf(doc_topic[doc][topic]);
				}
				writer.flush();
			    writer.writeNext(input);
			    writer.flush();
			}
			
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Finish writting doc_topic.");
		
				
	}
}
