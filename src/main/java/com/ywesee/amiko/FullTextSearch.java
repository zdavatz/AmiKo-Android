/*
Copyright (c) 2019 b123400

This file is part of AmiKo for Android.

AmiKo for Android is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.ywesee.amiko;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

class FullTextSearch {
    public ArrayList<String> listOfSectionIds;
    public ArrayList<String> listOfSectionTitles;
    private ArrayList<Medication> mListOfArticles;
    private HashMap<String, ArrayList<String>> mDict;

    public String table(
        ArrayList<Medication> listOfArticles,
        HashMap<String, ArrayList<String>> regChaptersDict,
        String filter
    ) {
        int rows = 0;
        HashMap<String, Integer> chaptersCountDict = new HashMap<>();
        String htmlStr = "<ul>";

        // Assign list and dictionaries only if != null
        if (listOfArticles != null) {
            mListOfArticles = listOfArticles;
            // Sort alphabetically (this is pretty neat!)
            mListOfArticles.sort(new Comparator<Medication>() {
                @Override
                public int compare(Medication o1, Medication o2) {
                    return o1.getTitle().compareTo(o2.getTitle());
                }
            });
        }
        if (regChaptersDict != null)
            mDict = regChaptersDict;

        // Loop through all articles
        for (Medication m : mListOfArticles) {
            boolean filtered = true;
            String contentStyle;
            String firstLetter = m.getTitle().substring(0, 1).toUpperCase();

            // TODO: is the styles available on android?
            if (rows % 2 == 0)
                contentStyle = "<li style=\"background-color:var(--background-color-gray);\" id=\"" + firstLetter + "\">";
            else
                contentStyle = "<li style=\"background-color:transparent;\" id=\"" + firstLetter + "\">";

            String contentChapters = "";
            String regnrs[] = m.getRegnrs().split(",");
            String anchor = "";

            HashMap<String, String> indexToTitlesDict = m.indexToTitlesDict();    // id -> chapter title
            // List of chapters
            if (regnrs.length > 0) {
                String r = regnrs[0];
                if (mDict.containsKey(r)) {
                    ArrayList<String> chapters = mDict.get(r);
                    for (String c : chapters) {
                        if (indexToTitlesDict.containsKey(c)) {
                            String cStr = indexToTitlesDict.get(c);
                            anchor = "section" + c;
                            int intValue = 0;
                            try {
                                intValue = Integer.parseInt(c);
                            } catch (Exception e) {
                            }
                            if (intValue > 100)
                                anchor = "Section" + c;
                            int count = 0;
                            if (chaptersCountDict.containsKey(cStr)) {
                                count = chaptersCountDict.get(cStr).intValue();
                            }
                            chaptersCountDict.put(cStr, count + 1);
                            if (filter.length() == 0 || filter.equals(cStr)) {
                                contentChapters += "<span style=\"font-size:0.75em; color:#0088BB\"> <a onclick=\"jsInterface.navigationToFachInfo('" + m.getRegnrs() + "','" + anchor + "')\">" + cStr + "</a></span><br>";
                                filtered = false;
                            }
                        }
                    }
                }
            }
            String contentTitle =
                "<a onclick=\"jsInterface.navigationToFachInfo('" + m.getRegnrs() + "','" + anchor + "')\"><span style=\"font-size:0.8em\"><b>" + m.getTitle() + "</b></span></a> <span style=\"font-size:0.7em\"> | " + m.getAuth() + "</span><br>";

            if (!filtered) {
                htmlStr += contentStyle + contentTitle + contentChapters + "</li>";
                rows++;
            }
        }

        htmlStr += "</ul>";

        ArrayList<String> listOfIds = new ArrayList<>();
        ArrayList<String> listOfTitles = new ArrayList<>();
        for (String cStr : chaptersCountDict.keySet()) {
            listOfIds.add(cStr);
            listOfTitles.add(cStr + " (" + chaptersCountDict.get(cStr) + ")");
        }

        listOfSectionIds = new ArrayList<>(listOfIds);
        // Update section titles
        listOfSectionTitles = new ArrayList<>(listOfTitles);

        return htmlStr;
    }
}
