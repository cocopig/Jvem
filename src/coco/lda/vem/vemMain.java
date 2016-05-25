package coco.lda.vem;

import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.opencsv.CSVWriter;

import coco.lda.conf.vemConfig;

public class vemMain {
	
	protected static int num_doc;  //number of document, from corpus
	protected static int num_topic;  //number of topic, set manually
	protected static Map<String, Integer> vocabulary = new HashMap<String, Integer>(); //vocabulary (unique words in all docs)
	protected static int num_vocab;		//size of vocabulary
	protected static int[] doc_sent;	    //number of sentence
	protected static int[] doc_wordcount; //number of words
	protected static String[][][] doc_sent_word;   //[doc][sentence][words], save word text, use vocabulary.get("word") to get word index.
	
	protected static double[][] topic_vocab_prob;  //beta in the paper
	protected static double[] topic_vocab_prob_sum;
	
	protected static double ALPHA;
	protected static double [][] final_gamma;
	
	vemConfig conf = new vemConfig();
	
	//Constructor
	public vemMain(int ndoc, int ntopic, Map<String, Integer> vocab, 
					int[] sentence, int[] wordcount, String[][][] dsw){
		
		//Initial used parameters
		num_doc = ndoc;
		num_topic = ntopic;
		num_vocab = vocabulary.size();
		vocabulary = vocab;
		doc_sent = sentence;
		doc_wordcount = wordcount;
		doc_sent_word = dsw;
		
		//Initial beta matrix and gamma matrix
		topic_vocab_prob = new double[num_topic][num_vocab];
		topic_vocab_prob_sum = new double[num_topic];
		final_gamma = new double[num_doc][num_topic];
		Random r = new Random();
		r.setSeed(4);
		for(int i = 0; i < num_topic; i ++){
			topic_vocab_prob_sum[i] = 0;
			for(int j = 0; j < num_vocab; j ++){
				topic_vocab_prob[i][j] = 1/num_vocab + r.nextDouble();
//				topic_vocab_prob[i][j] = 1/num_vocab;
				topic_vocab_prob_sum[i] = topic_vocab_prob_sum[i] + topic_vocab_prob[i][j];
			}
			for(int j = 0; j < num_vocab; j ++){
				topic_vocab_prob[i][j] = 1.0 * topic_vocab_prob[i][j]/topic_vocab_prob_sum[i];
			}
			for(int j = 0; j < num_doc; j ++){
				final_gamma[j][i] = 0;
			}
		}		
		
		ALPHA = conf.INITIAL_ALPHA;
	}
	

	
	public void run_em(){
		
		//Expectation Maximization
		double likelihood;
		double likelihood_old = 0;
		double converged = 1;
		int ite = 0;
		
		//Initial 2 classes
		vemEstep Estep = new vemEstep(num_doc, num_topic, vocabulary, doc_sent, doc_wordcount, doc_sent_word);
		vemMstep Mstep = new vemMstep(num_doc, num_topic, vocabulary, doc_sent, doc_wordcount, doc_sent_word);
		
		while((converged > conf.EM_CONVERGED) && (ite <= conf.EM_MAX_ITER)){
			ite ++;			
			
			//E step
			//calculate likelihood, update alpha_suff & temp_tvp
			//alpha_suff and temp_tvp will be used in Mstep
			likelihood = Estep.runExpectation();

			
			//M step
			//log_topic_vocab_prob will be updated in lda_mle
			ALPHA = Mstep.update_alpha(conf.ESTIMATE_ALPHA, Estep.get_tempALPHA());
			topic_vocab_prob = Mstep.update_beta(Estep.get_tempBETA());
//			System.out.println(topic_vocab_prob[1][1]);
			final_gamma = Mstep.update_gamma(Estep.get_tempGAMMA(), conf.GAMMA_THRE);
			
			//Check convergence
			converged = 1.0 * Math.abs((likelihood_old - likelihood) / likelihood_old);
//			if(converged < 0){
//				conf.VAR_MAX_ITER = conf.VAR_MAX_ITER * 2;
//			}
			likelihood_old = likelihood;
			System.out.println("new likelihood: " + likelihood);
			System.out.println("-------------------- EM Iteration: " + ite + "  Converged: " + converged + " ---------------------");

		}

	}
	
	
	public void get_beta(String topic_term_file){
		CSVWriter writer;
		try {
			writer = new CSVWriter(new FileWriter(topic_term_file));
			String[] title = new String[num_topic + 1];
			title[0] = "Vocabulary";
			for(int i = 0; i < num_topic; i ++){
				title[i+1] = String.valueOf("Topic" + i);
			}
			writer.flush();
			writer.writeNext(title);
			writer.flush();
			
			for(String key : vocabulary.keySet()){
				String[] csvinput = new String[num_topic + 1];
				csvinput[0] = key;
				for(int i = 0; i < num_topic; i ++){
					topic_vocab_prob[i][vocabulary.get(key)] = topic_vocab_prob[i][vocabulary.get(key)];
					csvinput[i+1] = String.valueOf(topic_vocab_prob[i][vocabulary.get(key)]);
				}
				writer.flush();
				writer.writeNext(csvinput);
				writer.flush();
			}
			
		} catch(Exception e){
			e.printStackTrace();
		}		

	}
	
	
	public void get_document_topic(String file_path, String[] fnumber, String[] fdate, String[] cik, int[] wc){
		
		CSVWriter writer;
		try {
			writer = new CSVWriter(new FileWriter(file_path));
			String[] title = new String[num_topic + 4];
			title[0] = "FilingNumber";
			title[1] = "FilingDate";
			title[2] = "cik";
			title[3] = "WordCount";
			
			for(int i = 0; i < num_topic; i ++){
				title[i+4] = String.valueOf("Topic" + i);
			}
			writer.flush();
			writer.writeNext(title);
			writer.flush();
			
			for(int j = 0; j < num_doc; j ++){
				String[] csvinput = new String[num_topic + 4];
				csvinput[0] = String.valueOf(fnumber[j]);
				csvinput[1] = String.valueOf(fdate[j]);
				csvinput[2] = String.valueOf(cik[j]);
				csvinput[3] = String.valueOf(wc[j]);
				
				for(int i = 0; i < num_topic; i ++){
					csvinput[i+4] = String.valueOf(final_gamma[j][i]);
				}
				writer.flush();
				writer.writeNext(csvinput);
				writer.flush();
			}
			
		} catch(Exception e){
			e.printStackTrace();
		}	
		
	}
	
	
	
}








