/**
 *
 */
package com.minethurn.cs7210.rss_similarity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

/**
 * Read the distance file and generate some statistics about the numbers
 */
public class StoryStats
{
   /** the number of columns we expect to find in the output */
   public static final int IN_ID1 = 0;
   /** the number of columns we expect to find in the output */
   public static final int IN_ID2 = 1;
   /** the number of columns we expect to find in the output */
   public static final int IN_URL1 = 0;
   /** the number of columns we expect to find in the output */
   public static final int IN_URL2 = 1;
   /** the number of columns we expect to find in the output */
   public static final int IN_TITLE1 = 2;
   /** the number of columns we expect to find in the output */
   public static final int IN_TITLE2 = 3;
   /** the number of columns we expect to find in the output */
   public static final int IN_TITLE_DISTANCE = 4;
   /** the number of columns we expect to find in the output */
   public static final int IN_TIME_DISTANCE = 5;
   /** the number of columns we expect to find in the output */
   public static final int IN_START_TIME = 6;
   /** the number of columns we expect to find in the output */
   public static final int IN_END_TIME = 7;
   /** the number of columns we expect to find in the output */
   public static final int IN_COUNT = 8;

   /** the total time of the graph */
   public static final int OUT_DURATION = 0;
   /** the average time between edges */
   public static final int OUT_DURATION_AVERAGE = 1;
   /** the average time between edges */
   public static final int OUT_DURATION_STDEV = 2;
   /** the size of the graph */
   public static final int OUT_EDGE_COUNT = 3;
   /** the average similarity of the graph */
   public static final int OUT_SIMILARITY_AVERAGE = 4;
   /** the average similarity of the graph */
   public static final int OUT_SIMILARITY_STDEV = 5;
   /** the filename of the graph */
   public static final int OUT_FILE = 6;
   /** the number of columns in the output table */
   public static final int OUT_COUNT = 7;

   private static DescriptiveStatistics storyStartStats = new DescriptiveStatistics();
   private static DescriptiveStatistics storyDuration = new DescriptiveStatistics();
   private static DescriptiveStatistics overallSimilarityStats = new DescriptiveStatistics();
   private static DescriptiveStatistics overallDurationStats = new DescriptiveStatistics();
   private static DescriptiveStatistics storySimilarityStats = new DescriptiveStatistics();

   /**
    * @param line
    */
   private static void captureStats(final String[] line)
   {
      try
      {
         final double startTime = Double.parseDouble(line[IN_START_TIME]);
         if (startTime > 0.0)
         {
            storyStartStats.addValue(startTime);
            overallSimilarityStats.addValue(startTime);
         }
         else
         {
            System.err.println("skipping time " + line[IN_START_TIME]);
         }
      }
      catch (final NumberFormatException e)
      {
         e.printStackTrace();
      }

      try
      {
         final double distance = Double.parseDouble(line[IN_TIME_DISTANCE]);
         if (distance > 0.0)
         {
            storyDuration.addValue(distance);
            overallDurationStats.addValue(distance);
         }
      }
      catch (final NumberFormatException e)
      {
         e.printStackTrace();
      }

      try
      {
         final double distance = Double.parseDouble(line[IN_TITLE_DISTANCE]);
         if (distance > 0.0)
         {
            storySimilarityStats.addValue(distance);
            overallSimilarityStats.addValue(distance);
         }
      }
      catch (final NumberFormatException e)
      {
         e.printStackTrace();
      }
   }

   /**
    * @param args
    */
   public static void main(final String[] args)
   {
      // prepare to parse the command line
      final Options options = new Options();
      // options.addOption(Option.builder("file").argName("filename").hasArg().desc("the input file name").build());
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

      // ok, get parsed results from command line
      // final String inputFilename = cmdLine.getOptionValue("file", "input.csv");
      final String outputFilename = cmdLine.getOptionValue("out", "stats.csv");

      String[] line;
      // ok, start the processing
      try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(outputFilename))))
      {
         // find all matching files
         final Pattern p = Pattern.compile("distance.*csv");
         final File[] files = new File(".").listFiles((FilenameFilter) (dir, name) ->
         {
            final Matcher m = p.matcher(name);
            return (m.matches());
         });

         overallStart(writer);

         // now for all the matching files, generate some stats
         for (final File f : files)
         {
            storyStart(writer);

            try (final CSVReader reader = new CSVReader(new FileReader(f)))
            {
               while ((line = reader.readNext()) != null)
               {
                  captureStats(line);
               }
            }
            catch (final IOException e)
            {
               e.printStackTrace();
            }
            storyEnd(writer, f.getName());
         } // for f in files

         // ok, dump the overall stats
         overallEnd(writer);
      }
      catch (final IOException e)
      {
         e.printStackTrace();
      }
   }

   /**
    * @param writer
    */
   private static void overallEnd(final CSVWriter writer)
   {
      // TODO output the story statistics
      final double min = overallSimilarityStats.getMin();
      final double max = overallSimilarityStats.getMax();
      final long count = overallSimilarityStats.getN();

      final String[] line = new String[OUT_COUNT];
      line[OUT_DURATION_AVERAGE] = Double.toString(overallSimilarityStats.getMean());
      line[OUT_DURATION_STDEV] = Double.toString(overallSimilarityStats.getStandardDeviation());
      line[OUT_SIMILARITY_AVERAGE] = Double.toString(storySimilarityStats.getMean());
      line[OUT_SIMILARITY_STDEV] = Double.toString(storySimilarityStats.getStandardDeviation());
      line[OUT_EDGE_COUNT] = Long.toString(count);
      line[OUT_DURATION] = Double.toString(max - min);
      line[OUT_FILE] = "ALL";

      writer.writeNext(line);
   }

   /**
    * @param writer
    *        TODO
    */
   private static void overallStart(final CSVWriter writer)
   {
      final String[] line = new String[OUT_COUNT];
      line[OUT_DURATION] = "Duration";
      line[OUT_DURATION_AVERAGE] = "Ave Duration";
      line[OUT_DURATION_STDEV] = "Duration Std Dev";
      line[OUT_EDGE_COUNT] = "Edge Count";
      line[OUT_FILE] = "Filename";
      line[OUT_SIMILARITY_AVERAGE] = "Ave Similarity";
      line[OUT_SIMILARITY_STDEV] = "Similarity Std Dev";

      writer.writeNext(line);
   }

   /**
    * @param writer
    * @param f
    *        the file that we just finished
    */
   private static void storyEnd(final CSVWriter writer, final String f)
   {
      // TODO output the story statistics
      final double min = storyStartStats.getMin();
      final double max = storyStartStats.getMax();
      final long count = storyStartStats.getN();

      final String[] line = new String[OUT_COUNT];
      line[OUT_DURATION_AVERAGE] = Double.toString(storyDuration.getMean());
      line[OUT_DURATION_STDEV] = Double.toString(storyDuration.getStandardDeviation());
      line[OUT_SIMILARITY_AVERAGE] = Double.toString(storySimilarityStats.getMean());
      line[OUT_SIMILARITY_STDEV] = Double.toString(storySimilarityStats.getStandardDeviation());
      line[OUT_EDGE_COUNT] = Long.toString(count);
      line[OUT_DURATION] = Double.toString(max - min);
      line[OUT_FILE] = f;

      writer.writeNext(line);
   }

   /**
    * @param writer
    */
   private static void storyStart(final CSVWriter writer)
   {
      storyStartStats.clear();
   }

}
