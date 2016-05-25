package main;

import java.util.Random;

import org.apache.commons.math3.special.Gamma;

public class test {
	public static double a = 2.5;
	public static void main(String args[]) {
		int[][] a = new int[2][3];
		int[] b = new int[3];
		b[0] = 1;
		b[1] = 2;
		b[2] = 3;
		a[0] = b;
		for(int i = 0; i < 3; i ++){
			System.out.println(a[0][i]);
		}
		
	}
	
	public static void calculation(){
		a = a + 3.4;
	}
}
