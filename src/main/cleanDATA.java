package main;

public class cleanDATA {

	public String cleanString (String input){
		String result = "";
		String[] step1 = input.split(",");
		
		for(int i = 0; i < step1.length; i ++){
			String result1 = "";
			String[] step2 = step1[i].split("_");
			for(int j = 0; j < step2.length; j ++){
				if(!step2[j].isEmpty()){
//					System.out.println(step2[j]);
					if(result1.isEmpty()){
						result1 = step2[j];
					}else{
						result1 = result1 + "_" + step2[j];
					}
				}
			}
			
//			System.out.println(result1);
			if(result.isEmpty()){
				result =result1;
			}else{
				result = result + "," + result1;
			}			
		}		
		return(result);
	}
}
