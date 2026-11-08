#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Convert the Bangla Ar-Raheeq Al-Makhtum PDF into structured Seerah JSON.

The IslamHouse PDF is a 508-page image scan. It has no usable text layer, so
this script:

1. tries PyMuPDF/pdfplumber/pdftotext first;
2. if the PDF is image-only, reads a cached OCR text file; or
3. renders pages with pdftoppm and OCRs them with Tesseract (Bengali).

Outputs (relative to --out):
    data/seerah.json
    data/seerah_index.json
    data/chapters/chapter_01.json ... chapter_50.json
    data/manifest.json
    txt/seerah_full.txt
    txt/chapters/chapter_01.txt ... chapter_50.txt

Usage:
    python3 scripts/pdf_to_json.py \
        --pdf /path/to/raheeq_bn.pdf \
        --ocr-text data/source/raheeq_ocr.txt \
        --out .

The chapter boundary table is specific to this IslamHouse Bangla edition. The
offsets refer to the cached OCR text; when a fresh Tesseract OCR is used the
script locates each chapter by its OCR heading instead.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

PDF_URL = "https://d1.islamhouse.com/data/bn/ih_books/single/bn_ar_raheequl_makhtoom.pdf"
BOOK_TITLE = "আর-রাহীকুল মাখতুম"
BOOK_TITLE_EN = "Ar-Raheeq Al-Makhtum"
AUTHOR = "সফিউর রহমান মুবারকপুরী"
TRANSLATOR = "খাদিজা আখতার রেজায়ী"
BODY_END = 1318600

# (start offset in the cached OCR text, cleaned title, period, summary)
# The offsets were verified against the 508-page IslamHouse scan.
CHAPTERS = [
    (49043, "আরবের ভৌগোলিক পরিচয় ও জাতিসমূহ", "প্রাক-ইসলামি আরব",
     "আরব উপদ্বীপের অবস্থান, ভৌগোলিক বৈশিষ্ট্য এবং আরব জাতিসমূহের তিন ভাগের পরিচয়।"),
    (92271, "আরবের প্রশাসনিক, অর্থনৈতিক ও ধর্মীয় অবস্থা", "প্রাক-ইসলামি আরব",
     "আরবের শাসনব্যবস্থা, ব্যবসা-বাণিজ্য, অর্থনীতি এবং পৌত্তলিক ধর্মবিশ্বাসের চিত্র।"),
    (117497, "আরবের সামাজিক অবস্থা ও জাহেলিয়াত সমাজ", "প্রাক-ইসলামি আরব",
     "পারিবারিক ও সামাজিক জীবন, নারীর অবস্থান, জাহেলি রীতিনীতি এবং আরবদের চারিত্রিক গুণাবলি।"),
    (130534, "নবী পরিবারের বংশ পরিচয়", "৫৭০ খ্রিস্টাব্দের পূর্বে",
     "রাসূলুল্লাহ ﷺ-এর বংশধারা, হাশিম, আবদুল মোত্তালেব ও নবী পরিবারের পরিচয়।"),
    (137755, "যমযম কূপের খনন ও হস্তী যুদ্ধ", "৫৭০ খ্রিস্টাব্দের পূর্বে",
     "আবদুল মোত্তালেবের যমযম কূপ খনন, কাবার সংস্কার এবং আবরাহার হস্তী যুদ্ধের ঘটনা।"),
    (147384, "জন্ম, শৈশব ও সিনা চাকের ঘটনা", "৫৭০–৫৭৭ খ্রিস্টাব্দ",
     "মক্কায় রাসূলুল্লাহ ﷺ-এর জন্ম, বনু সা'দ গোত্রে লালনপালন এবং সিনা চাকের ঘটনা।"),
    (154338, "মায়ের স্নেহ ও দাদার আদর", "৫৭৭–৫৮০ খ্রিস্টাব্দ",
     "মা আমেনার স্নেহ, মাতৃবিয়োগ, দাদা আবদুল মোত্তালেবের আদর ও লালনপালন।"),
    (157514, "আল্লাহর রহমতের সন্ধানে ও পাদ্রী বুহাইরা", "৫৮০–৫৯০ খ্রিস্টাব্দ",
     "চাচা আবু তালেবের স্নেহ, ব্যবসায়িক সফর, পাদ্রী বুহাইরার সাথে সাক্ষাৎ ও ফুজ্জারের যুদ্ধ।"),
    (162712, "হেলফুল ফুযুল ও সংগ্রামী জীবন", "৫৯০–৫৯৫ খ্রিস্টাব্দ",
     "হেলফুল ফুযুল সংগঠন, সামাজিক সংস্কার এবং ন্যায় প্রতিষ্ঠায় রাসূলুল্লাহ ﷺ-এর ভূমিকা।"),
    (164172, "বিবি খাদিজার সাথে বিয়ে", "৫৯৫ খ্রিস্টাব্দ",
     "বিবি খাদিজা রা.-এর সাথে রাসূলুল্লাহ ﷺ-এর বিবাহ এবং ব্যবসায়িক জীবনের সূচনা।"),
    (166567, "কাবার নির্মাণ ও হাজরে আসওয়াদের বিরোধ মীমাংসা", "৬০৫ খ্রিস্টাব্দ",
     "কাবার পুনর্নির্মাণ, হাজরে আসওয়াদ স্থাপন নিয়ে বিরোধ এবং রাসূলুল্লাহ ﷺ-এর বিচক্ষণ মীমাংসা।"),
    (170124, "নবুয়তের আগের জীবন", "৬০৫–৬১০ খ্রিস্টাব্দ",
     "নবুয়তের পূর্বে রাসূলুল্লাহ ﷺ-এর চরিত্র, ইবাদত, হেরা গুহার নির্জনতা ও আত্মপ্রস্তুতি।"),
    (177601, "প্রথম ওহী ও নবুয়তের সূচনা", "৬১০ খ্রিস্টাব্দ",
     "হেরা গুহায় জিবরাইল আ.-এর আগমন, প্রথম ওহী, খাদিজা রা.-এর সমর্থন ও প্রথম মুসলিমগণ।"),
    (204875, "প্রকাশ্য দাওয়াতের সূচনা", "৬১০–৬১৩ খ্রিস্টাব্দ",
     "নিকটাত্মীয়দের দাওয়াত, সাফা পাহাড়ের ঘোষণা এবং ইসলামের প্রকাশ্য প্রচার।"),
    (214347, "কোরায়েশদের প্রতিরোধ ও প্রতিনিধি দল", "৬১৩–৬১৪ খ্রিস্টাব্দ",
     "আবু তালেবের কাছে কোরায়েশ প্রতিনিধি দল, হাজীদের বাধা এবং প্রতিরোধের বিভিন্ন ধরন।"),
    (225679, "যুলুম নির্যাতন ও মুসলিমদের ধৈর্য", "৬১৪–৬১৫ খ্রিস্টাব্দ",
     "কোরায়েশদের অত্যাচার, দুর্বল মুসলিমদের ওপর নির্যাতন এবং সাহাবাদের অবিচলতা।"),
    (245290, "দারে আরকাম ও গোপন দাওয়াত", "৬১৫–৬১৬ খ্রিস্টাব্দ",
     "দারে আরকামের গোপন শিক্ষাকেন্দ্র, নতুন মুসলিমদের প্রশিক্ষণ এবং দাওয়াতের বিস্তার।"),
    (266107, "হামযা ও ওমরের ইসলাম গ্রহণ", "৬১৬–৬১৭ খ্রিস্টাব্দ",
     "রাসূলুল্লাহ ﷺ-কে হত্যার হীন প্রস্তাব, হামযা রা. ও ওমর রা.-এর ইসলাম গ্রহণ।"),
    (293726, "বনু হাশেমের বৈঠক ও সর্বাত্মক বয়কট", "৬১৭–৬১৯ খ্রিস্টাব্দ",
     "বনু হাশেম ও বনু মোত্তালেবের সমর্থন, শা'বে আবু তালেবের অবরোধ ও দলিল ছিন্ন হওয়া।"),
    (309911, "দুঃখ বেদনার বছর ও সাওদার সাথে বিবাহ", "৬১৯–৬২০ খ্রিস্টাব্দ",
     "আবু তালেব ও খাদিজা রা.-এর ইন্তেকাল, দুঃখ-দুশ্চিন্তা এবং সাওদা রা.-এর সাথে বিবাহ।"),
    (336532, "তায়েফের সফর ও মক্কার বাইরে দাওয়াত", "৬২০–৬২১ খ্রিস্টাব্দ",
     "তায়েফে ইসলামের দাওয়াত, প্রত্যাখ্যান, ফেরার পথে দোয়া এবং মক্কার বাইরে দাওয়াতের সূচনা।"),
    (349634, "বিভিন্ন গোত্র ও ব্যক্তির কাছে দাওয়াত", "৬২০–৬২১ খ্রিস্টাব্দ",
     "হজের মৌসুমে বিভিন্ন গোত্র ও ব্যক্তির কাছে ইসলামের দাওয়াত এবং এর প্রসার।"),
    (363892, "মদীনার ছয়জন, মেরাজ ও আয়েশা রা.-এর বিবাহ", "৬২০–৬২১ খ্রিস্টাব্দ",
     "মদীনা থেকে আগত ছয়জনের ইসলাম গ্রহণ, ইসরা ও মেরাজ এবং আয়েশা রা.-এর সাথে বিবাহ।"),
    (380323, "প্রথম বাইয়াতে আকাবা ও মদীনায় দূত প্রেরণ", "৬২১–৬২২ খ্রিস্টাব্দ",
     "প্রথম আকাবার বাইয়াত, মদীনায় মুসআব রা.-কে দূত করে প্রেরণ এবং ইসলামের প্রসার।"),
    (397218, "দ্বিতীয় বাইয়াতে আকাবা", "৬২২ খ্রিস্টাব্দ",
     "দ্বিতীয় আকাবার বাইয়াত, বারোজন নকীবের নাম এবং মদীনার নেতাদের সাথে চুক্তি।"),
    (411577, "দারুন নোদওয়া ও গৃহত্যাগ", "৬২২ খ্রিস্টাব্দ",
     "কোরায়েশদের গোপন বৈঠক, রাসূলুল্লাহ ﷺ-কে হত্যার পরিকল্পনা এবং গৃহত্যাগ।"),
    (426378, "ছুর গুহা ও মদীনার পথে হিজরত", "৬২২ খ্রিস্টাব্দ",
     "ছুর পর্বতের গুহায় আশ্রয়, আবু বকর রা.-এর সঙ্গী হওয়া এবং মদীনার পথে সফর।"),
    (444090, "কোবায় অবস্থান ও মদীনায় প্রবেশ", "৬২২ খ্রিস্টাব্দ",
     "কোবায় অবতরণ, প্রথম মসজিদ নির্মাণ এবং মদীনায় রাসূলুল্লাহ ﷺ-এর প্রবেশ।"),
    (449234, "মদীনায় প্রবেশ ও মসজিদে নববীর নির্মাণ", "৬২২ খ্রিস্টাব্দ",
     "মদীনার আনন্দময় অভ্যর্থনা, মসজিদে নববীর নির্মাণ এবং ইসলামী রাষ্ট্রের সূচনা।"),
    (454154, "মাদানী জীবনের সূচনা ও নতুন সমাজ ব্যবস্থা", "৬২২ খ্রিস্টাব্দ",
     "মদীনার সার্বিক অবস্থা, মুহাজির-আনসারের ভ্রাতৃত্ব এবং নতুন সমাজ ব্যবস্থার রূপায়ন।"),
    (471519, "মসজিদে নববী, ভ্রাতৃত্ব ও সমাজের নতুন কাঠামো", "৬২২ খ্রিস্টাব্দ",
     "মসজিদে নববীর ভূমিকা, মুসলিম ভ্রাতৃত্ব প্রতিষ্ঠা এবং সমাজ ব্যবস্থার মৌলিক কাঠামো।"),
    (489695, "ইহুদীদের সাথে চুক্তি ও যুদ্ধের অনুমতি", "৬২২–৬২৩ খ্রিস্টাব্দ",
     "মদীনার ইহুদী গোত্রগুলোর সাথে চুক্তি, কোরায়েশদের শত্রুতা এবং যুদ্ধের অনুমতি লাভ।"),
    (508032, "বদরের যুদ্ধের প্রস্তুতি", "৬২৩ খ্রিস্টাব্দ",
     "বদরের প্রস্তুতি, মুসলিম ও কোরায়েশ বাহিনীর অবস্থান, শুরা ও যুদ্ধের ময়দানে উপস্থিতি।"),
    (553487, "বদরের যুদ্ধ ও বিজয়", "৬২৩ খ্রিস্টাব্দ",
     "বদর প্রান্তরে দোয়া, ফেরেশতাদের অবতরণ, যুদ্ধের ঘটনাক্রম এবং মুসলিমদের বিজয়।"),
    (596693, "বদর-পরবর্তী সামরিক অভিযান", "৬২৩–৬২৫ খ্রিস্টাব্দ",
     "বনু সালিম, বনু কাইনুকা, কা'ব ইবনে আশরাফ ও অন্যান্য অভিযানের বিবরণ।"),
    (638040, "ওহুদের যুদ্ধ", "৬২৫ খ্রিস্টাব্দ",
     "ওহুদের প্রস্তুতি, যুদ্ধের ঘটনাক্রম, হামযা রা.-এর শাহাদাত এবং মুসলিমদের পরীক্ষা।"),
    (721002, "ওহুদ-পরবর্তী অভিযান ও বনু নাযির", "৬২৫–৬২৬ খ্রিস্টাব্দ",
     "ওহুদের পরবর্তী অভিযান, বনু নাযিরের বিশ্বাসঘাতকতা এবং তাদের বহিষ্কার।"),
    (790602, "খন্দকের যুদ্ধ", "৬২৭ খ্রিস্টাব্দ",
     "খন্দকের প্রস্তুতি, পরিখা খনন, মিত্রবাহিনীর অবরোধ এবং আল্লাহর সাহায্যে বিজয়।"),
    (840068, "বনু কোরায়যা ও পরবর্তী অভিযান", "৬২৭–৬২৮ খ্রিস্টাব্দ",
     "বনু কোরায়যার বিশ্বাসঘাতকতা, তাদের পরিণতি এবং খন্দক-পরবর্তী সামরিক অভিযান।"),
    (893443, "হোদায়বিয়ার সন্ধি", "৬২৮ খ্রিস্টাব্দ",
     "ওমরাহর প্রস্তুতি, কোরায়েশদের বাধা, বাইয়াতে রেজোয়ান এবং হোদায়বিয়ার সন্ধি।"),
    (932593, "সন্ধির পর চিঠি ও খয়বরের যুদ্ধ", "৬২৮–৬২৯ খ্রিস্টাব্দ",
     "বাদশাহদের কাছে ইসলামের চিঠি, খয়বরের যুদ্ধ এবং ইহুদী গোত্রগুলোর পরিণতি।"),
    (1007428, "খয়বর-পরবর্তী অভিযান ও মুতার যুদ্ধ", "৬২৯–৬৩০ খ্রিস্টাব্দ",
     "খয়বর-পরবর্তী অভিযান, মুতার যুদ্ধ, রোমকদের সাথে সংঘাতের সূচনা।"),
    (1084579, "মক্কা বিজয়", "৬৩০ খ্রিস্টাব্দ",
     "মক্কা বিজয়, কাবার মূর্তি অপসারণ, সাধারণ ক্ষমা এবং ইসলামের চূড়ান্ত বিজয়।"),
    (1109614, "হোনায়েন ও তায়েফের যুদ্ধ", "৬৩০ খ্রিস্টাব্দ",
     "হোনায়েনের যুদ্ধ, তায়েফের অবরোধ, গনীমত বণ্টন এবং আনসারদের মানসিক অবস্থা।"),
    (1135546, "তবুকের যুদ্ধ", "৬৩১ খ্রিস্টাব্দ",
     "রোমকদের প্রস্তুতি, তবুক অভিযান, মুসলিম বাহিনীর সঙ্কট এবং মদীনায় প্রত্যাবর্তন।"),
    (1179290, "যুদ্ধসমূহের পর্যালোচনা, দলে দলে ইসলাম ও বিদায় হজ", "৬৩১–৬৩২ খ্রিস্টাব্দ",
     "যুদ্ধসমূহের সংক্ষিপ্ত পর্যালোচনা, আবু বকর রা.-এর হজ, দলে দলে ইসলাম গ্রহণ ও বিদায় হজ।"),
    (1250298, "অসুখের শুরু ও জীবনের শেষ সপ্তাহ", "৬৩২ খ্রিস্টাব্দ",
     "মরণ অসুখের শুরু, নামাযে ইমামতি এবং জীবনের শেষ সপ্তাহের ঘটনাক্রম।"),
    (1263497, "ওফাত, দাফন ও শোকের ছায়া", "৬৩২ খ্রিস্টাব্দ",
     "মৃত্যুকালীন অবস্থা, মহানবী ﷺ-এর ওফাত, দাফন এবং সাহাবাদের শোক ও প্রতিক্রিয়া।"),
    (1272510, "নবীজীর সহধর্মিণীগণ", "জীবনী",
     "উম্মুল মুমেনীনগণের পরিচয় ও নবীজীর ﷺ পরিবার।"),
    (1293989, "চারিত্রিক বৈশিষ্ট ও শারীরিক সৌন্দর্য", "শামায়েল",
     "রাসূলুল্লাহ ﷺ-এর চারিত্রিক বৈশিষ্ট, শারীরিক গঠন, সৌন্দর্য ও শামায়েল।"),
]

REF_WORDS = (
    "পৃ", "খন্ড", "খণ্ড", "পৃষ্ঠা", "মেশকাত", "সহীহ", "বোখারী", "বুখারী",
    "মুসলিম", "তিরমিযি", "ইবনে", "যাদুল", "রহমাতুল", "মুসনাদ", "তাবাকাত",
    "তাফসীর", "তাফসির", "ফেকহ", "শাফা", "খোলাসাত", "তারীখ", "সীরাত", "সিরাত",
    "আল বেদায়া", "হাকেম", "যাহাবি", "নাসাঈ", "আবু দাউদ", "দারেমী", "আহমদ",
    "মোয়াত্তা", "মালিক", "বায়হাকী", "জামে", "সুনান", "মুস্তাদরাক", "আসার",
)

HEADING_SUFFIXES = (
    "অবস্থা", "পরিচয়", "পরিচিতি", "ইতিহাস", "নেতৃত্ব", "শাসন", "বাদশাহী",
    "রাজত্ব", "যুদ্ধ", "ঘটনা", "সফর", "দাওয়াত", "হিজরত", "বিবাহ", "জন্ম",
    "শৈশব", "নবুয়ত", "ওহী", "বায়াত", "সন্ধি", "বিজয়", "অভিযান", "মৃত্যু",
    "ওফাত", "পরিবার", "সহধর্মিণী", "শামায়েল", "বৈশিষ্ট্য", "প্রস্তুতি",
    "প্রতিরোধ", "নির্যাতন", "ধৈর্য", "গোপন", "প্রকাশ্য", "সূচনা", "সমাপ্তি",
    "পর্যালোচনা", "চুক্তি", "মসজিদ", "কাবা", "মক্কা", "মদীনা", "তায়েফ",
    "বদর", "ওহুদ", "খন্দক", "খয়বর", "হোদায়বিয়া", "তবুক", "বিদায় হজ",
)
ORDINAL_RE = re.compile(r"^(?:এক|দুই|তিন|চার|পাঁচ|ছয়|সাত|আট|নয়|দশ)[.)]\s")
LIST_RE = re.compile(r"^(এক|দুই|তিন|চার|পাঁচ|ছয়|সাত|আট|নয়|দশ|[০-৯]+)[.)]\s")


def bengali_count(text: str) -> int:
    return sum(1 for ch in text if "\u0980" <= ch <= "\u09ff")


def latin_count(text: str) -> int:
    return sum(1 for ch in text if "a" <= ch.lower() <= "z")


def is_reference_line(text: str) -> bool:
    value = text.strip()
    if not value:
        return False
    numbered = bool(re.match(r"^[\*\-–—]*\s*[০-৯0-9]{1,3}\s*[.)।,]?\s*", value))
    has_ref = any(word in value for word in REF_WORDS)
    if numbered and has_ref:
        return True
    if has_ref and len(value) < 140 and re.search(r"(পৃ|পৃষ্ঠা|খন্ড|খণ্ড|হিজরী|হিঃ|খৃঃ)", value):
        return True
    if "আরো দেখুন" in value and has_ref:
        return True
    if value.startswith("শরিফের") and "দেখুন" in value:
        return True
    return False


def is_header_line(text: str) -> bool:
    value = text.strip()
    if not value:
        return False
    if "QuranerAlo" in value or "http" in value.lower() or "www." in value.lower():
        return True
    if "মাখতুম" in value and len(value) < 90:
        return True
    if "রাহীকুল" in value and len(value) < 120:
        return True
    if re.match(r"^\[?\s*আর\s*রাহীকুল", value):
        return True
    if re.match(r"^\[?\s*আরর\s", value) or re.match(r"^\[?\s*জার\s", value):
        return True
    if re.match(r"^\[?\s*হী\s*মা", value) or re.match(r"^\[?\s*রা\s*হী", value):
        return True
    if re.fullmatch(r"[\s\[\]|০-৯0-9.,:;/\-–—]+", value):
        return True
    return False


def is_garbage_line(text: str) -> bool:
    value = text.strip()
    if not value:
        return True
    bengali = bengali_count(value)
    latin = latin_count(value)
    if bengali == 0 and latin == 0:
        return True
    if bengali < 2 and latin > 2:
        return True
    if bengali <= 2 and len(value) <= 4:
        return True
    if len(set(value)) <= 2 and len(value) > 3:
        return True
    return False


def clean_lines(raw: str) -> list[str]:
    result: list[str] = []
    for line in raw.splitlines():
        value = re.sub(r"[\u200b-\u200f\u202a-\u202e]", "", line)
        value = value.replace("\ufeff", "").replace("\t", " ")
        value = re.sub(r"[ ]{2,}", " ", value).strip()
        if not value:
            result.append("")
            continue
        if is_header_line(value) or is_garbage_line(value) or is_reference_line(value):
            continue
        value = re.sub(r"(?<=[।?!”“\"])\s*[০-৯0-9]{1,3}\b", "", value)
        value = re.sub(r"\[\s*[০-৯0-9]{1,4}\s*\]", "", value)
        value = re.sub(r"[|_]+", " ", value)
        value = re.sub(r"[ ]{2,}", " ", value).strip()
        if not value:
            continue
        if is_header_line(value) or is_garbage_line(value):
            continue
        result.append(value)
    return result


def is_heading_line(text: str) -> bool:
    """Title-like line used only to break the OCR text into paragraphs."""
    if len(text) > 55:
        return False
    if any(mark in text for mark in ("।", "?", "!")):
        return False
    if text.endswith(("।", "?", "!", "”", "\"", "'", ":", ";")):
        return False
    if LIST_RE.match(text):
        return True
    return bengali_count(text) >= 3


def paragraphs_from(lines: list[str]) -> list[str]:
    paragraphs: list[str] = []
    current: list[str] = []
    for line in lines:
        if not line:
            if current:
                paragraphs.append(" ".join(current))
                current = []
            continue
        if is_heading_line(line):
            if current:
                paragraphs.append(" ".join(current))
                current = []
            paragraphs.append(line)
            continue
        current.append(line)
        joined = " ".join(current)
        if (joined.endswith(("।", "?", "!", "”", "\"")) and len(joined) >= 70) or len(joined) > 900:
            paragraphs.append(joined)
            current = []
    if current:
        paragraphs.append(" ".join(current))
    cleaned: list[str] = []
    for paragraph in paragraphs:
        paragraph = re.sub(r"[ ]{2,}", " ", paragraph).strip()
        if not paragraph or bengali_count(paragraph) == 0 or len(paragraph) <= 2:
            continue
        cleaned.append(paragraph)
    return cleaned


def chapter_body(raw: str) -> str:
    lines = clean_lines(raw)
    start = 0
    while start < len(lines):
        value = lines[start]
        if not value:
            start += 1
            continue
        if value.endswith(("।", "?", "!", "”", "\"")) and len(value) >= 60:
            break
        if len(value) >= 70:
            break
        start += 1
    paragraphs = paragraphs_from(lines[start:])
    while paragraphs and len(paragraphs[0]) <= 55 and not paragraphs[0].endswith(("।", "?", "!", "”")):
        paragraphs.pop(0)
    while paragraphs and len(paragraphs[-1]) <= 40 and not any(
            mark in paragraphs[-1] for mark in ("।", "?", "!", "”")):
        paragraphs.pop()
    return "\n\n".join(paragraphs)


FOOTNOTE_TAIL = re.compile(r"[\s'\"“”]*[০-৯0-9]{1,3}\s*$")
REFERENCE_TAIL = re.compile(r"^[\s'\"“”]*(?:[০-৯0-9]{1,3}|\([^)]*\)|[০-৯0-9\-–—,.\s]+)+$")


def polish_body(body: str) -> str:
    body = body.strip()
    if not body:
        return body
    body = body.replace("সাল্লাল্লাহু আলাইহি ওয়া সাল্লামএমন",
                        "সাল্লাল্লাহু আলাইহি ওয়া সাল্লাম এমন")
    body = re.sub(r"([।?!])\s*[ও৷]\s*$", r"\1", body)
    body = FOOTNOTE_TAIL.sub("", body)
    stops = list(re.finditer(r"[।?!]", body))
    if stops:
        tail = body[stops[-1].end():]
        if 0 < len(tail.strip()) <= 40 and REFERENCE_TAIL.match(tail):
            body = body[:stops[-1].end()].rstrip()
    if body and body[-1] not in "।?!.”\"'" and not re.search(r"[০-৯0-9]{1,4}$", body):
        body += "।"
    return body


def merge_split_sentence(chapters: list[dict]) -> None:
    for previous, following in zip(chapters, chapters[1:]):
        body = previous["body"]
        next_body = following["body"]
        if not body or body[-1] in "।?!.”\"'":
            continue
        stops = list(re.finditer(r"[।?!]", body))
        if not stops:
            continue
        tail = body[stops[-1].end():].strip()
        if 0 < len(tail) <= 30 and next_body.startswith("তখন"):
            previous["body"] = body[:stops[-1].end()].rstrip()
            following["body"] = tail + " " + next_body


def is_subheading(paragraph: str) -> bool:
    value = paragraph.strip()
    if not (2 <= len(value) <= 60):
        return False
    if len(value.split()) > 8:
        return False
    if re.search(r"[।?!৷]$", value):
        return False
    if value.endswith((",", ";", ":", "”", "\"", "'", "—", "-", "|")):
        return False
    if value.startswith(("“", "\"", "'", "‘", "(", "[", "-")):
        return False
    if re.search(r"[0-9A-Za-z০-৯]", value):
        return False
    if "http" in value.lower() or "মাখতুম" in value or "রাহীকুল" in value:
        return False
    if any(word in value for word in REF_WORDS):
        return False
    if len(value.split()) < 2:
        return False
    if ORDINAL_RE.match(value):
        return True
    return any(value.endswith(suffix) for suffix in HEADING_SUFFIXES)


def content_blocks(title: str, body: str) -> list[dict]:
    blocks = [{"type": "heading", "text": title}]
    for paragraph in body.split("\n\n"):
        paragraph = paragraph.strip()
        if not paragraph:
            continue
        block_type = "heading" if is_subheading(paragraph) else "paragraph"
        blocks.append({"type": block_type, "text": paragraph})
    return blocks


def slug_for(index: int) -> str:
    return f"chapter-{index:02d}"


def chapter_payload(index: int, title: str, period: str, summary: str, body: str) -> dict:
    blocks = content_blocks(title, body)
    char_count = sum(len(block["text"]) for block in blocks)
    return {
        "id": index,
        "title": title,
        "slug": slug_for(index),
        "period": period,
        "summary": summary,
        "char_count": char_count,
        "content": blocks,
    }


def build_meta(generated: str) -> dict:
    return {
        "title": BOOK_TITLE,
        "title_en": BOOK_TITLE_EN,
        "author": AUTHOR,
        "translator": TRANSLATOR,
        "language": "bn",
        "total_chapters": len(CHAPTERS),
        "source": "islamhouse.com",
        "source_url": PDF_URL,
        "edition": "IslamHouse Bangla PDF, 508-page scan (OCR-derived text)",
        "generated": generated,
        "notice": "Technical cleanup only. Redistribution permission is not independently confirmed; "
                  "provided for free Islamic/educational use with source credit. Not a scholarly review.",
    }


def write_json(path: Path, payload: dict, pretty: bool = False) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if pretty:
        text = json.dumps(payload, ensure_ascii=False, indent=2)
    else:
        text = json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
    path.write_text(text + "\n", encoding="utf-8")


def write_chapter_text(path: Path, chapter: dict) -> None:
    lines = [chapter["title"], chapter.get("period", ""), ""]
    for block in chapter["content"]:
        if block["type"] == "heading":
            lines.extend(["", block["text"], ""])
        else:
            lines.extend([block["text"], ""])
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines).strip() + "\n", encoding="utf-8")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def pdf_has_text_layer(pdf: Path) -> bool:
    """Return True when the PDF exposes a meaningful text layer."""
    try:
        import fitz  # type: ignore
        document = fitz.open(str(pdf))
        sample = "".join(document[index].get_text() for index in range(min(12, document.page_count)))
        return bengali_count(sample) > 400
    except Exception:
        pass
    try:
        result = subprocess.run(
            ["pdftotext", "-f", "1", "-l", "12", str(pdf), "-"],
            check=False, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL,
        )
        return bengali_count(result.stdout.decode("utf-8", "replace")) > 400
    except Exception:
        return False


def ocr_pdf(pdf: Path, workdir: Path) -> str:
    """Render the PDF and OCR it with Tesseract Bengali. Slow but dependency-light."""
    if shutil.which("pdftoppm") is None or shutil.which("tesseract") is None:
        raise SystemExit("The PDF is image-only and pdftoppm/tesseract are not installed. "
                         "Pass --ocr-text with a cached OCR file.")
    pages = workdir / "pages"
    pages.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        ["pdftoppm", "-r", "300", "-png", str(pdf), str(pages / "page")],
        check=True,
    )
    texts: list[str] = []
    for image in sorted(pages.glob("page-*.png")):
        result = subprocess.run(
            ["tesseract", str(image), "-", "-l", "ben", "--psm", "6"],
            check=False, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL,
        )
        texts.append(result.stdout.decode("utf-8", "replace"))
    return "\n".join(texts)


def load_ocr_text(pdf: Path, ocr_text: Path | None) -> str:
    if ocr_text and ocr_text.is_file():
        return ocr_text.read_text(encoding="utf-8", errors="replace")
    if pdf_has_text_layer(pdf):
        try:
            import fitz  # type: ignore
            document = fitz.open(str(pdf))
            return "\n".join(page.get_text() for page in document)
        except Exception:
            result = subprocess.run(
                ["pdftotext", "-layout", str(pdf), "-"],
                check=True, stdout=subprocess.PIPE,
            )
            return result.stdout.decode("utf-8", "replace")
    with tempfile.TemporaryDirectory(prefix="seerah-ocr-") as temporary:
        return ocr_pdf(pdf, Path(temporary))


def locate_chapter_starts(text: str, ocr_text_supplied: bool) -> list[int]:
    """Use verified offsets for the cached OCR, otherwise find each chapter heading."""
    if ocr_text_supplied:
        return [chapter[0] for chapter in CHAPTERS] + [BODY_END]
    starts: list[int] = []
    cursor = 0
    for _, title, _, _ in CHAPTERS:
        found = text.find(title, cursor)
        if found < 0:
            # OCR headings are often garbled; fall back to the first distinctive word.
            first_word = title.split()[0]
            found = text.find(first_word, cursor)
        if found < 0:
            raise SystemExit(f"Could not locate chapter heading: {title}")
        starts.append(found)
        cursor = found + 1
    starts.append(min(len(text), BODY_END if BODY_END > starts[-1] else len(text)))
    return starts


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pdf", required=True, type=Path, help="Path to the Bangla PDF")
    parser.add_argument("--ocr-text", type=Path, default=None,
                        help="Cached OCR text (required for this scanned edition unless Tesseract is available)")
    parser.add_argument("--out", type=Path, default=Path("."), help="Repository root")
    parser.add_argument("--generated", default="2026-11-08", help="Generation date for meta")
    args = parser.parse_args()

    if not args.pdf.is_file():
        raise SystemExit(f"PDF not found: {args.pdf}")

    supplied = bool(args.ocr_text and args.ocr_text.is_file())
    raw = load_ocr_text(args.pdf, args.ocr_text)
    if bengali_count(raw) < 10_000:
        raise SystemExit("OCR text contains too little Bengali text; check the input.")
    starts = locate_chapter_starts(raw, supplied)

    chapters: list[dict] = []
    for index, (_, title, period, summary) in enumerate(CHAPTERS):
        chapters.append({
            "id": index + 1,
            "title": title,
            "period": period,
            "summary": summary,
            "body": chapter_body(raw[starts[index]:starts[index + 1]]),
        })
    merge_split_sentence(chapters)
    for item in chapters:
        item["body"] = polish_body(item["body"])

    meta = build_meta(args.generated)
    payload_chapters: list[dict] = []
    for index, item in enumerate(chapters):
        payload_chapters.append(chapter_payload(
            index + 1, item["title"], item["period"], item["summary"], item["body"]
        ))

    if any(not chapter["content"] or len(chapter["content"]) < 2 for chapter in payload_chapters):
        raise SystemExit("A chapter is empty; refusing to write incomplete data.")

    data_dir = args.out / "data"
    chapter_dir = data_dir / "chapters"
    txt_dir = args.out / "txt"
    txt_chapter_dir = txt_dir / "chapters"
    chapter_dir.mkdir(parents=True, exist_ok=True)
    txt_chapter_dir.mkdir(parents=True, exist_ok=True)

    full = {"meta": meta, "chapters": payload_chapters}
    write_json(data_dir / "seerah.json", full)
    index_payload = {
        "meta": meta,
        "chapters": [
            {
                "id": chapter["id"],
                "title": chapter["title"],
                "slug": chapter["slug"],
                "period": chapter["period"],
                "summary": chapter["summary"],
                "char_count": chapter["char_count"],
                "file": f"data/chapters/chapter_{chapter['id']:02d}.json",
            }
            for chapter in payload_chapters
        ],
    }
    write_json(data_dir / "seerah_index.json", index_payload, pretty=True)

    for chapter in payload_chapters:
        write_json(chapter_dir / f"chapter_{chapter['id']:02d}.json", chapter)
        write_chapter_text(txt_chapter_dir / f"chapter_{chapter['id']:02d}.txt", chapter)

    full_text = [
        BOOK_TITLE,
        BOOK_TITLE_EN,
        f"লেখক: {AUTHOR}",
        f"অনুবাদ: {TRANSLATOR}",
        f"সূত্র: {PDF_URL}",
        "",
    ]
    for chapter in payload_chapters:
        full_text.extend([
            "=" * 72,
            f"অধ্যায় {chapter['id']:02d}: {chapter['title']}",
            "=" * 72,
            "",
        ])
        for block in chapter["content"]:
            full_text.append(block["text"])
            full_text.append("")
    (txt_dir / "seerah_full.txt").write_text("\n".join(full_text).strip() + "\n", encoding="utf-8")

    manifest = {
        "generated": args.generated,
        "pdf": str(args.pdf.name),
        "pdf_url": PDF_URL,
        "ocr_text_supplied": supplied,
        "total_chapters": len(payload_chapters),
        "total_blocks": sum(len(chapter["content"]) for chapter in payload_chapters),
        "total_chars": sum(chapter["char_count"] for chapter in payload_chapters),
        "files": {},
    }
    for path in sorted([data_dir / "seerah.json", data_dir / "seerah_index.json"] +
                       list(chapter_dir.glob("chapter_*.json")) +
                       [txt_dir / "seerah_full.txt"] +
                       list(txt_chapter_dir.glob("chapter_*.txt"))):
        manifest["files"][str(path.relative_to(args.out))] = {
            "bytes": path.stat().st_size,
            "sha256": sha256(path),
        }
    write_json(data_dir / "manifest.json", manifest, pretty=True)

    print(f"chapters: {len(payload_chapters)}")
    print(f"blocks: {manifest['total_blocks']}")
    print(f"characters: {manifest['total_chars']}")
    print(f"seerah.json: {(data_dir / 'seerah.json').stat().st_size} bytes")
    print(f"seerah_full.txt: {(txt_dir / 'seerah_full.txt').stat().st_size} bytes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
