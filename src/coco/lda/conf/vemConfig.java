package coco.lda.conf;

public class vemConfig {
	
	//whether we estimate/update alpha? yes(1)/no(0)
	public int ESTIMATE_ALPHA = 1;
	
	//initial alpha value
	public double INITIAL_ALPHA = 1.0;
	
	//initial iteration and convergence settings
	public int VAR_MAX_ITER = 100;
	public double VAR_CONVERGED = 0.00002;
	public int EM_MAX_ITER = 100;
	public double EM_CONVERGED = 0.00001;
	
	//initial parameters in opt_alpha()
	public double NEWTON_THRESH = 0.00001;
	public int MAX_ALPHA_ITER = 1000;
	
	//
	public double GAMMA_THRE = 0.01;
}
