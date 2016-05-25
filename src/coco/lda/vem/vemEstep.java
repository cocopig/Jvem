package coco.lda.vem;

import java.util.HashMap;

import org.apache.commons.math3.special.Gamma;

import coco.lda.conf.*;

public class vemEstep {

	private int Ndoc, Ntopic;
	private String[][][] dsw;
	private double[][] beta;
	private double alpha;
	private double[][] doc_gamma;
	private double[][][] sent_phi;
	vemConfig conf = new vemConfig();
	HashMap <String, Integer> vocabulary;
	
	private double[][] suffBETA;
	private double suffALPHA = 0;
	private double[][] phi;
	private double[] gamma;
		
	public vemEstep(int Numdoc, 
					int Numtopic, 
					double[][] probwt, 
					String[][][] doc_sent_word, 
					HashMap <String, Integer> vocab,
					double oldalpha){
		Ndoc = Numdoc;
		Ntopic = Numtopic;
		dsw = doc_sent_word;
		beta = probwt;
		alpha = oldalpha;
		vocabulary = vocab;
		doc_gamma = new double[Ndoc][Ntopic];
		sent_phi = new double[Ndoc][][];
				
		suffBETA = new double[Ntopic][vocabulary.size()];
		for(int topic = 0; topic < Ntopic; topic ++ ){
			for(int v = 0; v < vocabulary.size(); v ++){
				suffBETA[topic][v] = 0;
			}
		}
	}
	
	
	public double runEstep(){
		//Step 1: get total likelihood (sum of likelihood of all docs)
		double tot_likelihood = 0;
		for(int doc = 0; doc < Ndoc; doc ++){
			//Step 1: Initial parameter
			int Nsent = dsw[doc].length;
			phi = this.initialPHI(Ntopic, Nsent);
			gamma = this.initialGAMMA(Ntopic, Nsent);
			
			double oldlikelihood = 1;
			double converged = 100;
			int single_ite = 0;
			
			//Step 2: Get converged single likelihood
			while((converged > conf.VAR_CONVERGED) && ((single_ite < conf.VAR_MAX_ITER) || (conf.VAR_MAX_ITER == -1))){
				single_ite ++;
				double[] gamma_component = new double[Ntopic];
				double gamma_sum = 0;
				for(int g = 0; g < gamma.length; g++){
					gamma_sum = gamma_sum + gamma[g];
				}
				for(int g = 0; g < gamma.length; g ++){
					gamma_component[g] = Gamma.digamma(gamma[g]) - Gamma.digamma(gamma_sum);
				}
				
				double likelihood = this.single_inference(dsw[doc], gamma_component);
				converged = 1.0 * Math.abs(likelihood - oldlikelihood)/oldlikelihood;
				oldlikelihood = likelihood;
			}
			tot_likelihood = tot_likelihood + oldlikelihood;
			
			//gamma and phi are final paremeter (after convergence check).
			//They are used to update parameters (alpha and beta)
			//Step 3: Update sufficient parameters
			//Update suff ALPHA
			double gamma_sum = 0;
			for(int topic = 0; topic < Ntopic; topic ++){
				gamma_sum = gamma_sum + gamma[topic]; 
				suffALPHA = suffALPHA + Gamma.digamma(gamma[topic]);
			}
			suffALPHA = suffALPHA - Ntopic * Gamma.digamma(gamma_sum);
			
			//Update suff Beta (in M step, beta needs to be normalized).
			for(int sent = 0; sent < dsw[doc].length; sent ++){
				for(int word = 0; word < dsw[doc][sent].length; word ++){
					for(int topic = 0; topic < Ntopic; topic ++){
						int vocab_index = vocabulary.get(dsw[doc][sent][word]);
						suffBETA[topic][vocab_index] = suffBETA[topic][vocab_index] + phi[topic][sent];
					}
				}			
			}	
			
			doc_gamma[doc] = gamma;
			sent_phi[doc] = phi;
		}	
		
		return(tot_likelihood);
	}
	
	
	public double[][] getsuffBETA(){
		return(suffBETA);
	}
	
	public double getsuffALPHA(){
		return(suffALPHA);
	}
	
	public double[][] getGAMMA(){
		return(doc_gamma);
	}
	
	public double[][][] getPHI(){
		return(sent_phi);
	}
	
	private double[][] initialPHI (int Ntopic, int Nsent){
		//For one sentence, sum_phi = 1
		//Initial phi evenly
		double[][] phi = new double[Ntopic][Nsent];
		for(int topic = 0; topic < Ntopic; topic ++){
			for(int sent = 0; sent < Nsent; sent ++){
				phi[topic][sent] = 1.0;
			}
		}
		return(phi);
	}
	
	private double[] initialGAMMA (int Ntopic, int Nsent){
		double[] gamma = new double[Ntopic];
		for(int topic = 0; topic < Ntopic; topic ++){
			gamma[topic] = alpha + 1.0 * Nsent/Ntopic;
		}
		return(gamma);
	}
	
	private double single_inference(String[][] dsw_sent, double[] gamma_component){
		double single_likelihood = 1;

		for(int topic = 0; topic < Ntopic; topic ++){
			gamma[topic] = alpha;
		}
		
		
		//Step 1: update phi;
		for(int sent = 0; sent < dsw_sent.length; sent ++){
			double phi_sum = 0;
			for(int topic = 0; topic < Ntopic; topic ++){
				for(int word = 0 ; word < dsw_sent[sent].length; word ++){
					if(word == 0){
						phi[topic][sent] = 1.0 * beta[topic][vocabulary.get(dsw_sent[sent][word])];
					}else{
						phi[topic][sent] = 1.0 * phi[topic][sent] * beta[topic][vocabulary.get(dsw_sent[sent][word])];
					}
					
				}
				phi[topic][sent] = phi[topic][sent] * Math.exp(gamma_component[topic]);
				if(phi[topic][sent] == 0){
					phi[topic][sent] = Math.pow(0.1, 200);
				}
				phi_sum = phi_sum + phi[topic][sent];
				
			}
			
			//Step 2: Normalize Ntopic phis on current sentence.
			//        And update gamma;
			
			for(int topic = 0; topic < Ntopic; topic ++){
				phi[topic][sent] = 1.0 * phi[topic][sent]/phi_sum;
				gamma[topic] = gamma[topic] + phi[topic][sent];
			}
					
		}
		
		single_likelihood = this.single_likelihood(dsw_sent);
		return(single_likelihood);
	}
	
	private double single_likelihood(String[][] dsw_sent){
		//Part 1.
		double result = Gamma.logGamma(alpha * Ntopic) + Ntopic * Gamma.gamma(alpha);
		double gamma_sum = 0;
		for(int topic = 0; topic < Ntopic; topic ++){
			gamma_sum = gamma_sum + gamma[topic];
		}
		
		//Part 2.
		result = result + Gamma.logGamma(gamma_sum);
		
		for(int topic = 0; topic < Ntopic; topic ++){
			//Part 2.
			result = result + (alpha - 1) * (Gamma.digamma(gamma[topic]) - Gamma.digamma(gamma_sum))
							+ Gamma.logGamma(gamma[topic])
							+ (gamma[topic] - 1) * (Gamma.digamma(gamma[topic]) - Gamma.digamma(gamma_sum));
			
			//Part 3.
			for(int sent = 0; sent < dsw_sent.length; sent ++){
				double temp = 0;
				for(int word = 0; word < dsw_sent[sent].length; word ++){
					temp = temp + beta[topic][vocabulary.get(dsw_sent[sent][word])];
				}
				result = result 
						+ phi[topic][sent] * (Gamma.digamma(gamma[topic]) - Gamma.digamma(gamma_sum))
						+ phi[topic][sent] * temp
						+ phi[topic][sent] * Math.log(phi[topic][sent]);
			}
		}
		return(result);
	}
	
	
	
}







