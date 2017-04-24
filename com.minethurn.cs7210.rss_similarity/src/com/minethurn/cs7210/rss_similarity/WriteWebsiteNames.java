/**
 *
 */
package com.minethurn.cs7210.rss_similarity;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.opencsv.CSVWriter;

/**
 * @author Steve Betts
 */
public class WriteWebsiteNames
{

   /**
    * @param args
    * @throws IOException
    * @throws FileNotFoundException
    */
   public static void main(final String[] args) throws FileNotFoundException, IOException
   {
      // prepare to parse the command line
      final Options options = new Options();
      options.addOption(Option.builder("file").argName("filename").hasArg().desc("the input file name").build());
      options.addOption(Option.builder("out").argName("filename").hasArg().desc("the output file name").build());

      final CommandLineParser parser = new DefaultParser();
      CommandLine cmdLine;
      try
      {
         cmdLine = parser.parse(options, args);
      }
      catch (@SuppressWarnings("unused") final ParseException e)
      {
         final HelpFormatter formatter = new HelpFormatter();
         formatter.printHelp("bf", options);
         return;
      }

      final String inputFilename = cmdLine.getOptionValue("file", "rss.txt");
      final String outputFilename = cmdLine.getOptionValue("out", "websiteTypes.csv");

      try (final BufferedReader reader = new BufferedReader(new FileReader(inputFilename));
            final CSVWriter writer = new CSVWriter(new FileWriter(outputFilename)))
      {
         String line;
         final String[] out = new String[2];
         int count = 0;

         while ((line = reader.readLine()) != null)
         {
            out[0] = SimilarityApp.getWebsiteName(line);
            out[1] = "Alt";

            writer.writeNext(out);
            count += 1;
         }

         System.out.println("wrote " + count + " lines");
      }
   }

}
