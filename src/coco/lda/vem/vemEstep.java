package coco.lda.vem;

import java.util.Map;

import org.apache.commons.math3.special.Gamma;

public class vemEstep extends vemMain{

	
	private double alpha_suff;
	private double[][] temp_beta;
	private double[][] phi;
	private double[] var_gamma;
	
	public vemEstep(int ndoc, int ntopic, Map<String, Integer> vocab, 
			int[] sentence, int[] wordcount, String[][][] dsw){
		super(num_doc, num_topic, vocabulary, doc_sent, doc_wordcount, doc_sent_word);
		
		alpha_suff = 0;
		temp_beta = new double[num_topic][vocabulary.size()];
		for(int i = 0; i < num_topic; i ++){
			for(int j = 0; j < vocabulary.size(); j ++){
				temp_beta[i][j] = 0;
			}
		}
	}

	
	/**
	 * Run e step
	 * loop for each document
	 * update sufficient (temp_tvp and alpha_suff)
	 * After estep, alpha and beta matrix need to be updated.
	 * @param like
	 * @return
	 */
	protected double runExpectation(){
		double likelihood = 0;
		for(int d = 0; d < num_doc; d ++){
			phi = this.initial_phi(d);
			var_gamma = this.initial_gamma(d);
			
			likelihood = likelihood + this.single_doc_inference(d);
			
			alpha_suff = this.single_doc_temp_alpha(alpha_suff);
			temp_beta = this.single_doc_temp_beta(temp_beta, d);
//			System.out.println("tempbeta: " + temp_beta[1][1]);
		}
		
		return(likelihood);
	}
	
	
	protected double get_tempALPHA(){
		return(this.alpha_suff);
	}
	
	protected double[][] get_tempBETA(){
		return(this.temp_beta);
	}
	
	//function should be applied on each doc
	private double single_doc_inference(int doc_label){

		
		//Set other parameters
		double likelihood = 0;
		double likelihood_old = 0;
		double phisum = 0;
		double converged = 1;
		int var_iter = 0;
		int num_sentence = phi[0].length;
		
		while((converged > conf.VAR_CONVERGED) && ((var_iter < conf.VAR_MAX_ITER) || (conf.VAR_MAX_ITER == -1))){
			var_iter ++;
			
			double[] phi_sent_sum = new double[num_topic];
			
			double digamma_sum = 0;
			double[] digamma = new double[num_topic];
			for(int i = 0; i < num_topic; i ++){
				digamma_sum = digamma_sum + var_gamma[i];
				digamma[i] = Gamma.digamma(var_gamma[i]);
				phi_sent_sum[i] = 0;
			}
			digamma_sum = Gamma.digamma(digamma_sum);
		
			//Update phi for each sentence, each topic
			for(int i = 0; i < num_sentence; i++){
				phisum = 0;	//used to normalize phi	
				
				//Update phi[topic][sentence]
				for(int j = 0; j < num_topic; j ++){				
					
					//Sent-LDA, calculate phi
					//phi = beta_sentence(multiply all word beta in this sentence) * exp(digamma - digamma(sum))
					double beta_sentence = 1;
					for(int w = 0; w < doc_sent_word[doc_label][i].length; w ++){
						int w_index = vocabulary.get(doc_sent_word[doc_label][i][w]);
						beta_sentence = beta_sentence * topic_vocab_prob[j][w_index];
					}
					
					//final phi[][]
					phi[j][i] = beta_sentence * Math.exp(digamma[j] - digamma_sum);					
					phisum = phisum + phi[j][i];						
				}		
				
				//Finish updating phi[][sentence] on all topics
				//Normalize phi
				for(int j = 0; j < num_topic; j ++){
					phi[j][i] = 1.0 * phi[j][i]/phisum;
					phi_sent_sum[j] = phi_sent_sum[j] + phi[j][i];
				}
				
			}
			
			//Update gamma
			for(int i = 0; i < num_topic; i ++){			
				var_gamma[i] = ALPHA + phi_sent_sum[i];
			}			
			
			//Check convergence, whether continue while() loop
			likelihood = compute_likelihood(doc_label, phi, var_gamma);
			converged = 1.0* Math.abs((likelihood_old - likelihood)/likelihood_old);
			likelihood_old = likelihood;
			
			System.out.println("Doc: " + doc_label + " Iteration: " + var_iter + "  Converged: " + converged + " likelihood: " + likelihood);
			
		}
		
		//return the converged likelihood for one document.
		return(likelihood);
	}
	
	
	
	
	private double single_doc_temp_alpha(double old_alpha){
		double re_alpha = old_alpha;
		double gamma_sum = 0;
		
		for(int i = 0; i < num_topic; i ++){
			gamma_sum = gamma_sum + var_gamma[i];
			re_alpha = re_alpha + Gamma.digamma(var_gamma[i]);					
		}
		re_alpha = re_alpha - num_topic * Gamma.digamma(gamma_sum);		
		return(re_alpha);
	}
	
	
	private double[][] single_doc_temp_beta(double[][] temp_beta, int doc_label){
		double[][] re_beta = temp_beta;
		double[] sum = new double[num_topic];
		
		for(int i = 0; i < num_topic; i ++){
			sum[i] = 0;
					
			//update re_beta and sum
			for(int j = 0; j < doc_sent[doc_label]; j ++){
				for(int w = 0; w < doc_sent_word[doc_label][j].length; w ++){
					int w_index = vocabulary.get(doc_sent_word[doc_label][j][w]);
					re_beta[i][w_index] = re_beta[i][w_index] + phi[i][j];
					sum[i] = sum[i] + re_beta[i][w_index];
				}
			}			
		}	
		return(re_beta);
	}
	
	

	
	
	private double[] initial_gamma (int doc_label){
		double[] var_gamma = new double[num_topic];
		for(int i = 0; i < num_topic; i ++){
			var_gamma[i] = ALPHA + 1.0*doc_sent[doc_label]/num_topic;
		}		
		return(var_gamma);
	}
	
	
	
	
	//initial phi, var_gamma, digamma_gam for each inference
	//Used in single_doc_inference
	private double[][] initial_phi (int doc_label){
		double[][] phi = new double[num_topic][doc_sent_word[doc_label].length];

		int num_sentence = doc_sent[doc_label];		
		for(int i = 0; i < num_topic; i ++){	
			for(int j = 0; j < num_sentence; j ++){
				phi[i][j] = 1.0/num_topic;
			}
		}	
		
		return(phi);
	}
	
	
	
	
	//compute_likelihood from Blei(2003) and Bao(2014) format, adjusted for sent-LDA
	private double compute_likelihood(int doc_label, double[][] phi, double[] var_gamma){
		double likelihood = 0;
		int num_sentence = doc_sent[doc_label];
		double[] dig = new double[num_topic];
		double digsum = 0;
		double var_gamma_sum = 0;
		
		for(int i = 0 ; i < num_topic; i ++){
			dig[i] = Gamma.digamma(var_gamma[i]);
			var_gamma_sum = var_gamma_sum + var_gamma[i];
		}
		digsum = Gamma.digamma(var_gamma_sum);
		
		//Component 1
		likelihood = Gamma.logGamma(ALPHA * num_topic)
						- num_topic * Gamma.logGamma(ALPHA)
						- Gamma.logGamma(var_gamma_sum);
		
		for(int i = 0 ; i < num_topic; i ++){
			
			//Component 2
			likelihood = likelihood 
							+ (ALPHA - 1) * (dig[i] - digsum)
							+ Gamma.logGamma(var_gamma[i])
							- (var_gamma[i] - 1)*(dig[i] - digsum);
			
			//Different of Sent-LDA compared with original
			for(int j = 0 ; j < num_sentence; j ++){		
				
				if (phi[i][j] > 0){
					//Component 3
					likelihood = likelihood + phi[i][j] * (dig[i] - digsum)
							+ phi[i][j] * Math.log(phi[i][j]);	
					
					for(int w = 0; w < doc_sent_word[doc_label][j].length; w ++){
						int w_index = vocabulary.get(doc_sent_word[doc_label][j][w]);
						//Component 4
						likelihood = likelihood + Math.log(topic_vocab_prob[i][w_index]) * phi[i][j];
					}
				}
			}
		}	
		return(likelihood);
	}
	
	
}
