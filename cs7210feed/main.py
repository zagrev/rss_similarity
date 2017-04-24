import schedule
import time
import argparse
import pytimeparse
import feedparser
import csv
import io
from datetime import datetime
from unidecode import unidecode
from pprint import pprint

args={}
        
    
def job():
    global args
    print "{2}:  Reading {0} and writing to {1}".format(args.rssfile, args.outfile,datetime.now())
    
    with open(args.outfile, mode="ab") as outfile:
        writer = csv.writer(outfile)
        with open(args.rssfile, "r") as infile:
            for line in infile:
                feedname = line.strip()
                if (feedname):
                    #print "feed: {}".format(feedname)
                    d = feedparser.parse(line)
                    for entry in d.entries:
                        #pprint (vars(entry))
                        title = entry.title
                        
                        if "published" in entry:
                            published = entry.published
                        elif "date" in entry:
                            published = entry.date
                        elif "pubDate" in entry:
                            published = entry.pubDate
                        elif "updated" in entry:
                            published = entry.updated
                        else:
                            published = datetime.now().isoformat()
                            
                        if "summary" in entry:
                            summary = entry.summary
                        elif "description" in entry:
                            summary = entry.description
                        else:
                            summary = "empty"
                        #print "  entry {}".format(title)

                        title = unidecode(title)
                        published = unidecode(published)
                        summary = unidecode(summary)

                        writer.writerow([feedname, published, title, entry.link, summary])
                    #print ""
    print "Sleeping {} seconds".format(args.period)

########################
#
#  main: starts up the processing. Every hour it reads all the RSS feeds in the 
#        given file and appends to the output file.
#
########################

def main():
    global args
    sleepTime = 60

    # parse the command line
    parser = argparse.ArgumentParser(description='Read a list of RSS feeds and append to a file')
    parser.add_argument('--rssfile', metavar='RSS', default='rss.txt',
        help='a file containing the list of RSS files to read')
    parser.add_argument('--outfile', metavar='OUT', default='output.txt',
        help='the date/time, source, title, and content of each entry is appended to the output file')
    parser.add_argument('--period', metavar='TIME', default='1h',
        help='the time period upon which to read updates from the RSS feeds')
    
    args = parser.parse_args()
    
    # parse the period
    #sleepTime = pytimeparse.parse(args.period)
    #schedule.every(sleepTime).seconds.do(job)
    
    # run the job right now to start
    job()
    
    #while True:
    #    schedule.run_pending()
    #    time.sleep(1)

if __name__ == "__main__":
    main()
