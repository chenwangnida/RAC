/*
 * This software and all files contained in it are distrubted under the MIT license.
 * 
 * Copyright (c) 2013 Cogito Learning Ltd
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package expressionParser;

/**
 * Test the Parser
 */
public class Test
{

  /**
   * The main method to test the functionality of the parser
   */
  public static void main(String[] args)
  {
    
    String exprstr = "((normalizedItemMem * (((leftVmMem + normalizedItemMem) " +
            "+ normalizedItemMem) - (leftVmMem + normalizedItemCpu))) + (((leftVmCpu " +
            "/ (leftVmMem + normalizedItemMem)) / normalizedVmCpuOverhead) + ((leftVmMem " +
            "+ normalizedItemMem) - (leftVmMem + (leftVmCpu / leftVmCpu))))) * ((((leftVmMem" +
            " + normalizedItemCpu) + (normalizedItemCpu * (leftVmCpu / normalizedVmCpuOverhead))) " +
            "- leftVmMem) * ((leftVmMem + (normalizedItemCpu * (leftVmCpu / normalizedVmCpuOverhead))) " +
            "- leftVmMem))";
    if (args.length>0) exprstr = args[0];
    
    Parser parser = new Parser();
    ExpressionNode expr = parser.parse(exprstr);
    try
    {
      expr.accept(new SetVariable("normalizedItemMem", 1));
      expr.accept(new SetVariable("leftVmMem", 2));
      expr.accept(new SetVariable("leftVmCpu", 3));
      expr.accept(new SetVariable("normalizedItemCpu", 4));
      expr.accept(new SetVariable("normalizedVmCpuOverhead", 0));
      System.out.println("The value of the expression is "+expr.getValue());
      
    }
    catch (ParserException e)
    {
      System.out.println(e.getMessage());
    }
    catch (EvaluationException e)
    {
      System.out.println(e.getMessage());
    }

  }
}