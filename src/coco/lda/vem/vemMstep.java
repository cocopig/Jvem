package coco.lda.vem;

import java.util.HashMap;

import org.apache.commons.math3.special.Gamma;

import coco.lda.conf.*;;

public class vemMstep {

	private int Ndoc, Ntopic;


	
	public vemMstep(int Numdoc, int Numtopic) {
		// TODO Auto-generated constructor stub
		Ndoc = Numdoc;
		Ntopic = Numtopic;
	}

	public double MstepALPHA(double alpha_suff){
		double newalpha = 0;
		
		vemConfig conf = new vemConfig();
		int alphaite = 0;
		double f, df, d2f;
		double a, init_a = 100;
		double log_a = Math.log(init_a);
		do{
			alphaite ++;
			a = Math.exp(log_a);
			f = Ndoc * (Gamma.logGamma(Ntopic * a) - Ntopic * Gamma.logGamma(a)) + (a - 1) * alpha_suff;
			df = Ndoc * (Ntopic * Gamma.digamma(Ntopic * a) - Ntopic * Gamma.digamma(a)) + alpha_suff;
			d2f = Ndoc * (Ntopic * Ntopic * Gamma.trigamma(Ntopic * a) - Ntopic * Gamma.trigamma(a));
			log_a = log_a - df/(d2f * a + df);
		}while((Math.abs(df) > conf.NEWTON_THRESH) && (alphaite < conf.MAX_ALPHA_ITER));
		
		newalpha = Math.exp(log_a);
		return(newalpha);
	}
	
	public double[][] MstepBETA(double[][] suffBETA){
		double[] beta_summ = new double[suffBETA.length];
		
		for(int topic = 0; topic < suffBETA.length; topic ++){		
			beta_summ[topic] = suffBETA[topic][0];
			for(int word = 1; word < suffBETA[0].length; word ++) {
				beta_summ[topic] = beta_summ[topic] + suffBETA[topic][word];
				
			}
		}
	
		double[][] newbeta = suffBETA;
		for(int topic = 0; topic < suffBETA.length; topic ++){
			for(int word = 0; word < suffBETA[0].length; word ++){
				newbeta[topic][word] = 1.0 * suffBETA[topic][word]/beta_summ[topic];
			}			
		} 	
		
		return(newbeta);
	}

	public double[][] MstepDOCT(double[][] doc_gamma){
		double[][] result = doc_gamma;
		double[] gamma_summ = new double[doc_gamma.length];
		
		for(int doc = 0; doc < doc_gamma.length; doc ++){		
			gamma_summ[doc] = doc_gamma[doc][0];
			for(int topic = 1; topic < doc_gamma[0].length; topic ++) {
				gamma_summ[doc] = gamma_summ[doc] + doc_gamma[doc][topic];				
			}
		}
	
		for(int doc = 0; doc < doc_gamma.length; doc ++){
			for(int topic = 1; topic < doc_gamma[0].length; topic ++) {
				result[doc][topic] = 1.0 * doc_gamma[doc][topic]/gamma_summ[doc];
			}			
		} 
		return(result);
	}
	
	public HashMap<int[], double[]> MstepSENTT(double[][][] sent_phi){
		HashMap<int[], double[]> result = new HashMap<int[], double[]>();

		for(int doc = 0; doc < sent_phi.length; doc ++){
			for(int s = 0; s < sent_phi[doc][0].length; s ++){
				int[] key = {doc, s};
				double[] value = new double[Ntopic];
				double sum = 0;
				for(int t = 0; t < Ntopic; t ++){
					sum = sum + sent_phi[doc][t][s];
				}
				for(int t = 0; t < Ntopic; t ++){
					value[t] = 1.0 * sent_phi[doc][t][s]/sum;
				}

				result.put(key, value);
			}
		}
		return(result);
	}

}
