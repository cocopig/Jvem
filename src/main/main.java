package main;

import java.sql.*;
import java.util.*;

import coco.lda.vem.vemMain;

public class main {
	
	//Parameters
	static int topicNum = 30;
	
	static int Ndocs;//vocabulary size, topic number, document number
	
	static String [] filing_number;
	static String [] filing_date;
	static String [] cik;
	
	int [][] doc_topic;//given document m, count times of topic k. M*K
	int [][] topic_vocab;//given topic k, count times of term t. K*V
	int [] doc_topicsum;//Sum for each row in doc_topic (actually is total number of words in this document)
	static int [] topic_vocabsum;//Sum for each row in topic_vocab
	
	
    //Topic index on each sentence on each doc doc_topic[0][0] indicates first sentence in first doc is assigned as topic x
	static String [][][] doc_sent_word; //Save word text for later loop.
	
	static Map<String, Integer> vocabulary = new HashMap<String, Integer>();
	static int V = 0; //number of vocabularies	
		
	static //Outputs
	int [] doc_sent_num; //Save number of sentences in each doc	
	static int[] doc_wordcount; //number of words in each doc
		
	public static void main(String args[]) {

		//Record Start time
		long Tstart = System.currentTimeMillis()/1000;
		
		List<List> index = new ArrayList<List>();
		
		//Initial what I can
		topic_vocabsum = new int[topicNum];
				
 		Connection conn = sql_conn.sqlconn();
 /**
  * Step 0: Get number of Documents		
  */
 		try{
 			Statement st = conn.createStatement();
 			ResultSet rs = st.executeQuery("SELECT COUNT(filing_number) FROM tm_risk_10k_subtitle "
 											+ "where subtitle != '' and filing_date < '2011-01-01' "
 											+ "limit 100");

 			while(rs.next()){
 				Ndocs = rs.getInt(1);
// 				Ndocs = 1;
 				doc_sent_num = new int [Ndocs];
 				doc_sent_word = new String [Ndocs][][];
 				doc_wordcount = new int[Ndocs];
 				
 				filing_number = new String [Ndocs];
 				filing_date = new String [Ndocs];
 				cik = new String [Ndocs];
 			}
 		} catch (Exception e){
 			e.printStackTrace();
 		}
 		
 		try{
 			Statement st = conn.createStatement();
 			ResultSet rs = st.executeQuery("SELECT * FROM tm_risk_10k_subtitle "
 											+ "where subtitle != '' and filing_date < '2011-01-01' ");
// 											+ "and filing_number = '0001193125-10-161874'");

			/**
			 * Step 1: Read each line and:
			 * 				Save filing_number, filing_date, cik, sic into index list
			 * 				Save vocabulary		
			 */
 			int doc_count = 0;
 			while (rs.next()){			
 				
 				filing_number[doc_count] = rs.getString("filing_number");
 				filing_date[doc_count] = rs.getString("filing_date");
 				cik[doc_count] = rs.getString("cik");
 				
 				String temp_cleanvalue = rs.getString("subtitle");
 				temp_cleanvalue = CleanData(temp_cleanvalue);
 				
 				//save sentence into temp_sentences array
 				String[] temp_sentences = temp_cleanvalue.split(",");
 				doc_sent_word[doc_count] = new String[temp_sentences.length][];
 				
 				doc_sent_num[doc_count] = temp_sentences.length;
 				doc_wordcount[doc_count] = 0;
 				
 				//Loop for each sentence
 				for(int ind1 = 0; ind1 < temp_sentences.length; ind1 ++){
 					String[] temp_words = temp_sentences[ind1].split("_");
 					doc_sent_word[doc_count][ind1] = new String[temp_words.length]; 					
 					doc_wordcount[doc_count] = doc_wordcount[doc_count] + temp_words.length;
 					
 					//loop for each words in one sentence
 					for(int ind2 = 0; ind2 < temp_words.length; ind2 ++){
 						
 						if(!vocabulary.keySet().contains(temp_words[ind2])){
 							//Add to vocabulary
 							vocabulary.put(temp_words[ind2], V);
 							V ++;		
 							doc_sent_word[doc_count][ind1][ind2] = temp_words[ind2];	

 						} else {
 							doc_sent_word[doc_count][ind1][ind2] = temp_words[ind2];
 						}						
 					}
 				}
				
 				//Finish this document
// 				System.out.println("Finish Document Initialization: " + doc_count);
 				
 				doc_count ++ ;
 				
 			}
 			System.out.println("-------- Step1 Finish Document Initialization --------");
 			System.out.println("number of vocab: " + vocabulary.size());
 			
 			conn.close();
 		} catch (Exception e){
 			e.printStackTrace();
 		}
 		
 		
 		//Run VEM and save topic_term_prob into csv file.
 		vemMain ldavem = new vemMain(Ndocs, topicNum, vocabulary, doc_sent_num, doc_wordcount, doc_sent_word);
		
		
		System.out.println("Run EM");
		ldavem.run_em();
		System.out.println("Finish EM, Start save beta");
		
		ldavem.get_beta("C:/Users/xzhu/Documents/vem_topic_term_0504_30t.csv");
		ldavem.get_document_topic("C:/Users/xzhu/Documents/vem_doc_topic_0504_30t.csv", 
									filing_number, filing_date, cik, doc_wordcount);
		
 		long Tend = (System.currentTimeMillis()/1000 - Tstart);
 		System.out.println("Done in " + Tend + " seconds");
 		
	}
	
		
	
	
	
	private static String CleanData(String orig){
		String[] temp_sentences = orig.split(",");
			
		String cleaned_doc = "";
		int firstsent = 1;
		
		for(int i = 0; i < temp_sentences.length; i ++){
			if(temp_sentences[i].isEmpty()){
				continue;
			} else if (firstsent == 1){
				firstsent = 0;
				String[] temp_words = temp_sentences[i].split("_");
				int firstword =  1;
				for(int j = 0; j < temp_words.length; j++){
					if(temp_words[j].isEmpty()){
						continue;
					} else if (firstword == 1){
						firstword = 0;
						cleaned_doc = cleaned_doc + temp_words[j];
					} else {
						cleaned_doc = cleaned_doc + "_" + temp_words[j];
					}
				}
			} else {
				String[] temp_words = temp_sentences[i].split("_");
				int firstword =  1;
				for(int j = 0; j < temp_words.length; j++){
					if(temp_words[j].isEmpty()){
						continue;
					} else if (firstword == 1){
						firstword = 0;
						cleaned_doc = cleaned_doc + "," + temp_words[j];
					} else {
						cleaned_doc = cleaned_doc + "_" + temp_words[j];
					}
				}
			}
		}
		return(cleaned_doc);
	}
}
