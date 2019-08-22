/*
Copyright (c) 2013 Max Lungarella

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

import java.io.Serializable;
import java.util.HashMap;

public class Medication implements Serializable {

	private long id;
	private String title;
	private String auth;
	private String atccode;
	private String substances;
	private String regnrs;
	private String atcclass;
	private String therapy;
	private String application;
	private String indications;
	private int customer_id;
	private String pack_info;
	private String addinfo;
	private String sectionIds;
	private String sectionTitles;
	private String content;
	private String style_str;
	private String packages;

	static String[] SectionTitle_DE = {"Zusammensetzung", "Galenische Form", "Kontraindikationen", "Indikationen", "Dosierung/Anwendung",
		"Vorsichtsmassnahmen", "Interaktionen", "Schwangerschaft", "Fahrtüchtigkeit", "Unerwünschte Wirk.", "Überdosierung", "Eig./Wirkung",
		"Kinetik", "Präklinik", "Sonstige Hinweise", "Zulassungsnummer", "Packungen", "Inhaberin", "Stand der Information"};

	static String[] SectionTitle_FR = {"Composition", "Forme galénique", "Contre-indications", "Indications", "Posologie", "Précautions",
		"Interactions", "Grossesse/All.", "Conduite", "Effets indésir.", "Surdosage", "Propriétés/Effets", "Cinétique", "Préclinique", "Remarques",
		"Numéro d'autorisation", "Présentation", "Titulaire", "Mise à jour"};

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuth() {
		return this.auth;
	}

	public void setAuth(String auth) {
		this.auth = auth;
	}

	public String getAtcCode() {
		return this.atccode;
	}

	public void setAtcCode(String atccode) {
		this.atccode = atccode;
	}

	public String getSubstances() {
		return this.substances;
	}

	public void setSubstances(String substances) {
		this.substances = substances;
	}

	public String getRegnrs() {
		return this.regnrs;
	}

	public void setRegnrs(String regnrs) {
		this.regnrs = regnrs;
	}

	public String getAtcClass() {
		return this.atcclass;
	}

	public void setAtcClass(String atcclass) {
		this.atcclass = atcclass;
	}

	public String getTherapy() {
		return this.therapy;
	}

	public void setTherapy(String therapy) {
		this.therapy = therapy;
	}

	public String getApplication() {
		return this.application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getIndications() {
		return this.indications;
	}

	public void setIndications(String indications) {
		this.indications = indications;
	}

	public int getCustomerId() {
		return this.customer_id;
	}

	public void setCustomerId(int customer_id) {
		this.customer_id = customer_id;
	}

	public String getPackInfo() {
		return this.pack_info;
	}

	public void setPackInfo(String pack_info) {
		this.pack_info = pack_info;
	}

	public String getAddInfo() {
		return this.addinfo;
	}

	public void setAddInfo(String addinfo) {
		this.addinfo = addinfo;
	}

	public String getSectionIds() {
		return this.sectionIds;
	}

	public void setSectionIds(String sectionIds) {
		this.sectionIds = sectionIds;
	}

	public String getSectionTitles() {
		return this.sectionTitles;
	}

	public void setSectionTitles(String sectionTitles) {
		this.sectionTitles = sectionTitles;
	}

	public String getContent() {
		return this.content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getStyle() {
		return this.style_str;
	}

	public void setStyle(String style_str) {
		this.style_str = style_str;
	}

	public String getPackages() {
		return this.packages;
	}

	public void setPackages(String packages) {
		this.packages = packages;
	}

	// Used by the ArrayAdapter in ListView
	@Override
	public String toString() {
		return id + "-" + title;
	}

	public String[] packagesFromPackInfo() {
		return this.getPackInfo().split("\n");
	}

	public String[] listOfSectionIds() {
		return this.getSectionIds().split(",");
	}

	public String[] listOfSectionTitles() {
		String titles[] = sectionTitles.split(";");
		int n = titles.length;
		for (int i=0; i<n; ++i) {
			titles[i] = this.shortTitle(titles[i]);
		}
		return titles;
	}

	public String shortTitle(String longTitle) {
		String t = longTitle.toLowerCase();
		if (Constants.appLanguage().equals("de") ) {
			for (int i=0; i<19; i++) {
				if (i >= SectionTitle_DE.length) continue;
				String compareString = SectionTitle_DE[i].toLowerCase();
				if (t.contains(compareString)) {
					return SectionTitle_DE[i];
				}
			}
		} else if (Constants.appLanguage().equals("fr")) {
			for (int i=0; i<19; i++) {
				if (i >= SectionTitle_FR.length) continue;
				String compareString = SectionTitle_FR[i].toLowerCase();
				if (t.contains(compareString)) {
					return SectionTitle_FR[i];
				}
			}
		}
		return longTitle;
	}

	public HashMap<String, String> indexToTitlesDict() {
		HashMap<String, String> dict = new HashMap<>();

		String ids[] = this.listOfSectionIds();
		String titles[] = this.listOfSectionTitles();

		int n1 = ids.length;
		int n2 = titles.length;
		int n = n1 < n2 ? n1 : n2;
		for (int i=0; i<n; ++i) {
			String id = ids[i];
			id = id.replace("section", "");
			id = id.replace("Section", "");
			if (id.length()>0) {
				dict.put(id, this.shortTitle(titles[i]));
			}
		}
		return dict;
	}
}
