/**
 *
 */
package com.minethurn.cs7210.rss_similarity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

/**
 *
 */
public class BfMain
{

   /** the news feed that gave us the article of the article */
   public static final int IN_URL = 0;
   /** the date the article was published */
   public static final int IN_DATE = 1;
   /** the title of the article */
   public static final int IN_TITLE = 2;
   /** the permanent location of the article */
   public static final int IN_PERMID = 3;
   /** the text of the article */
   public static final int IN_DESC = 4;
   /** number fo fields */
   public static final int IN_FIELD_COUNT = 5;

   private static final DateTimeFormatter zonedDateFormats[] =
   {
         DateTimeFormatter.ISO_ZONED_DATE_TIME, //
         DateTimeFormatter.ISO_DATE_TIME, //
         DateTimeFormatter.RFC_1123_DATE_TIME, //
         DateTimeFormatter.ISO_DATE, //
         DateTimeFormatter.ISO_LOCAL_DATE, //
         DateTimeFormatter.ISO_LOCAL_DATE_TIME, //
         DateTimeFormatter.ISO_OFFSET_DATE, //
         DateTimeFormatter.ISO_OFFSET_DATE_TIME, //
         DateTimeFormatter.ISO_OFFSET_TIME, //
         DateTimeFormatter.ISO_ORDINAL_DATE, //
         DateTimeFormatter.ISO_TIME, //
         DateTimeFormatter.ISO_WEEK_DATE, //
         // for example: 2017-03-17T07:59:07.451348
         DateTimeFormatter.ofPattern("y-M-d'T'H:m:s[.A]"), //
         DateTimeFormatter.ofPattern("c, d M y H:m:s z"), //
         DateTimeFormatter.BASIC_ISO_DATE, //
         DateTimeFormatter.ofPattern("y") // dummy to end with
   };

   /**
    * translate the date string into a {@link java.sql.Date} for use in a DB
    * <p>
    * Examples: Sun, 19 Mar 2017 20:56:42 +0000<br>
    *
    * @param date
    *        the date string to parse
    * @return the DB date the date in the string, or {@code null} if the format is unrecognized
    */
   static LocalDateTime getDate(final String date)
   {
      LocalDateTime recordDate = null;
      String modDate = date;
      final int index = modDate.lastIndexOf('.');
      if (index == modDate.length() - 7)
      {
         modDate = modDate.substring(0, index);
      }

      // System.out.println("parsing " + date);
      for (int i = 0; i < zonedDateFormats.length && recordDate == null; i++)
      {
         try
         {
            recordDate = LocalDateTime.parse(modDate, zonedDateFormats[i]);
         }
         catch (@SuppressWarnings("unused") final DateTimeParseException e)
         {
            // just keep rolling through the choices
         }
      }

      if (recordDate == null)
      {
         try
         {
            if (modDate.endsWith("PDT"))
            {
               recordDate = LocalDateTime.parse(modDate.replace("PDT", "-0700"), DateTimeFormatter.RFC_1123_DATE_TIME);
            }
            else
            {
               if (modDate.endsWith("PST"))
               {
                  recordDate = LocalDateTime.parse(modDate.replace("PST", "-0700"),
                        DateTimeFormatter.RFC_1123_DATE_TIME);
               }
               else
                  if (modDate.endsWith("EDT"))
                  {
                     recordDate = LocalDateTime.parse(modDate.replace("EDT", "-0400"),
                           DateTimeFormatter.RFC_1123_DATE_TIME);
                  }
                  else
                     if (modDate.endsWith("EST"))
                     {
                        recordDate = LocalDateTime.parse(modDate.replace("EST", "-0500"),
                              DateTimeFormatter.RFC_1123_DATE_TIME);
                     }
            }
         }
         catch (@SuppressWarnings("unused") final DateTimeParseException ignored)
         {
            // well, we tried
         }
      }

      if (recordDate == null)
      {
         System.out.println("Invalid date format: " + date);
      }
      return recordDate;
   }

   /**
    * This will be a brute force push through the data file. We will read each line (csv format) and check if that
    * permid has already been seen. If not then generate a unique id and store the new record.
    * <p>
    * the input file should be a CSV with the format
    * <p>
    * url, date, title, permId, fullText
    *
    * @param args
    *        The input filename
    */
   public static void main(final String[] args)
   {
      // prepare to parse the command line
      final Options options = new Options();
      options.addOption(Option.builder("file").argName("filename").hasArg().desc("the input file name").build());
      options.addOption(Option.builder("out").argName("filename").hasArg().desc("the output file name").build());
      options.addOption(
            Option.builder("progressCount").argName("n").hasArg().desc("output a status ever n lines").build());

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
      final String inputFilename = cmdLine.getOptionValue("file", "input.csv");
      final String outputFilename = cmdLine.getOptionValue("out", "titles.csv");
      final String progressCountArg = cmdLine.getOptionValue("progressCount", "50000");
      int progressCount = 0;
      try
      {
         progressCount = Integer.parseInt(progressCountArg);
      }
      catch (@SuppressWarnings("unused") final NumberFormatException ignored)
      {
         System.out
               .println("Invalid number for progressCount (" + progressCountArg + "). No progress will be reported");
      }

      // ok, prepare the date formatters by using the longest first.
      // Arrays.sort(dateFormats, (o1, o2) -> o1.toString().length() - o2.toString().length());

      final BfMain app = new BfMain();

      try (TitleWriter writer = new TitleWriter(
            new CSVWriter(new OutputStreamWriter(new FileOutputStream(outputFilename))));
            final CSVReader reader = new CSVReader(new FileReader(inputFilename));)
      {
         app.readFile(reader, writer, progressCount);
      }
      catch (@SuppressWarnings("unused") final FileNotFoundException e)
      {
         final File file = new File(inputFilename);
         System.err.println("Invalid input file: " + file.getAbsolutePath());
      }
      catch (final IOException e)
      {
         e.printStackTrace();
      }

   }

   private final NumberFormat integerFormat = NumberFormat.getIntegerInstance();

   /**
    * @param writer
    * @param reader
    * @param progressCount
    *        Output a progress message every progressCount lines. Zero will not output any lines.
    * @throws IOException
    */
   private void readFile(final CSVReader reader, final TitleWriter writer, final int progressCount) throws IOException
   {
      final HashMap<String, String> existingTitles = new HashMap<>();

      String[] line = null;
      int lineCount = 1;
      boolean invalidLine = false;
      int writeCount = 0;
      MessageDigest sha1 = null;
      try
      {
         sha1 = MessageDigest.getInstance("SHA1");
      }
      catch (final NoSuchAlgorithmException e)
      {
         e.printStackTrace();
         return;
      }

      while ((line = reader.readNext()) != null)
      {
         // if the line before was invalid, check that this line starts with the IN_URL
         if (invalidLine && line.length > 0 && line[0].startsWith("http"))
         {
            invalidLine = false;
         }
         if (!invalidLine)
         {
            // is this line crap?
            if (line.length >= IN_FIELD_COUNT)
            {
               final byte[] hashBytes = sha1.digest((line[IN_URL] + line[IN_TITLE]).getBytes());
               final String hash = new String(hashBytes);

               if (!existingTitles.containsKey(hash))
               {
                  final LocalDateTime date = getDate(line[IN_DATE]);

                  writer.write(lineCount, line[IN_URL], date, line[IN_TITLE], line[IN_DESC]);
                  existingTitles.put(hash, line[IN_TITLE]);
                  writeCount++;
               }
            }
            else // this line is definitely crap
            {
               invalidLine = true;
               if (line.length > 0)
               {
                  final int length = Math.min(20, line[0].length());
                  final String msg = line[0].substring(0, length);
                  System.err.println("Invalid line at line " + lineCount + ", line = " + msg);
               }
               else
               {
                  System.err.println("line is just crap");
               }
            }
         } // last line was ok
         if (progressCount > 0 && lineCount % progressCount == 0)
         {
            System.out.println("Read " + integerFormat.format(lineCount) + " lines");
         }

         lineCount++;
      } // end while
      if (progressCount > 0)
      {
         System.out.println("Read " + integerFormat.format(lineCount) + " lines");
      }
      System.out.println("Wrote " + integerFormat.format(writeCount) + " lines");
   }

}
