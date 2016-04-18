package coco.lda.vem;

import java.util.Map;

import org.apache.commons.math3.special.Gamma;

public class vemMstep extends vemMain{

	public vemMstep(int ndoc, int ntopic, Map<String, Integer> vocab, 
			int[] sentence, int[] wordcount, String[][][] dsw){
		super(num_doc, num_topic, vocabulary, doc_sent, doc_wordcount, doc_sent_word);

	}
	
	
	protected double update_alpha(int estimate_alpha, double alpha_suff){
		double new_alpha = 0;
		if(estimate_alpha == 1){
			new_alpha = opt_alpha(alpha_suff);
		}
		return(new_alpha);
	}
	
	protected double[][] update_beta(double[][] temp_beta){
		double[][] new_beta = new double[num_topic][num_vocab];
		double[] sum = new double[num_topic];
		for(int i = 0; i < num_topic; i ++){
			sum[i] = 0;
			for(int j = 0; j < num_vocab; j ++){
				sum[i] = sum[i] + temp_beta[i][j];
			}
			for(int j = 0; j < num_vocab; j ++){
				if(temp_beta[i][j] > 0){
					new_beta[i][j] = 1.0 * temp_beta[i][j]/sum[i];
				} else {
					new_beta[i][j] = -100;
				}				
			}			
		}
		
		return(new_beta);
	}
	
	
	private double opt_alpha(double alpha_suff){

		double a, log_a, init_a = 100;
		double f, df, d2f;
		int iter = 0;
		
		log_a = Math.log(init_a);
		do{
			iter ++;
			a = Math.exp(log_a);
			f = num_doc * (Gamma.logGamma(a * num_topic) - num_topic * Gamma.logGamma(a))
					+ (a - 1) * alpha_suff;
			df = num_doc * (num_topic * Gamma.digamma(a * num_topic) - num_topic * Gamma.digamma(a))
					+ alpha_suff;
			d2f = num_doc * (num_topic * num_topic * Gamma.trigamma(a * num_topic) 
					- num_topic * Gamma.trigamma(a));
			
			log_a = log_a - df/(d2f * a + df);
			
		}while(Math.abs(df) > conf.NEWTON_THRESH && (iter < conf.MAX_ALPHA_ITER));
		
		return(Math.exp(log_a));
	}
}


