#!/usr/bin/env bash

# read int all the feeds and scan them
. ~sbetts/feed_env/bin/activate

cd ~sbetts/cs7210feed
python main.py
