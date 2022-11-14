package expressionParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ReadExpression {
    StringBuilder contentBuilder = new StringBuilder();
    String path;

    public ReadExpression(String path){
        this.path = path;
    }
//TODO sort out the path
    public String readExpFrom(int run){
        
        path += run + ".txt";
        
        System.out.println("load rule from path:"+ path);
        BufferedReader br = null;
        try{
            br = new BufferedReader((new FileReader(path)));
        } catch (IOException e){
            e.printStackTrace();
        }
        String myExpression;

        try {
            while ((myExpression = br.readLine()) != null) {
                contentBuilder.append(myExpression);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }
}
